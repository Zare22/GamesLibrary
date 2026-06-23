package hr.kotwave.gameslibrary.gog

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The persisted GOG session: the bearer token, its refresh token, and the absolute expiry (epoch seconds). */
@Serializable
data class GogToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
) {
    /** True once within [EXPIRY_SKEW_SECONDS] of expiry, so a sync refreshes before racing the boundary. */
    fun isExpired(nowEpochSeconds: Long): Boolean = nowEpochSeconds >= expiresAt - EXPIRY_SKEW_SECONDS

    fun encode(): String = GogTokenJson.encodeToString(this)

    companion object {
        fun decode(value: String): GogToken? =
            runCatching { GogTokenJson.decodeFromString<GogToken>(value) }.getOrNull()
    }
}

private const val EXPIRY_SKEW_SECONDS = 60L

private val GogTokenJson = Json { ignoreUnknownKeys = true }
