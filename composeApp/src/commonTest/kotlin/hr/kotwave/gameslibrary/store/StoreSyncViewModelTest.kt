package hr.kotwave.gameslibrary.store

import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.data.sync.SyncSummary
import hr.kotwave.gameslibrary.data.sync.SyncTailRow
import hr.kotwave.gameslibrary.importer.SyncReviewResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class StoreSyncViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun successAssignsSummaryAndTailAndClearsSyncing() = runTest {
        val tail = listOf(SyncTailRow(name = "Hollow Knight", uids = listOf("42")))
        val vm = TestSyncVM { setStage ->
            setOwnedCount(3)
            setStage(TestStage.Match)
            StoreSyncResult(SyncSummary(added = 2, updated = 1), tail)
        }

        vm.sync()

        assertEquals(3, vm.ownedCount)
        assertEquals(SyncSummary(2, 1), vm.lastSummary)
        assertEquals(tail, vm.reviewTail)
        assertFalse(vm.syncing)
        assertNull(vm.syncFailure)
    }

    @Test
    fun failureCapturesTheStageItReached() = runTest {
        val vm = TestSyncVM { setStage ->
            setOwnedCount(5)
            setStage(TestStage.Match)
            throw IllegalStateException("igdb unreachable")
        }

        vm.sync()

        assertEquals(TestStage.Match, vm.syncFailure)
        assertEquals(5, vm.ownedCount)
        assertFalse(vm.syncing)
    }

    @Test
    fun emptyLibrarySummarizesAsZero() = runTest {
        val vm = TestSyncVM { _ ->
            setOwnedCount(0)
            StoreSyncResult(SyncSummary(0, 0), emptyList())
        }

        vm.sync()

        assertEquals(SyncSummary(0, 0), vm.lastSummary)
        assertEquals(emptyList<SyncTailRow>(), vm.reviewTail)
        assertNull(vm.syncFailure)
    }

    @Test
    fun cancellationIsNotRecordedAsAFailure() = runTest {
        val vm = TestSyncVM { _ -> throw CancellationException("scope gone") }

        vm.sync()

        assertNull(vm.syncFailure)
        assertFalse(vm.syncing)
    }

    @Test
    fun reviewFoldsTheReturnedOutcomeAndDropsItsSettledRows() = runTest {
        val handled = SyncTailRow(name = "Celeste", uids = listOf("100"))
        val kept = SyncTailRow(name = "Tunic", uids = listOf("200"))
        val vm = TestSyncVM { _ ->
            setOwnedCount(2)
            StoreSyncResult(SyncSummary(added = 1, updated = 0), listOf(handled, kept))
        }
        vm.sync()

        var reviewed: Pair<Store, List<SyncTailRow>>? = null
        vm.review { store, rows ->
            reviewed = store to rows
            SyncReviewResult(added = 1, updated = 1, handledUids = setOf("100"))
        }

        assertEquals(Store.STEAM to listOf(handled, kept), reviewed)
        assertEquals(SyncSummary(added = 2, updated = 1), vm.lastSummary)
        assertEquals(listOf(kept), vm.reviewTail)
    }

    @Test
    fun anAbandonedReviewLeavesTheSummaryAndTailIntact() = runTest {
        val row = SyncTailRow(name = "Celeste", uids = listOf("100"))
        val vm = TestSyncVM { _ -> StoreSyncResult(SyncSummary(added = 1, updated = 0), listOf(row)) }
        vm.sync()

        vm.review { _, _ -> null }

        assertEquals(SyncSummary(added = 1, updated = 0), vm.lastSummary)
        assertEquals(listOf(row), vm.reviewTail)
    }

    @Test
    fun reviewIsANoOpWithoutATail() = runTest {
        val vm = TestSyncVM { _ -> StoreSyncResult(SyncSummary(0, 0), emptyList()) }
        vm.sync()

        var ran = false
        vm.review { _, _ ->
            ran = true
            null
        }

        assertFalse(ran)
    }
}

private enum class TestStage { First, Match, Merge }

private class TestSyncVM(
    private val onResolve: suspend TestSyncVM.((TestStage) -> Unit) -> StoreSyncResult,
) : StoreSyncViewModel<TestStage>() {
    override val store = Store.STEAM
    override val connected = true
    override val initialStage = TestStage.First
    override suspend fun resolve(setStage: (TestStage) -> Unit): StoreSyncResult = onResolve(this, setStage)
    override suspend fun clearConnection() = Unit
    fun setOwnedCount(value: Int) {
        ownedCount = value
    }
}
