package hr.kotwave.gameslibrary.psn

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The persisted PSN session: the bearer token, its refresh token, and the absolute expiry (epoch seconds). */
@Serializable
data class PsnToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
) {
    /** True once within [EXPIRY_SKEW_SECONDS] of expiry, so a sync refreshes before racing the boundary. */
    fun isExpired(nowEpochSeconds: Long): Boolean = nowEpochSeconds >= expiresAt - EXPIRY_SKEW_SECONDS

    fun encode(): String = PsnTokenJson.encodeToString(this)

    companion object {
        fun decode(value: String): PsnToken? =
            runCatching { PsnTokenJson.decodeFromString<PsnToken>(value) }.getOrNull()
    }
}

private const val EXPIRY_SKEW_SECONDS = 60L

private val PsnTokenJson = Json { ignoreUnknownKeys = true }
