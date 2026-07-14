package hr.kotwave.gameslibrary.epic

open class EpicException(message: String) : Exception(message)

/** The token endpoint rejected the pasted authorizationCode — invalid, already used, or expired (~5 min). */
class EpicCodeRejectedException : EpicException("Epic token exchange rejected the authorization code")
