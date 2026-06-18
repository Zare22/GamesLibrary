package hr.kotwave.gameslibrary.data

/** Outcome of a matched add: the local Game's id, and whether the `igdbId` was already present. */
data class MatchedAddResult(val gameId: Long, val alreadyExisted: Boolean)
