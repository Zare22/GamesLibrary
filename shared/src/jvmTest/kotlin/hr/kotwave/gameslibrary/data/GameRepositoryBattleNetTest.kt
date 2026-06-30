package hr.kotwave.gameslibrary.data

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Proves the Battle.net picker's titles flow through the shared [GameRepository.confirmImport] path on
 * [Store.BATTLE_NET] — additive, tagged PASTE_IMPORT — and that the new enum value round-trips Room.
 */
class GameRepositoryBattleNetTest {

    private lateinit var dbFile: File
    private lateinit var database: GamesLibraryDatabase
    private lateinit var dao: GameDao
    private lateinit var repository: GameRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("games_library_battlenet_test", ".db").apply { delete() }
        database = Room.databaseBuilder<GamesLibraryDatabase>(name = dbFile.absolutePath)
            .buildGamesLibraryDatabase()
        dao = database.gameDao()
        repository = GameRepository(dao)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun pickedMatchedTitleIsAddedOnBattleNetTaggedPasteImport() = runTest {
        val summary = repository.confirmImport(
            listOf(ImportEntry.Matched(sampleIgdb(igdbId = 138L, name = "Diablo IV"), store = Store.BATTLE_NET)),
        )

        assertEquals(ImportSummary(added = 1, attached = 0), summary)
        val game = dao.getGameByIgdbId(138L)!!
        val ownership = dao.ownershipsFor(game.id).single()
        assertEquals(Store.BATTLE_NET, ownership.store)
        assertEquals(Source.PASTE_IMPORT, ownership.source)
    }

    @Test
    fun pickedTitleAlreadyOwnedElsewhereAttachesBattleNetOwnership() = runTest {
        val added = repository.addMatchedGame(sampleIgdb(igdbId = 42L), wishlist = false, stores = setOf(Store.STEAM))

        val summary = repository.confirmImport(
            listOf(ImportEntry.Matched(sampleIgdb(igdbId = 42L), store = Store.BATTLE_NET)),
        )

        assertEquals(ImportSummary(added = 0, attached = 1), summary)
        assertEquals(1, repository.ownedGames.first().size)
        val stores = dao.ownershipsFor(added.gameId).associateBy { it.store }
        assertEquals(setOf(Store.STEAM, Store.BATTLE_NET), stores.keys)
        assertEquals(Source.PASTE_IMPORT, stores.getValue(Store.BATTLE_NET).source)
    }

    @Test
    fun unmatchedPickedTitleAddsAnIgdbNullGameOnBattleNet() = runTest {
        val summary = repository.confirmImport(
            listOf(ImportEntry.Unmatched(name = "Warcraft I: Remastered", store = Store.BATTLE_NET)),
        )

        assertEquals(ImportSummary(added = 1, attached = 0), summary)
        val tile = repository.ownedGames.first().single()
        assertNull(tile.game.igdbId)
        assertEquals("Warcraft I: Remastered", tile.game.name)
        assertEquals(Store.BATTLE_NET, tile.ownerships.single().store)
    }

    private fun sampleIgdb(igdbId: Long, name: String = "Sample Game"): IgdbGame = IgdbGame(
        igdbId = igdbId,
        name = name,
        slug = "a-slug",
        firstReleaseDate = 1488499200L,
        coverImageId = "co3p2d",
        developer = "Some Studio",
        totalRating = 90.0,
        totalRatingCount = 100,
        platforms = listOf(Platform("PC (Microsoft Windows)", "PC")),
        alternativeNames = emptyList(),
        externalGames = emptyList(),
    )
}
