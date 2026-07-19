package hr.kotwave.gameslibrary.mirror

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_NEEDS_REPAIR_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class MirrorStep { PULL, COMPARE, REVIEW, SEND, APPLY }

sealed interface MirrorSessionPhase {

    /** The step checklist. [rowsToDecide] is the undecided Review rows of the current attempt. */
    data class Steps(
        val current: MirrorStep = MirrorStep.PULL,
        val attempt: Int = 1,
        val pulledCount: Int? = null,
        val rowsToDecide: Int? = null,
        val awaitingReview: Boolean = false,
        val firstMirror: Boolean = false,
    ) : MirrorSessionPhase

    /** The Conflict Review, holding only the rows without a still-applicable decision. */
    data class Review(
        val conflicts: List<MirrorConflict>,
        val collisions: List<MirrorCollision>,
    ) : MirrorSessionPhase

    data class SideCounts(val added: Int, val updated: Int, val removed: Int) {
        val isEmpty: Boolean get() = added == 0 && updated == 0 && removed == 0
    }

    data class Summary(
        val gameCount: Int,
        val finishedAtMillis: Long,
        val mine: SideCounts,
        val theirs: SideCounts,
        val firstMirror: Boolean,
    ) : MirrorSessionPhase {
        val nothingChanged: Boolean get() = mine.isEmpty && theirs.isEmpty
    }

    sealed interface Failure : MirrorSessionPhase {
        /** V4: three pull→push rounds, a 409 every time. */
        data object HostKeptChanging : Failure

        /** V6: the transport dropped before the push committed. */
        data object ConnectionLost : Failure

        /** V7: the host answered 401 mid-session — the pairing is dead. */
        data object PairingRejected : Failure

        /** Pinning failed: the host presented a different cert than the pairing pinned. */
        data object HostIdentityChanged : Failure
    }
}

/**
 * One phone-initiated Mirror session over [MirrorSession.mirror]: progress events drive the step
 * checklist, the suspend Review callback parks on a [CompletableDeferred] the Review screen
 * completes, and decisions are cached by row value so a 409 retry only re-asks changed rows.
 */
class MirrorSessionViewModel(
    session: MirrorSession,
    private val secureStorage: SecureStorage,
    private val clock: Clock = Clock.System,
    private val isPinningFailure: (Throwable) -> Boolean = ::isMirrorPinningFailure,
    private val runMirror: suspend (
        onProgress: (MirrorProgress) -> Unit,
        review: suspend (MirrorReview) -> MirrorReviewDecisions,
    ) -> MirrorSessionResult = { onProgress, review -> session.mirror(onProgress, review) },
) : ViewModel() {

    var phase by mutableStateOf<MirrorSessionPhase>(MirrorSessionPhase.Steps())
        private set

    /** The Review screen's in-progress picks: Conflict → keep mine, collision → merge. */
    val conflictChoices = mutableStateMapOf<MirrorConflict, Boolean>()
    val collisionChoices = mutableStateMapOf<MirrorCollision, Boolean>()

    private var job: Job? = null
    private var steps = MirrorSessionPhase.Steps()
    private var currentReview: MirrorReview? = null
    private var pendingReview: MirrorReview? = null
    private var reviewCompletion: CompletableDeferred<MirrorReviewDecisions>? = null
    private val decidedConflicts = mutableMapOf<MirrorConflict, Boolean>()
    private val decidedCollisions = mutableMapOf<MirrorCollision, Boolean>()

    fun start() {
        if (job != null) return
        job = viewModelScope.launch { run() }
    }

    /** Cancel is only offered before the push window; the session's push→apply is non-cancellable. */
    fun cancel() {
        job?.cancel()
    }

    /** V4/V6 "Mirror again": a fresh session keeping the decision cache, like a 409 round. */
    fun retry() {
        if (job?.isActive == true) return
        job = null
        currentReview = null
        pendingReview = null
        reviewCompletion = null
        steps = MirrorSessionPhase.Steps()
        phase = steps
        start()
    }

    fun openReview() {
        val pending = pendingReview ?: return
        phase = MirrorSessionPhase.Review(pending.conflicts, pending.collisions)
    }

    fun backFromReview() {
        if (phase is MirrorSessionPhase.Review) phase = steps
    }

    fun chooseConflict(conflict: MirrorConflict, keepMine: Boolean) {
        conflictChoices[conflict] = keepMine
    }

    fun chooseCollision(collision: MirrorCollision, merge: Boolean) {
        collisionChoices[collision] = merge
    }

    fun confirmReview() {
        val review = currentReview ?: return
        val pending = pendingReview ?: return
        val decided = pending.conflicts.all { it in conflictChoices } &&
            pending.collisions.all { it in collisionChoices }
        if (!decided) return
        decidedConflicts.putAll(conflictChoices)
        decidedCollisions.putAll(collisionChoices)
        pendingReview = null
        steps = steps.copy(current = MirrorStep.SEND, awaitingReview = false)
        phase = steps
        reviewCompletion?.complete(decisionsFor(review))
    }

    private suspend fun run() {
        try {
            val result = runMirror(::onProgress, ::review)
            phase = when (result) {
                is MirrorSessionResult.Completed -> summaryOf(result.outcome)
                MirrorSessionResult.HostKeptChanging -> MirrorSessionPhase.Failure.HostKeptChanging
            }
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: MirrorNotPairedException) {
            secureStorage.put(MIRROR_CLIENT_NEEDS_REPAIR_KEY, "true")
            phase = MirrorSessionPhase.Failure.PairingRejected
        } catch (failure: Exception) {
            phase = if (isPinningFailure(failure)) {
                secureStorage.put(MIRROR_CLIENT_NEEDS_REPAIR_KEY, "true")
                MirrorSessionPhase.Failure.HostIdentityChanged
            } else {
                MirrorSessionPhase.Failure.ConnectionLost
            }
        }
    }

    private fun onProgress(progress: MirrorProgress) {
        steps = when (progress) {
            is MirrorProgress.Pulling -> steps.copy(
                current = MirrorStep.PULL,
                attempt = progress.attempt,
                pulledCount = null,
                rowsToDecide = null,
                awaitingReview = false,
            )

            is MirrorProgress.Pulled -> steps.copy(current = MirrorStep.COMPARE, pulledCount = progress.gameCount)
            is MirrorProgress.Compared -> steps.copy(
                current = MirrorStep.REVIEW,
                rowsToDecide = progress.rowsToDecide,
                firstMirror = progress.firstMirror,
            )

            MirrorProgress.Pushing -> steps.copy(current = MirrorStep.SEND, awaitingReview = false)
            MirrorProgress.Applied -> steps.copy(current = MirrorStep.APPLY)
        }
        phase = steps
    }

    private suspend fun review(review: MirrorReview): MirrorReviewDecisions {
        currentReview = review
        val newConflicts = review.conflicts.filterNot { it in decidedConflicts }
        val newCollisions = review.collisions.filterNot { it in decidedCollisions }
        if (newConflicts.isEmpty() && newCollisions.isEmpty()) return decisionsFor(review)

        conflictChoices.clear()
        collisionChoices.clear()
        pendingReview = MirrorReview(newConflicts, newCollisions)
        steps = steps.copy(awaitingReview = true, rowsToDecide = newConflicts.size + newCollisions.size)
        phase = steps
        val completion = CompletableDeferred<MirrorReviewDecisions>()
        reviewCompletion = completion
        return completion.await()
    }

    private fun decisionsFor(review: MirrorReview) = MirrorReviewDecisions(
        conflictDecisions = review.conflicts.map { MirrorConflictDecision(it, decidedConflicts.getValue(it)) },
        collisionDecisions = review.collisions.map { MirrorCollisionDecision(it, decidedCollisions.getValue(it)) },
    )

    private fun summaryOf(outcome: MirrorOutcome) = MirrorSessionPhase.Summary(
        gameCount = outcome.converged.size,
        finishedAtMillis = clock.now().toEpochMilliseconds(),
        mine = outcome.mineChanges.counts(),
        theirs = outcome.theirsChanges.counts(),
        firstMirror = steps.firstMirror,
    )

    private fun MirrorSideChanges.counts() =
        MirrorSessionPhase.SideCounts(added = adds.size, updated = updates.size, removed = deletes.size)
}
