package hr.kotwave.gameslibrary.mirror

import hr.kotwave.gameslibrary.data.SyncDismissal
import hr.kotwave.gameslibrary.transfer.ExportedGame

/** The user-state fields a three-way merge can find changed on both sides to different values. */
enum class MirrorField { STATUS, USER_RATING, WISHLIST }

enum class MirrorConflictKind {
    /** A [MirrorConflict.fields] set changed on both sides to different values (or is unattributable on a first Mirror). */
    UserState,

    /** Deleted on one side, user-edited on the other; the null side of the conflict is the deletion. */
    DeleteVsEdit,
}

/**
 * One row pair needing a per-row Review decision ("this device / other device"). [merged] is the pair
 * merged on every non-conflicting field; resolution overlays the chosen side's [fields] onto it
 * (null for [MirrorConflictKind.DeleteVsEdit], where resolution keeps or drops a whole row).
 */
data class MirrorConflict(
    val kind: MirrorConflictKind,
    val mine: ExportedGame?,
    val theirs: ExportedGame?,
    val fields: Set<MirrorField> = emptySet(),
    val merged: ExportedGame? = null,
)

/** A Review decision on a [MirrorConflict]: keep this device's version (true) or the other device's. */
data class MirrorConflictDecision(val conflict: MirrorConflict, val keepMine: Boolean)

/**
 * A cross-key title collision: a manual row new since Baseline on one side, titled like an
 * IGDB-matched row on the other. Review decides merge-onto-matched vs keep both rows.
 */
data class MirrorCollision(val manual: ExportedGame, val matched: ExportedGame, val manualIsMine: Boolean)

/** True folds the manual row's Ownerships/externals onto the matched row; false keeps two rows. */
data class MirrorCollisionDecision(val collision: MirrorCollision, val merge: Boolean)

/** What one device must apply to arrive at the converged library; list sizes are the summary counts. */
data class MirrorSideChanges(
    val adds: List<ExportedGame> = emptyList(),
    val updates: List<ExportedGame> = emptyList(),
    val deletes: List<ExportedGame> = emptyList(),
)

/**
 * A fully resolved Mirror merge. [converged] is the library both devices arrive at — the push
 * payload and the next Baseline; [dismissals] is the union of both sides' sync-Review skip memory.
 */
data class MirrorOutcome(
    val converged: List<ExportedGame>,
    val mineChanges: MirrorSideChanges,
    val theirsChanges: MirrorSideChanges,
    val dismissals: List<SyncDismissal>,
)
