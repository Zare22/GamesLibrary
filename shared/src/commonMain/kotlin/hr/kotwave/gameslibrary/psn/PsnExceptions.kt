package hr.kotwave.gameslibrary.psn

open class PsnException(message: String) : Exception(message)

/** The authorize leg returned no code: the pasted npsso is invalid or expired (~2-month lifetime). */
class PsnNpssoRejectedException : PsnException("PSN authorize returned no code — npsso invalid or expired")

/** Sony rejected the persisted query's sha256 hash (PersistedQueryNotFound) — the hash has rotated. */
class PsnQueryOutdatedException : PsnException("PSN persisted query rejected — the sha256 hash has rotated")
