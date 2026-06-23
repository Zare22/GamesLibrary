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

class GameRepositoryGogSyncTest {

    private lateinit var dbFile: File
    private lateinit var database: GamesLibraryDatabase
    private lateinit var dao: GameDao
    private lateinit var repository: GameRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("games_library_gog_test", ".db").apply { delete() }
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
    fun gogSyncAddsMatchedAndUnmatchedGames() = runTest {
        val summary = repository.syncGogGames(
            listOf(
                GogSyncEntry.Matched(sampleIgdb(igdbId = 1207658691L, name = "The Witcher 3")),
                GogSyncEntry.Unmatched(gogId = "999", name = "Some GOG Indie"),
            ),
        )

        assertEquals(GogSyncSummary(added = 2, updated = 0), summary)
        val owned = repository.ownedGames.first()
        assertEquals(2, owned.size)
        owned.forEach { tile ->
            assertEquals(listOf(Store.GOG), tile.ownerships.map { it.store })
            assertEquals(listOf(Source.GOG_SYNC), tile.ownerships.map { it.source })
        }
        val unmatched = dao.getGameByExternalUid(5, "999")!!
        assertNull(unmatched.igdbId)
        assertEquals("Some GOG Indie", unmatched.name)
    }

    @Test
    fun gogResyncIsIdempotent() = runTest {
        val entries = listOf(
            GogSyncEntry.Matched(sampleIgdb(igdbId = 1207658691L)),
            GogSyncEntry.Unmatched(gogId = "999", name = "Some GOG Indie"),
        )
        repository.syncGogGames(entries)

        val summary = repository.syncGogGames(entries)

        assertEquals(GogSyncSummary(added = 0, updated = 2), summary)
        assertEquals(2, repository.ownedGames.first().size)
    }

    @Test
    fun gogUnmatchedDedupsByGogIdNotName() = runTest {
        repository.syncGogGames(listOf(GogSyncEntry.Unmatched(gogId = "999", name = "Indie")))

        val summary = repository.syncGogGames(listOf(GogSyncEntry.Unmatched(gogId = "999", name = "Indie Renamed")))

        assertEquals(GogSyncSummary(added = 0, updated = 1), summary)
        val game = dao.getGameByExternalUid(5, "999")!!
        assertEquals("Indie", game.name) // name is never overwritten
        assertEquals(1, repository.ownedGames.first().size)
    }

    @Test
    fun gogSyncUpgradesManualOwnershipToGogSync() = runTest {
        val added = repository.addMatchedGame(sampleIgdb(igdbId = 5L), wishlist = false, stores = setOf(Store.GOG))
        assertEquals(Source.MANUAL, dao.ownershipsFor(added.gameId).single().source)

        repository.syncGogGames(listOf(GogSyncEntry.Matched(sampleIgdb(igdbId = 5L))))

        val ownership = dao.ownershipsFor(added.gameId).single()
        assertEquals(Store.GOG, ownership.store)
        assertEquals(Source.GOG_SYNC, ownership.source)
    }

    @Test
    fun gogSyncPreservesLocalStateAndCachedMetadata() = runTest {
        val added = repository.addMatchedGame(
            sampleIgdb(igdbId = 7L, name = "Original Name"),
            wishlist = false,
            status = Status.PLAYING,
            stores = setOf(Store.GOG),
        )
        repository.setUserRating(added.gameId, 9.0)

        repository.syncGogGames(listOf(GogSyncEntry.Matched(sampleIgdb(igdbId = 7L, name = "Changed Name"))))

        val game = dao.getGame(added.gameId)!!
        assertEquals("Original Name", game.name) // cached metadata is never overwritten by a sync
        assertEquals(Status.PLAYING, game.status)
        assertEquals(9.0, game.userRating)
        assertEquals(1, dao.ownershipsFor(added.gameId).size)
    }

    @Test
    fun gogSyncOnWishlistedGameClearsWishlistAndOwnsIt() = runTest {
        val wished = repository.addMatchedGame(sampleIgdb(igdbId = 8L), wishlist = true)

        repository.syncGogGames(listOf(GogSyncEntry.Matched(sampleIgdb(igdbId = 8L))))

        val game = dao.getGame(wished.gameId)!!
        assertEquals(false, game.wishlist)
        assertEquals(Status.BACKLOG, game.status)
        val ownership = dao.ownershipsFor(wished.gameId).single()
        assertEquals(Store.GOG, ownership.store)
        assertEquals(Source.GOG_SYNC, ownership.source)
    }

    private fun sampleIgdb(igdbId: Long, name: String = "Sample Game"): IgdbGame = IgdbGame(
        igdbId = igdbId,
        name = name,
        slug = "a-slug",
        firstReleaseDate = 1488499200L,
        coverImageId = "co3p2d",
        developer = "CD Projekt RED",
        totalRating = 92.0,
        totalRatingCount = 500,
        platforms = listOf(Platform("PC (Microsoft Windows)", "PC")),
        alternativeNames = emptyList(),
        externalGames = listOf(ExternalRef(category = 5, uid = igdbId.toString(), url = "https://www.gog.com/game/$igdbId")),
    )
}
