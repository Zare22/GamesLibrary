package hr.kotwave.gameslibrary.igdb

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Holds the Twitch app access token (IGDB's client-credentials auth), fetching it on demand and
 * caching it until shortly before expiry. [invalidate] forces a refetch after a 401.
 */
internal class TwitchTokenProvider(
    private val httpClient: HttpClient,
    private val config: IgdbConfig,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val mutex = Mutex()
    private var token: String? = null
    private var expiresAt: TimeMark? = null

    suspend fun token(): String = mutex.withLock {
        val current = token
        val deadline = expiresAt
        if (current != null && deadline != null && deadline.hasNotPassedNow()) current else fetch()
    }

    suspend fun invalidate() = mutex.withLock {
        token = null
        expiresAt = null
    }

    private suspend fun fetch(): String {
        val body = httpClient.post(config.authUrl) {
            parameter("client_id", config.clientId)
            parameter("client_secret", config.clientSecret)
            parameter("grant_type", "client_credentials")
        }.bodyAsText()
        val response = IgdbJson.decodeFromString<TwitchToken>(body)
        token = response.accessToken
        expiresAt = timeSource.markNow() + (response.expiresIn - 60).coerceAtLeast(0).seconds
        return response.accessToken
    }
}

@Serializable
private data class TwitchToken(
    val accessToken: String,
    val expiresIn: Long,
    val tokenType: String,
)
