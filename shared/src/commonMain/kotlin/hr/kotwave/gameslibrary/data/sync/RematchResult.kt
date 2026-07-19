package hr.kotwave.gameslibrary.data.sync

/** Outcome of repointing a Game's `igdb_id` to a chosen IGDB entry. */
sealed interface RematchResult {
    data object Success : RematchResult

    /** Another Game already holds that `igdb_id` (Match is unique) — re-match is blocked. */
    data class AlreadyInLibrary(val existingGameId: Long) : RematchResult
}
