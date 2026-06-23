package hr.kotwave.gameslibrary.secure

/**
 * A small key→string secret store, provided per platform via `platformModule` (Android
 * Keystore-backed; Desktop a weak file — ADR 0003/0016). Built now for the public SteamID so later
 * per-user secrets (GOG refresh token, PSN NPSSO) drop in without new infrastructure.
 */
interface SecureStorage {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)
}

/** The signed-in user's public SteamID64. */
const val STEAM_ID_KEY = "steam.id64"

/** The GOG session (bearer + refresh token + expiry), serialized as one JSON blob. A real secret. */
const val GOG_TOKEN_KEY = "gog.token"
