package hr.kotwave.gameslibrary.psn

/** One PSN game the account owns or has played, deduped across the purchased and played lists. */
data class PsnOwnedGame(
    val conceptId: String?,
    val titleId: String,
    val name: String,
) {
    /** The per-account dedup key and unmatched `external_game` uid: conceptId when known, else titleId. */
    val uid: String get() = conceptId ?: titleId
}
