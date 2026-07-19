package hr.kotwave.gameslibrary.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.sync.SyncSummary
import hr.kotwave.gameslibrary.data.sync.SyncTailRow
import hr.kotwave.gameslibrary.importer.SyncReviewResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** The outcome of a store's [StoreSyncViewModel.resolve]: the merged summary and the id-unmatched Review tail. */
data class StoreSyncResult(val summary: SyncSummary, val reviewTail: List<SyncTailRow>)

/**
 * The shared skeleton behind every store's additive Sync: owns the sync state, the [sync] scaffold,
 * [absorbReview], and [disconnect]. A subclass supplies the store-specific connect flow plus the
 * auth + entry resolution ([resolve]) and connection teardown ([clearConnection]); [F] is the store's
 * own sync-failure-stage enum, so each store keeps its exact stage set.
 */
abstract class StoreSyncViewModel<F : Any> : ViewModel() {

    var syncing by mutableStateOf(false)
        private set

    /** Games owned on the store from the last sync; null until a sync has run. Set by [resolve] once the store knows it. */
    var ownedCount by mutableStateOf<Int?>(null)
        protected set

    var lastSummary by mutableStateOf<SyncSummary?>(null)
        private set

    /** The last sync's needs-review tail: id-unmatched rows the Review picker can settle. */
    var reviewTail by mutableStateOf<List<SyncTailRow>>(emptyList())
        private set

    /** Which stage of the last sync failed, or null if it succeeded or hasn't run. */
    var syncFailure by mutableStateOf<F?>(null)
        private set

    abstract val connected: Boolean

    /** The stage a sync starts in: the store's first pipeline step (token refresh, or the fetch when there is no token). */
    protected abstract val initialStage: F

    /**
     * Runs the store leg — token refresh, fetch, IGDB match, tail split, and the merge — and returns the
     * final result. Reports progress through [setStage] so a throw is captured as the stage it reached,
     * and sets [ownedCount] at the point the store knows it. An empty library returns a zeroed result.
     */
    protected abstract suspend fun resolve(setStage: (F) -> Unit): StoreSyncResult

    /** Removes the persisted credential and resets the store-specific connect state. */
    protected abstract suspend fun clearConnection()

    /** Maps a caught sync exception to a failure stage; defaults to the stage the sync had reached. */
    protected open fun classifyFailure(error: Exception, stage: F): F = stage

    fun sync() {
        if (syncing || !connected) return
        syncing = true
        syncFailure = null
        reviewTail = emptyList()
        viewModelScope.launch {
            var stage = initialStage
            try {
                val result = resolve { stage = it }
                lastSummary = result.summary
                reviewTail = result.reviewTail
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                syncFailure = classifyFailure(e, stage)
            } finally {
                syncing = false
            }
        }
    }

    /** Folds a confirmed Review's merge counts into the last summary and drops its settled rows from the tail. */
    fun absorbReview(result: SyncReviewResult) {
        lastSummary = SyncSummary(
            added = (lastSummary?.added ?: 0) + result.added,
            updated = (lastSummary?.updated ?: 0) + result.updated,
        )
        reviewTail = reviewTail.filterNot { row -> row.uids.any { it in result.handledUids } }
    }

    fun disconnect() {
        viewModelScope.launch {
            clearConnection()
            ownedCount = null
            lastSummary = null
            reviewTail = emptyList()
            syncFailure = null
        }
    }
}
