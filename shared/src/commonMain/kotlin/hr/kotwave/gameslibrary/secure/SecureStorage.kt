package hr.kotwave.gameslibrary.secure

/** A small key→string secret store, provided per platform via `platformModule`. */
interface SecureStorage {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)
}

/** The signed-in user's public SteamID64. */
const val STEAM_ID_KEY = "steam.id64"

/** The GOG session (bearer + refresh token + expiry), serialized as one JSON blob. A real secret. */
const val GOG_TOKEN_KEY = "gog.token"

/** The PSN session (bearer + refresh token + expiry), serialized as one JSON blob. A real secret. */
const val PSN_TOKEN_KEY = "psn.token"

/** The Epic session (bearer + refresh token + expiry), serialized as one JSON blob. A real secret. */
const val EPIC_TOKEN_KEY = "epic.token"
