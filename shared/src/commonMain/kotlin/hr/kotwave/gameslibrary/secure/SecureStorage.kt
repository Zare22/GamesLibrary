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

/** The Mirror client's long-lived session token, issued by the paired desktop host. A real secret. */
const val MIRROR_CLIENT_TOKEN_KEY = "mirror.client.token"

/** The pinned Mirror host cert's SHA-256 fingerprint (lowercase hex) — also the pairingId. */
const val MIRROR_CLIENT_HOST_FINGERPRINT_KEY = "mirror.client.host.fingerprint"

/** The Mirror host's last-known `ip:port`, for the silent re-Mirror reconnect. */
const val MIRROR_CLIENT_HOST_ENDPOINT_KEY = "mirror.client.host.endpoint"

/** When this device paired with its Mirror host, as epoch millis — the paired card's date. */
const val MIRROR_CLIENT_PAIRED_AT_KEY = "mirror.client.paired.at"

/** Set when pinning fails against the paired host (its identity changed) — the card asks to pair again. */
const val MIRROR_CLIENT_NEEDS_REPAIR_KEY = "mirror.client.needs.repair"

/** When the last Mirror completed on this device, as epoch millis — the paired card's last-Mirror line. */
const val MIRROR_CLIENT_LAST_MIRROR_AT_KEY = "mirror.client.last.mirror.at"

/** The password of the Mirror host's TLS keystore file; without it the cert regenerates. */
const val MIRROR_HOST_KEYSTORE_PASSWORD_KEY = "mirror.host.keystore.password"

/** SHA-256 of the token issued to the paired Mirror client; the token itself is never stored. */
const val MIRROR_HOST_TOKEN_HASH_KEY = "mirror.host.token.hash"
