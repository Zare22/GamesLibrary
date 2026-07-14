package hr.kotwave.gameslibrary.data

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GameRepositorySyncReviewTest {

    private lateinit var dbFile: File
    private lateinit var database: GamesLibraryDatabase
    private lateinit var dao: GameDao
    private lateinit var repository: GameRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("games_library_sync_review_test", ".db").apply { delete() }
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
    fun splitSyncTailPartitionsKnownDismissedAndFresh() = runTest {
        repository.syncPsnGames(listOf(PsnSyncEntry.Unmatched(psnUids = listOf("CUSA1_00"), name = "Bare From Earlier")))
        repository.confirmSyncReview(
            Store.PSN,
            picks = emptyList(),
            bare = emptyList(),
            dismissed = listOf(SyncTailRow("Some App", listOf("CUSA2_00"))),
        )
        val known = SyncTailRow("Bare From Earlier", listOf("CUSA1_00"))
        val dismissed = SyncTailRow("Some App", listOf("CUSA2_00"))
        val fresh = SyncTailRow("Never Seen", listOf("CUSA3_00"))

        val split = repository.splitSyncTail(Store.PSN, listOf(known, dismissed, fresh))

        assertEquals(listOf(known), split.known)
        assertEquals(listOf(fresh), split.needsReview)
    }

    @Test
    fun splitSyncTailKnownUidWinsOverDismissedOnTheSameRow() = runTest {
        repository.syncPsnGames(listOf(PsnSyncEntry.Unmatched(psnUids = listOf("CUSA1_00"), name = "Bare")))
        repository.confirmSyncReview(
            Store.PSN,
            picks = emptyList(),
            bare = emptyList(),
            dismissed = listOf(SyncTailRow("Bare", listOf("233449"))),
        )
        val row = SyncTailRow("Bare", listOf("233449", "CUSA1_00"))

        val split = repository.splitSyncTail(Store.PSN, listOf(row))

        assertEquals(listOf(row), split.known)
        assertEquals(emptyList(), split.needsReview)
    }

    @Test
    fun confirmSyncReviewPickRecordsStoreUidsIgdbLacks() = runTest {
        // The KOTOR shape: the picked IGDB game has no Epic reference at all.
        val pick = SyncReviewPick(sampleIgdb(igdbId = 501L, name = "Star Wars: KOTOR", refs = emptyList()), listOf("offer1", "item1"))

        val outcome = repository.confirmSyncReview(Store.EPIC, picks = listOf(pick), bare = emptyList(), dismissed = emptyList())

        assertEquals(SyncReviewOutcome(added = 1, updated = 0), outcome)
        val game = dao.getGameByExternalUid(26, "offer1")
        assertNotNull(game)
        assertEquals(501L, game.igdbId)
        assertEquals(game.id, dao.getGameByExternalUid(26, "item1")?.id)
        assertEquals(listOf(Source.EPIC_SYNC), dao.ownershipsFor(game.id).map { it.source })

        // The re-sync loop closes: the row is now known and the merge dedups it.
        val split = repository.splitSyncTail(Store.EPIC, listOf(SyncTailRow("Star Wars: KOTOR", listOf("offer1", "item1"))))
        assertEquals(emptyList(), split.needsReview)
        val resync = repository.syncEpicGames(listOf(EpicSyncEntry.Unmatched(epicUids = listOf("offer1", "item1"), name = "Star Wars: KOTOR")))
        assertEquals(EpicSyncSummary(added = 0, updated = 1), resync)
        assertEquals(1, repository.ownedGames.first().size)
    }

    @Test
    fun confirmSyncReviewPickAttachesToExistingGameAndRecordsUid() = runTest {
        val added = repository.addMatchedGame(sampleIgdb(igdbId = 5L, refs = emptyList()), wishlist = false, stores = setOf(Store.GOG))

        val outcome = repository.confirmSyncReview(
            Store.STEAM,
            picks = listOf(SyncReviewPick(sampleIgdb(igdbId = 5L, refs = emptyList()), listOf("777"))),
            bare = emptyList(),
            dismissed = emptyList(),
        )

        assertEquals(SyncReviewOutcome(added = 0, updated = 1), outcome)
        assertEquals(added.gameId, dao.getGameByExternalUid(1, "777")?.id)
        val ownerships = dao.ownershipsFor(added.gameId)
        assertEquals(setOf(Store.GOG, Store.STEAM), ownerships.map { it.store }.toSet())
        assertEquals(Source.STEAM_SYNC, ownerships.single { it.store == Store.STEAM }.source)
        assertEquals(1, repository.ownedGames.first().size)
    }

    @Test
    fun confirmSyncReviewBareAddsIgdbNullGameWithUid() = runTest {
        val outcome = repository.confirmSyncReview(
            Store.GOG,
            picks = emptyList(),
            bare = listOf(SyncTailRow("Obscure Shareware", listOf("2077"))),
            dismissed = emptyList(),
        )

        assertEquals(SyncReviewOutcome(added = 1, updated = 0), outcome)
        val game = dao.getGameByExternalUid(5, "2077")
        assertNotNull(game)
        assertNull(game.igdbId)
        assertEquals("Obscure Shareware", game.name)
        assertEquals(listOf(Source.GOG_SYNC), dao.ownershipsFor(game.id).map { it.source })
        val split = repository.splitSyncTail(Store.GOG, listOf(SyncTailRow("Obscure Shareware", listOf("2077"))))
        assertEquals(emptyList(), split.needsReview)
    }

    @Test
    fun confirmSyncReviewDismissalAddsNothingAndSticks() = runTest {
        val outcome = repository.confirmSyncReview(
            Store.PSN,
            picks = emptyList(),
            bare = emptyList(),
            dismissed = listOf(SyncTailRow("Netflix", listOf("CUSA0001_00", "10001"))),
        )

        assertEquals(SyncReviewOutcome(added = 0, updated = 0), outcome)
        assertEquals(0, repository.ownedGames.first().size)
        val split = repository.splitSyncTail(Store.PSN, listOf(SyncTailRow("Netflix", listOf("CUSA0001_00", "10001"))))
        assertEquals(emptyList(), split.known)
        assertEquals(emptyList(), split.needsReview)
    }

    @Test
    fun confirmSyncReviewRejectsStoreWithoutSync() = runTest {
        assertFailsWith<IllegalStateException> {
            repository.confirmSyncReview(Store.BATTLE_NET, picks = emptyList(), bare = emptyList(), dismissed = emptyList())
        }
    }

    @Test
    fun syncRecordsEntryUidsOnExistingMatchedGame() = runTest {
        repository.syncPsnGames(listOf(PsnSyncEntry.Matched(sampleIgdb(igdbId = 7L), psnUids = listOf("PPSA7_00"))))

        repository.syncPsnGames(listOf(PsnSyncEntry.Matched(sampleIgdb(igdbId = 7L), psnUids = listOf("PPSA7_00", "CUSA7_00"))))

        val game = dao.getGameByExternalUid(36, "CUSA7_00")
        assertNotNull(game)
        assertEquals(7L, game.igdbId)
        assertEquals(1, repository.ownedGames.first().size)
    }

    private fun sampleIgdb(
        igdbId: Long,
        name: String = "Sample Game",
        refs: List<ExternalRef> = listOf(ExternalRef(category = 36, uid = igdbId.toString(), url = null)),
    ): IgdbGame = IgdbGame(
        igdbId = igdbId,
        name = name,
        slug = "a-slug",
        firstReleaseDate = 1488499200L,
        coverImageId = "co3p2d",
        developer = "Sample Dev",
        totalRating = 92.0,
        totalRatingCount = 500,
        platforms = listOf(Platform("PC (Microsoft Windows)", "PC")),
        alternativeNames = emptyList(),
        externalGames = refs,
    )
}
