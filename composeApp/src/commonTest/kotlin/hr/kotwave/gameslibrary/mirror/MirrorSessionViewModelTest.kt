package hr.kotwave.gameslibrary.mirror

import hr.kotwave.gameslibrary.data.SyncDismissal
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_NEEDS_REPAIR_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import hr.kotwave.gameslibrary.transfer.ExportedGame
import hr.kotwave.gameslibrary.transfer.LibraryExport
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class MirrorSessionViewModelTest {

    private lateinit var storage: FakeSessionStorage

    private val gameMine = ExportedGame(name = "Baldur's Gate 3", igdbId = 1, status = "PLAYING")
    private val gameTheirs = gameMine.copy(status = "COMPLETED")
    private val conflict = MirrorConflict(
        kind = MirrorConflictKind.UserState,
        mine = gameMine,
        theirs = gameTheirs,
        fields = setOf(MirrorField.STATUS),
        merged = gameMine,
    )
    private val collision = MirrorCollision(
        manual = ExportedGame(name = "Witcher 3 GOTY"),
        matched = ExportedGame(name = "The Witcher 3: Wild Hunt", igdbId = 2),
        manualIsMine = true,
    )
    private val outcome = MirrorOutcome(
        converged = listOf(gameMine, collision.matched),
        mineChanges = MirrorSideChanges(adds = listOf(collision.matched)),
        theirsChanges = MirrorSideChanges(updates = listOf(gameMine)),
        dismissals = emptyList(),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        storage = FakeSessionStorage()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        isPinning: (Throwable) -> Boolean = { false },
        runMirror: suspend (
            (MirrorProgress) -> Unit,
            suspend (MirrorReview) -> MirrorReviewDecisions,
        ) -> MirrorSessionResult,
    ): MirrorSessionViewModel = MirrorSessionViewModel(
        session = MirrorSession(NoopLocalStore(), storage),
        secureStorage = storage,
        isPinningFailure = isPinning,
        runMirror = runMirror,
    )

    @Test
    fun happyPathWithoutReviewReachesTheSummary() = runTest {
        val vm = viewModel { onProgress, _ ->
            onProgress(MirrorProgress.Pulling(1))
            onProgress(MirrorProgress.Pulled(152))
            onProgress(MirrorProgress.Compared(rowsToDecide = 0, firstMirror = false))
            onProgress(MirrorProgress.Pushing)
            onProgress(MirrorProgress.Applied)
            MirrorSessionResult.Completed(outcome)
        }

        vm.start()

        val summary = assertIs<MirrorSessionPhase.Summary>(vm.phase)
        assertEquals(2, summary.gameCount)
        assertEquals(MirrorSessionPhase.SideCounts(added = 1, updated = 0, removed = 0), summary.mine)
        assertEquals(MirrorSessionPhase.SideCounts(added = 0, updated = 1, removed = 0), summary.theirs)
        assertTrue(!summary.nothingChanged)
        assertTrue(!summary.firstMirror)
    }

    @Test
    fun emptyOutcomeSummarizesAsNothingChanged() = runTest {
        val vm = viewModel { onProgress, _ ->
            onProgress(MirrorProgress.Compared(rowsToDecide = 0, firstMirror = true))
            MirrorSessionResult.Completed(outcome.copy(mineChanges = MirrorSideChanges(), theirsChanges = MirrorSideChanges()))
        }

        vm.start()

        val summary = assertIs<MirrorSessionPhase.Summary>(vm.phase)
        assertTrue(summary.nothingChanged)
        assertTrue(summary.firstMirror)
    }

    @Test
    fun reviewParksThenDecisionsContinueTheMirror() = runTest {
        var received: MirrorReviewDecisions? = null
        val vm = viewModel { onProgress, review ->
            onProgress(MirrorProgress.Compared(rowsToDecide = 2, firstMirror = false))
            received = review(MirrorReview(listOf(conflict), listOf(collision)))
            MirrorSessionResult.Completed(outcome)
        }

        vm.start()
        val steps = assertIs<MirrorSessionPhase.Steps>(vm.phase)
        assertTrue(steps.awaitingReview)
        assertEquals(2, steps.rowsToDecide)

        vm.openReview()
        val review = assertIs<MirrorSessionPhase.Review>(vm.phase)
        assertEquals(listOf(conflict), review.conflicts)
        assertEquals(listOf(collision), review.collisions)

        vm.chooseConflict(conflict, keepMine = false)
        vm.chooseCollision(collision, merge = true)
        vm.confirmReview()

        assertEquals(
            MirrorReviewDecisions(
                conflictDecisions = listOf(MirrorConflictDecision(conflict, keepMine = false)),
                collisionDecisions = listOf(MirrorCollisionDecision(collision, merge = true)),
            ),
            received,
        )
        assertIs<MirrorSessionPhase.Summary>(vm.phase)
    }

    @Test
    fun confirmIsIgnoredUntilEveryRowIsDecided() = runTest {
        val vm = viewModel { _, review ->
            review(MirrorReview(listOf(conflict), listOf(collision)))
            MirrorSessionResult.Completed(outcome)
        }

        vm.start()
        vm.openReview()
        vm.chooseConflict(conflict, keepMine = true)

        vm.confirmReview()

        assertIs<MirrorSessionPhase.Review>(vm.phase)
    }

    @Test
    fun identicalRowsOnA409RetryAreAutoAnswered() = runTest {
        var secondDecisions: MirrorReviewDecisions? = null
        val vm = viewModel { onProgress, review ->
            review(MirrorReview(listOf(conflict), emptyList()))
            onProgress(MirrorProgress.Pulling(2))
            secondDecisions = review(MirrorReview(listOf(conflict), emptyList()))
            MirrorSessionResult.Completed(outcome)
        }

        vm.start()
        vm.openReview()
        vm.chooseConflict(conflict, keepMine = true)
        vm.confirmReview()

        assertEquals(
            MirrorReviewDecisions(conflictDecisions = listOf(MirrorConflictDecision(conflict, keepMine = true))),
            secondDecisions,
        )
        assertIs<MirrorSessionPhase.Summary>(vm.phase)
    }

    @Test
    fun onlyChangedRowsComeBackOnA409Retry() = runTest {
        val changed = conflict.copy(theirs = gameTheirs.copy(status = "DROPPED"))
        var received: MirrorReviewDecisions? = null
        val vm = viewModel { _, review ->
            review(MirrorReview(listOf(conflict), emptyList()))
            received = review(MirrorReview(listOf(conflict, changed), emptyList()))
            MirrorSessionResult.Completed(outcome)
        }

        vm.start()
        vm.openReview()
        vm.chooseConflict(conflict, keepMine = true)
        vm.confirmReview()

        val reasked = assertIs<MirrorSessionPhase.Steps>(vm.phase)
        assertTrue(reasked.awaitingReview)
        assertEquals(1, reasked.rowsToDecide)
        vm.openReview()
        assertEquals(listOf(changed), assertIs<MirrorSessionPhase.Review>(vm.phase).conflicts)

        vm.chooseConflict(changed, keepMine = false)
        vm.confirmReview()

        assertEquals(
            MirrorReviewDecisions(
                conflictDecisions = listOf(
                    MirrorConflictDecision(conflict, keepMine = true),
                    MirrorConflictDecision(changed, keepMine = false),
                ),
            ),
            received,
        )
    }

    @Test
    fun hostKeptChangingShowsV4() = runTest {
        val vm = viewModel { _, _ -> MirrorSessionResult.HostKeptChanging }

        vm.start()

        assertEquals(MirrorSessionPhase.Failure.HostKeptChanging, vm.phase)
        assertNull(storage.values[MIRROR_CLIENT_NEEDS_REPAIR_KEY])
    }

    @Test
    fun transportFailureShowsConnectionLostWithoutNeedsRepair() = runTest {
        val vm = viewModel { _, _ -> error("connection reset") }

        vm.start()

        assertEquals(MirrorSessionPhase.Failure.ConnectionLost, vm.phase)
        assertNull(storage.values[MIRROR_CLIENT_NEEDS_REPAIR_KEY])
    }

    @Test
    fun midSession401WritesNeedsRepairAndShowsV7() = runTest {
        val vm = viewModel { _, _ -> throw MirrorNotPairedException() }

        vm.start()

        assertEquals(MirrorSessionPhase.Failure.PairingRejected, vm.phase)
        assertEquals("true", storage.values[MIRROR_CLIENT_NEEDS_REPAIR_KEY])
    }

    @Test
    fun pinningFailureWritesNeedsRepairAndShowsIdentityChanged() = runTest {
        val vm = viewModel(isPinning = { true }) { _, _ -> error("ssl handshake") }

        vm.start()

        assertEquals(MirrorSessionPhase.Failure.HostIdentityChanged, vm.phase)
        assertEquals("true", storage.values[MIRROR_CLIENT_NEEDS_REPAIR_KEY])
    }

    @Test
    fun cancelStopsTheSessionOnTheSteps() = runTest {
        val vm = viewModel { _, _ -> awaitCancellation() }

        vm.start()
        vm.cancel()

        assertIs<MirrorSessionPhase.Steps>(vm.phase)
        assertNull(storage.values[MIRROR_CLIENT_NEEDS_REPAIR_KEY])
    }

    @Test
    fun retryRunsAFreshSessionKeepingDecidedRows() = runTest {
        var round = 0
        val vm = viewModel { _, review ->
            round++
            when (round) {
                1 -> {
                    review(MirrorReview(listOf(conflict), emptyList()))
                    error("connection reset")
                }

                else -> {
                    review(MirrorReview(listOf(conflict), emptyList()))
                    MirrorSessionResult.Completed(outcome)
                }
            }
        }

        vm.start()
        vm.openReview()
        vm.chooseConflict(conflict, keepMine = true)
        vm.confirmReview()
        assertEquals(MirrorSessionPhase.Failure.ConnectionLost, vm.phase)

        vm.retry()

        assertIs<MirrorSessionPhase.Summary>(vm.phase)
    }
}

private class FakeSessionStorage : SecureStorage {
    val values = mutableMapOf<String, String>()
    override suspend fun get(key: String): String? = values[key]
    override suspend fun put(key: String, value: String) {
        values[key] = value
    }

    override suspend fun remove(key: String) {
        values.remove(key)
    }
}

private class NoopLocalStore : MirrorLocalStore {
    override suspend fun snapshot(): LibraryExport = LibraryExport()
    override suspend fun dismissals(): List<SyncDismissal> = emptyList()
    override suspend fun baseline(pairingId: String): LibraryExport? = null
    override suspend fun apply(pairingId: String, outcome: MirrorOutcome) = Unit
}
