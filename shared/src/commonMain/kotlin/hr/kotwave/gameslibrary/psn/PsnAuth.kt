package hr.kotwave.gameslibrary.psn

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlin.time.Clock

/**
 * PSN OAuth2 via the npsso cookie: the user signs in at [signInUrl], copies the value shown at
 * [npssoUrl], and [exchangeNpsso] swaps it for a [PsnToken]; [refresh] renews an expired one.
 * Authorized by the PlayStation App's public client credentials; the browser/paste capture lives in
 * `:composeApp`. The injected [httpClient] must keep redirects unfollowed — the authorize `code` is
 * read off the 302 Location.
 */
class PsnAuth internal constructor(
    private val httpClient: HttpClient,
    private val config: PsnConfig,
    private val clock: Clock = Clock.System,
) {
    /** Where the user signs in first (system browser); the npsso only exists behind this session. */
    fun signInUrl(): String = config.signInUrl

    /** The page that shows `{"npsso":"…"}` once signed in; its value is what the user pastes. */
    fun npssoUrl(): String = config.npssoUrl

    /**
     * Pulls the npsso out of a paste — the page's whole `{"npsso":"…"}` JSON or the bare value
     * (64 chars today). Null if the input carries nothing token-shaped (e.g. an error JSON).
     */
    fun extractNpsso(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val candidate = NPSSO_JSON.find(trimmed)?.groupValues?.get(1) ?: trimmed
        return candidate.takeIf {
            it.length >= NPSSO_MIN_LENGTH && it.none(Char::isWhitespace) && it.none { c -> c in "\"{}:,/" }
        }
    }

    /** Exchanges a pasted [npsso] for a [PsnToken]: authorize (code off the 302 Location) + token. */
    suspend fun exchangeNpsso(npsso: String): PsnToken {
        val response = httpClient.get(config.authorizeUrl) {
            parameter("access_type", "offline")
            parameter("client_id", config.clientId)
            parameter("redirect_uri", config.redirectUri)
            parameter("response_type", "code")
            parameter("scope", config.scope)
            header(HttpHeaders.Cookie, "npsso=$npsso")
        }
        val location = response.headers[HttpHeaders.Location]
        val code = location
            ?.takeIf { "code=" in it }
            ?.substringAfter("code=")?.substringBefore('&')
            ?.takeIf { it.isNotBlank() }
            ?: throw PsnNpssoRejectedException()
        return token(
            listOf(
                "code" to code,
                "redirect_uri" to config.redirectUri,
                "grant_type" to "authorization_code",
                "token_format" to "jwt",
            ),
        )
    }

    /** Renews a token from its [refreshToken]. */
    suspend fun refresh(refreshToken: String): PsnToken =
        token(
            listOf(
                "refresh_token" to refreshToken,
                "grant_type" to "refresh_token",
                "token_format" to "jwt",
                "scope" to config.scope,
            ),
        )

    private suspend fun token(grantParams: List<Pair<String, String>>): PsnToken {
        val response = httpClient.submitForm(
            url = config.tokenUrl,
            formParameters = parameters { grantParams.forEach { (key, value) -> append(key, value) } },
        ) {
            header(HttpHeaders.Authorization, "Basic ${config.basicAuth}")
        }
        if (!response.status.isSuccess()) {
            throw PsnException("PSN token request failed: ${response.status}")
        }
        val body = PsnJson.decodeFromString<PsnTokenResponse>(response.bodyAsText())
        return PsnToken(
            accessToken = body.accessToken,
            refreshToken = body.refreshToken,
            expiresAt = clock.now().epochSeconds + body.expiresIn,
        )
    }
}

private val NPSSO_JSON = Regex("\"npsso\"\\s*:\\s*\"([^\"]+)\"")

/** Floor for a plausible npsso paste; the real value is 64 chars, garbage and error pages are shorter. */
private const val NPSSO_MIN_LENGTH = 32
