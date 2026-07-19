package hr.kotwave.gameslibrary.data

import androidx.room.Room
import hr.kotwave.gameslibrary.data.db.GameDao
import hr.kotwave.gameslibrary.data.db.GamesLibraryDatabase
import hr.kotwave.gameslibrary.data.db.buildGamesLibraryDatabase
import hr.kotwave.gameslibrary.data.sync.ImportEntry
import hr.kotwave.gameslibrary.data.sync.ImportSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameRepositoryImportTest {

    private lateinit var dbFile: File
    private lateinit var database: GamesLibraryDatabase
    private lateinit var dao: GameDao
    private lateinit var repository: GameRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("games_library_import_test", ".db").apply { delete() }
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
    fun matchedNewGameIsAddedOnTheStoreTaggedPasteImport() = runTest {
        val summary = repository.confirmImport(
            listOf(ImportEntry.Matched(sampleIgdb(igdbId = 1942L, name = "The Witcher 3"), store = Store.GOG)),
        )

        assertEquals(ImportSummary(added = 1, attached = 0), summary)
        val game = dao.getGameByIgdbId(1942L)!!
        val ownership = dao.ownershipsFor(game.id).single()
        assertEquals(Store.GOG, ownership.store)
        assertEquals(Source.PASTE_IMPORT, ownership.source)
        assertEquals(false, game.wishlist)
    }

    @Test
    fun matchedExistingGameAttachesAStoreOwnershipInsteadOfDuplicating() = runTest {
        val added = repository.addMatchedGame(sampleIgdb(igdbId = 5L), wishlist = false, stores = setOf(Store.STEAM))

        val summary = repository.confirmImport(
            listOf(ImportEntry.Matched(sampleIgdb(igdbId = 5L), store = Store.GOG)),
        )

        assertEquals(ImportSummary(added = 0, attached = 1), summary)
        assertEquals(1, repository.ownedGames.first().size)
        val stores = dao.ownershipsFor(added.gameId).associateBy { it.store }
        assertEquals(setOf(Store.STEAM, Store.GOG), stores.keys)
        assertEquals(Source.PASTE_IMPORT, stores.getValue(Store.GOG).source)
    }

    @Test
    fun matchedExistingWishlistGameBecomesOwned() = runTest {
        val wished = repository.addMatchedGame(sampleIgdb(igdbId = 8L), wishlist = true)

        repository.confirmImport(listOf(ImportEntry.Matched(sampleIgdb(igdbId = 8L), store = Store.GOG)))

        val game = dao.getGame(wished.gameId)!!
        assertEquals(false, game.wishlist)
        assertEquals(Store.GOG, dao.ownershipsFor(game.id).single().store)
    }

    @Test
    fun unmatchedAddsAnIgdbNullGameWithTheStoreOwnership() = runTest {
        val summary = repository.confirmImport(
            listOf(ImportEntry.Unmatched(name = "Some Indie", store = Store.PSN)),
        )

        assertEquals(ImportSummary(added = 1, attached = 0), summary)
        val tile = repository.ownedGames.first().single()
        assertNull(tile.game.igdbId)
        assertEquals("Some Indie", tile.game.name)
        val ownership = tile.ownerships.single()
        assertEquals(Store.PSN, ownership.store)
        assertEquals(Source.PASTE_IMPORT, ownership.source)
    }

    @Test
    fun reConfirmingTheSameMatchedGameAndStoreIsIdempotent() = runTest {
        val entries = listOf(ImportEntry.Matched(sampleIgdb(igdbId = 5L), store = Store.GOG))
        repository.confirmImport(entries)

        val summary = repository.confirmImport(entries)

        assertEquals(ImportSummary(added = 0, attached = 1), summary)
        val game = dao.getGameByIgdbId(5L)!!
        assertEquals(1, dao.ownershipsFor(game.id).size) // (Game, Store) Ownership is unique
    }

    @Test
    fun mixedBatchCountsAddsAndAttachesSeparately() = runTest {
        repository.addMatchedGame(sampleIgdb(igdbId = 5L), wishlist = false, stores = setOf(Store.STEAM))

        val summary = repository.confirmImport(
            listOf(
                ImportEntry.Matched(sampleIgdb(igdbId = 5L), store = Store.GOG), // attaches
                ImportEntry.Matched(sampleIgdb(igdbId = 99L, name = "Brand New"), store = Store.GOG), // adds
                ImportEntry.Unmatched(name = "No IGDB Hit", store = Store.GOG), // adds
            ),
        )

        assertEquals(ImportSummary(added = 2, attached = 1), summary)
        assertTrue(repository.ownedGames.first().size == 3)
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
