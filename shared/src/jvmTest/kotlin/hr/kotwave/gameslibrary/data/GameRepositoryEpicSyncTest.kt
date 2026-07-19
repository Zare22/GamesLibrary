package hr.kotwave.gameslibrary.data

import androidx.room.Room
import hr.kotwave.gameslibrary.data.db.GameDao
import hr.kotwave.gameslibrary.data.db.GamesLibraryDatabase
import hr.kotwave.gameslibrary.data.db.buildGamesLibraryDatabase
import hr.kotwave.gameslibrary.data.sync.SyncEntry
import hr.kotwave.gameslibrary.data.sync.SyncSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameRepositoryEpicSyncTest {

    private lateinit var dbFile: File
    private lateinit var database: GamesLibraryDatabase
    private lateinit var dao: GameDao
    private lateinit var repository: GameRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("games_library_epic_test", ".db").apply { delete() }
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
    fun epicSyncAddsMatchedAndUnmatchedGames() = runTest {
        val summary = repository.syncStore(
            Store.EPIC,
            listOf(
                SyncEntry.Matched(sampleIgdb(igdbId = 1905L, name = "World War Z"), uids = listOf("catItem1", "offer1")),
                SyncEntry.Unmatched(uids = listOf("fm24item"), name = "Football Manager 2024"),
            ),
        )

        assertEquals(SyncSummary(added = 2, updated = 0), summary)
        val owned = repository.ownedGames.first()
        assertEquals(2, owned.size)
        owned.forEach { tile ->
            assertEquals(listOf(Store.EPIC), tile.ownerships.map { it.store })
            assertEquals(listOf(Source.EPIC_SYNC), tile.ownerships.map { it.source })
        }
        val unmatched = dao.getGameByExternalUid(26, "fm24item")!!
        assertNull(unmatched.igdbId)
        assertEquals("Football Manager 2024", unmatched.name)
    }

    @Test
    fun epicResyncIsIdempotent() = runTest {
        val entries = listOf(
            SyncEntry.Matched(sampleIgdb(igdbId = 1905L), uids = listOf("catItem1", "offer1")),
            SyncEntry.Unmatched(uids = listOf("fm24item"), name = "Football Manager 2024"),
        )
        repository.syncStore(Store.EPIC, entries)

        val summary = repository.syncStore(Store.EPIC, entries)

        assertEquals(SyncSummary(added = 0, updated = 2), summary)
        assertEquals(2, repository.ownedGames.first().size)
    }

    @Test
    fun epicUnmatchedDedupsByUidNotName() = runTest {
        repository.syncStore(Store.EPIC, listOf(SyncEntry.Unmatched(uids = listOf("kotorItem"), name = "Knights of the Old Republic")))

        val summary = repository.syncStore(
            Store.EPIC,
            listOf(SyncEntry.Unmatched(uids = listOf("kotorItem"), name = "KOTOR Renamed")),
        )

        assertEquals(SyncSummary(added = 0, updated = 1), summary)
        val game = dao.getGameByExternalUid(26, "kotorItem")!!
        assertEquals("Knights of the Old Republic", game.name) // name is never overwritten
        assertEquals(1, repository.ownedGames.first().size)
    }

    @Test
    fun epicSyncUpgradesManualOwnershipToEpicSync() = runTest {
        val added = repository.addMatchedGame(sampleIgdb(igdbId = 5L), wishlist = false, stores = setOf(Store.EPIC))
        assertEquals(Source.MANUAL, dao.ownershipsFor(added.gameId).single().source)

        repository.syncStore(Store.EPIC, listOf(SyncEntry.Matched(sampleIgdb(igdbId = 5L), uids = listOf("item5"))))

        val ownership = dao.ownershipsFor(added.gameId).single()
        assertEquals(Store.EPIC, ownership.store)
        assertEquals(Source.EPIC_SYNC, ownership.source)
    }

    @Test
    fun epicSyncPreservesLocalStateAndCachedMetadata() = runTest {
        val added = repository.addMatchedGame(
            sampleIgdb(igdbId = 7L, name = "Original Name"),
            wishlist = false,
            status = Status.PLAYING,
            stores = setOf(Store.EPIC),
        )
        repository.setUserRating(added.gameId, 9.0)

        repository.syncStore(Store.EPIC, listOf(SyncEntry.Matched(sampleIgdb(igdbId = 7L, name = "Changed Name"), uids = listOf("item7"))))

        val game = dao.getGame(added.gameId)!!
        assertEquals("Original Name", game.name) // cached metadata is never overwritten by a sync
        assertEquals(Status.PLAYING, game.status)
        assertEquals(9.0, game.userRating)
        assertEquals(1, dao.ownershipsFor(added.gameId).size)
    }

    @Test
    fun epicSyncOnWishlistedGameClearsWishlistAndOwnsIt() = runTest {
        val wished = repository.addMatchedGame(sampleIgdb(igdbId = 8L), wishlist = true)

        repository.syncStore(Store.EPIC, listOf(SyncEntry.Matched(sampleIgdb(igdbId = 8L), uids = listOf("item8"))))

        val game = dao.getGame(wished.gameId)!!
        assertEquals(false, game.wishlist)
        assertEquals(Status.BACKLOG, game.status)
        val ownership = dao.ownershipsFor(wished.gameId).single()
        assertEquals(Store.EPIC, ownership.store)
        assertEquals(Source.EPIC_SYNC, ownership.source)
    }

    @Test
    fun epicMatchedUpgradesBareGameInPlace() = runTest {
        // A delisted game synced unmatched earlier heals once IGDB gains its row (or the offer returns).
        repository.syncStore(Store.EPIC, listOf(SyncEntry.Unmatched(uids = listOf("wcfItem"), name = "Wild Card Football")))
        val bare = dao.getGameByExternalUid(26, "wcfItem")!!
        repository.setUserRating(bare.id, 9.0)

        val summary = repository.syncStore(
            Store.EPIC,
            listOf(SyncEntry.Matched(sampleIgdb(igdbId = 252L, name = "Wild Card Football"), uids = listOf("wcfItem", "wcfOffer"))),
        )

        assertEquals(SyncSummary(added = 0, updated = 1), summary)
        assertEquals(1, repository.ownedGames.first().size)
        val upgraded = dao.getGame(bare.id)!!
        assertEquals(252L, upgraded.igdbId)
        assertEquals("Wild Card Football", upgraded.name)
        assertEquals("co3p2d", upgraded.coverImageId)
        assertEquals(9.0, upgraded.userRating) // local state survives the upgrade
        assertEquals(Status.BACKLOG, upgraded.status)
        assertEquals(bare.id, dao.getGameByExternalUid(26, "wcfOffer")!!.id)
        assertEquals(bare.id, dao.getGameByExternalUid(26, "wcfItem")!!.id)
    }

    @Test
    fun epicUnmatchedDedupsAcrossAnyUid() = runTest {
        repository.syncStore(Store.EPIC, listOf(SyncEntry.Unmatched(uids = listOf("catItemX"), name = "Some Game")))

        val summary = repository.syncStore(
            Store.EPIC,
            listOf(SyncEntry.Unmatched(uids = listOf("offerX", "catItemX"), name = "Some Game")),
        )

        assertEquals(SyncSummary(added = 0, updated = 1), summary)
        assertEquals(1, repository.ownedGames.first().size)
    }

    @Test
    fun uidsAlreadyMatchedReturnsOnlyIgdbMatchedEpicUids() = runTest {
        repository.syncStore(
            Store.EPIC,
            listOf(
                SyncEntry.Matched(sampleIgdb(igdbId = 1905L), uids = listOf("catItem1", "offer1")),
                SyncEntry.Unmatched(uids = listOf("fm24item"), name = "Bare"),
            ),
        )

        val matched = repository.uidsAlreadyMatched(Store.EPIC, listOf("catItem1", "fm24item", "neverSeen"))

        assertEquals(setOf("catItem1"), matched)
    }

    private fun sampleIgdb(igdbId: Long, name: String = "Sample Game"): IgdbGame = IgdbGame(
        igdbId = igdbId,
        name = name,
        slug = "a-slug",
        firstReleaseDate = 1488499200L,
        coverImageId = "co3p2d",
        developer = "Saber Interactive",
        totalRating = 82.0,
        totalRatingCount = 300,
        platforms = listOf(Platform("PC (Microsoft Windows)", "PC")),
        alternativeNames = emptyList(),
        externalGames = listOf(ExternalRef(category = 26, uid = "offer$igdbId", url = "https://store.epicgames.com/en-US/p/a-slug")),
    )
}
