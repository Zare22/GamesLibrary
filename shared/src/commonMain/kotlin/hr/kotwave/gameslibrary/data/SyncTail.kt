package hr.kotwave.gameslibrary.data

/**
 * One id-unmatched sync row headed for the Review picker: the store's display [name] and every store
 * [uids] entry behind it (one appid/product id for Steam/GOG; titleIds + conceptId for PSN,
 * catalogItemIds + offerId for Epic).
 */
data class SyncTailRow(val name: String, val uids: List<String>)

/**
 * An id-unmatched tail partitioned for Review: [known] rows already have a Game holding one of their
 * uids (merged for ownership, never re-offered), [needsReview] rows go to the picker. Dismissed rows
 * are dropped.
 */
data class SyncTailSplit(val known: List<SyncTailRow>, val needsReview: List<SyncTailRow>)

/** A Review pick: the user-chosen IGDB match for a tail row, with the row's store uids. */
data class SyncReviewPick(val igdb: IgdbGame, val uids: List<String>)

/** The merge outcome of a confirmed sync Review, in the sync summaries' added/updated shape. */
data class SyncReviewOutcome(val added: Int, val updated: Int)
