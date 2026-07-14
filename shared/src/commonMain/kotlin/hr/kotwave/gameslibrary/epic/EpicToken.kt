package hr.kotwave.gameslibrary.epic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The persisted Epic session: the bearer token, its refresh token, and the absolute expiry (epoch
 * seconds). Epic rotates the refresh token on every refresh — always persist the newest [EpicToken].
 */
@Serializable
data class EpicToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
) {
    /** True once within [EXPIRY_SKEW_SECONDS] of expiry, so a sync refreshes before racing the boundary. */
    fun isExpired(nowEpochSeconds: Long): Boolean = nowEpochSeconds >= expiresAt - EXPIRY_SKEW_SECONDS

    fun encode(): String = EpicTokenJson.encodeToString(this)

    companion object {
        fun decode(value: String): EpicToken? =
            runCatching { EpicTokenJson.decodeFromString<EpicToken>(value) }.getOrNull()
    }
}

private const val EXPIRY_SKEW_SECONDS = 60L

private val EpicTokenJson = Json { ignoreUnknownKeys = true }
