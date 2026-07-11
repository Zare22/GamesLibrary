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

class GameRepositoryPsnSyncTest {

    private lateinit var dbFile: File
    private lateinit var database: GamesLibraryDatabase
    private lateinit var dao: GameDao
    private lateinit var repository: GameRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("games_library_psn_test", ".db").apply { delete() }
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
    fun psnSyncAddsMatchedAndUnmatchedGames() = runTest {
        val summary = repository.syncPsnGames(
            listOf(
                PsnSyncEntry.Matched(sampleIgdb(igdbId = 27134L, name = "Deep Rock Galactic"), psnUids = listOf("PPSA100_00", "27134")),
                PsnSyncEntry.Unmatched(psnUids = listOf("CUSA999_00"), name = "Disc Only Game"),
            ),
        )

        assertEquals(PsnSyncSummary(added = 2, updated = 0), summary)
        val owned = repository.ownedGames.first()
        assertEquals(2, owned.size)
        owned.forEach { tile ->
            assertEquals(listOf(Store.PSN), tile.ownerships.map { it.store })
            assertEquals(listOf(Source.PSN_SYNC), tile.ownerships.map { it.source })
        }
        val unmatched = dao.getGameByExternalUid(36, "CUSA999_00")!!
        assertNull(unmatched.igdbId)
        assertEquals("Disc Only Game", unmatched.name)
    }

    @Test
    fun psnResyncIsIdempotent() = runTest {
        val entries = listOf(
            PsnSyncEntry.Matched(sampleIgdb(igdbId = 27134L), psnUids = listOf("PPSA100_00", "27134")),
            PsnSyncEntry.Unmatched(psnUids = listOf("233449"), name = "Disc Only Game"),
        )
        repository.syncPsnGames(entries)

        val summary = repository.syncPsnGames(entries)

        assertEquals(PsnSyncSummary(added = 0, updated = 2), summary)
        assertEquals(2, repository.ownedGames.first().size)
    }

    @Test
    fun psnUnmatchedDedupsByUidNotName() = runTest {
        repository.syncPsnGames(listOf(PsnSyncEntry.Unmatched(psnUids = listOf("10003601"), name = "Cricket 22")))

        val summary = repository.syncPsnGames(listOf(PsnSyncEntry.Unmatched(psnUids = listOf("10003601"), name = "Cricket 22 Renamed")))

        assertEquals(PsnSyncSummary(added = 0, updated = 1), summary)
        val game = dao.getGameByExternalUid(36, "10003601")!!
        assertEquals("Cricket 22", game.name) // name is never overwritten
        assertEquals(1, repository.ownedGames.first().size)
    }

    @Test
    fun psnSyncUpgradesManualOwnershipToPsnSync() = runTest {
        val added = repository.addMatchedGame(sampleIgdb(igdbId = 5L), wishlist = false, stores = setOf(Store.PSN))
        assertEquals(Source.MANUAL, dao.ownershipsFor(added.gameId).single().source)

        repository.syncPsnGames(listOf(PsnSyncEntry.Matched(sampleIgdb(igdbId = 5L), psnUids = listOf("PPSA5_00"))))

        val ownership = dao.ownershipsFor(added.gameId).single()
        assertEquals(Store.PSN, ownership.store)
        assertEquals(Source.PSN_SYNC, ownership.source)
    }

    @Test
    fun psnSyncPreservesLocalStateAndCachedMetadata() = runTest {
        val added = repository.addMatchedGame(
            sampleIgdb(igdbId = 7L, name = "Original Name"),
            wishlist = false,
            status = Status.PLAYING,
            stores = setOf(Store.PSN),
        )
        repository.setUserRating(added.gameId, 9.0)

        repository.syncPsnGames(listOf(PsnSyncEntry.Matched(sampleIgdb(igdbId = 7L, name = "Changed Name"), psnUids = listOf("PPSA7_00"))))

        val game = dao.getGame(added.gameId)!!
        assertEquals("Original Name", game.name) // cached metadata is never overwritten by a sync
        assertEquals(Status.PLAYING, game.status)
        assertEquals(9.0, game.userRating)
        assertEquals(1, dao.ownershipsFor(added.gameId).size)
    }

    @Test
    fun psnSyncOnWishlistedGameClearsWishlistAndOwnsIt() = runTest {
        val wished = repository.addMatchedGame(sampleIgdb(igdbId = 8L), wishlist = true)

        repository.syncPsnGames(listOf(PsnSyncEntry.Matched(sampleIgdb(igdbId = 8L), psnUids = listOf("PPSA8_00"))))

        val game = dao.getGame(wished.gameId)!!
        assertEquals(false, game.wishlist)
        assertEquals(Status.BACKLOG, game.status)
        val ownership = dao.ownershipsFor(wished.gameId).single()
        assertEquals(Store.PSN, ownership.store)
        assertEquals(Source.PSN_SYNC, ownership.source)
    }

    @Test
    fun psnMatchedUpgradesBareGameInPlace() = runTest {
        repository.syncPsnGames(listOf(PsnSyncEntry.Unmatched(psnUids = listOf("CUSA100_00"), name = "SHAREfactory")))
        val bare = dao.getGameByExternalUid(36, "CUSA100_00")!!
        repository.setUserRating(bare.id, 9.0)

        val summary = repository.syncPsnGames(
            listOf(PsnSyncEntry.Matched(sampleIgdb(igdbId = 27134L, name = "Deep Rock Galactic"), psnUids = listOf("CUSA100_00", "27134"))),
        )

        assertEquals(PsnSyncSummary(added = 0, updated = 1), summary)
        assertEquals(1, repository.ownedGames.first().size)
        val upgraded = dao.getGame(bare.id)!!
        assertEquals(27134L, upgraded.igdbId)
        assertEquals("Deep Rock Galactic", upgraded.name)
        assertEquals("co3p2d", upgraded.coverImageId)
        assertEquals(9.0, upgraded.userRating) // local state survives the upgrade
        assertEquals(Status.BACKLOG, upgraded.status)
        assertEquals(bare.id, dao.getGameByExternalUid(36, "27134")!!.id)
        assertEquals(bare.id, dao.getGameByExternalUid(36, "CUSA100_00")!!.id)
    }

    @Test
    fun psnUnmatchedDedupsAcrossAnyUid() = runTest {
        repository.syncPsnGames(listOf(PsnSyncEntry.Unmatched(psnUids = listOf("233449"), name = "Old Key Style")))

        val summary = repository.syncPsnGames(
            listOf(PsnSyncEntry.Unmatched(psnUids = listOf("CUSA200_00", "233449"), name = "Old Key Style")),
        )

        assertEquals(PsnSyncSummary(added = 0, updated = 1), summary)
        assertEquals(1, repository.ownedGames.first().size)
    }

    @Test
    fun psnUidsAlreadyMatchedReturnsOnlyIgdbMatchedUids() = runTest {
        repository.syncPsnGames(
            listOf(
                PsnSyncEntry.Matched(sampleIgdb(igdbId = 27134L), psnUids = listOf("PPSA1_00", "27134")),
                PsnSyncEntry.Unmatched(psnUids = listOf("CUSA2_00"), name = "Bare"),
            ),
        )

        val matched = repository.psnUidsAlreadyMatched(listOf("PPSA1_00", "CUSA2_00", "CUSA3_00"))

        assertEquals(setOf("PPSA1_00"), matched)
    }

    private fun sampleIgdb(igdbId: Long, name: String = "Sample Game"): IgdbGame = IgdbGame(
        igdbId = igdbId,
        name = name,
        slug = "a-slug",
        firstReleaseDate = 1488499200L,
        coverImageId = "co3p2d",
        developer = "Ghost Ship Games",
        totalRating = 92.0,
        totalRatingCount = 500,
        platforms = listOf(Platform("PlayStation 5", "PS5")),
        alternativeNames = emptyList(),
        externalGames = listOf(ExternalRef(category = 36, uid = igdbId.toString(), url = "https://store.playstation.com/en-us/concept/$igdbId")),
    )
}
