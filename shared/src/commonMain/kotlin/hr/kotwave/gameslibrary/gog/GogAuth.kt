package hr.kotwave.gameslibrary.gog

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import kotlin.time.Clock

/**
 * GOG OAuth2: [authUrl] is the browser destination, [exchangeCode] swaps the post-login `code` for a
 * token, [refresh] renews an expired one. Authorized by Galaxy's public client credentials; the user's
 * own token then authorizes the library API. The browser/redirect capture lives in `:composeApp`.
 */
class GogAuth internal constructor(
    private val httpClient: HttpClient,
    private val config: GogConfig,
    private val clock: Clock = Clock.System,
) {
    /**
     * The GOG login URL to open in a browser; GOG redirects to [GogConfig.redirectUri] with `?code=`.
     * No `layout=client2` — that Galaxy-client skin needs the native client's JS bridge to reveal its
     * login form and stays blank in a bare Android WebView; the default web login renders anywhere.
     */
    fun authUrl(): String =
        URLBuilder(config.authUrl).apply {
            parameters.append("client_id", config.clientId)
            parameters.append("redirect_uri", config.redirectUri)
            parameters.append("response_type", "code")
        }.buildString()

    /** Exchanges the authorization [code] for a [GogToken] (access + refresh + absolute expiry). */
    suspend fun exchangeCode(code: String): GogToken =
        token(
            listOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to config.redirectUri,
            ),
        )

    /** Renews a token from its [refreshToken]. */
    suspend fun refresh(refreshToken: String): GogToken =
        token(
            listOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
            ),
        )

    /**
     * Pulls the `code` from a pasted post-login redirect — a full `on_login_success?...&code=X` URL or a
     * bare code. Null if the input carries no usable code (e.g. an error redirect with no `code` param).
     */
    fun extractCode(redirect: String): String? {
        val trimmed = redirect.trim()
        if (trimmed.isEmpty()) return null
        val marker = "code="
        val idx = trimmed.indexOf(marker)
        val raw = if (idx >= 0) {
            trimmed.substring(idx + marker.length).substringBefore('&').substringBefore('#')
        } else {
            trimmed
        }
        return raw.takeIf { it.isNotBlank() && it.none(Char::isWhitespace) && '/' !in it }
    }

    private suspend fun token(grantParams: List<Pair<String, String>>): GogToken {
        val response = httpClient.get(config.tokenUrl) {
            parameter("client_id", config.clientId)
            parameter("client_secret", config.clientSecret)
            grantParams.forEach { (key, value) -> parameter(key, value) }
        }
        if (!response.status.isSuccess()) {
            throw GogException("GOG token request failed: ${response.status}")
        }
        val body = GogJson.decodeFromString<GogTokenResponse>(response.bodyAsText())
        return GogToken(
            accessToken = body.accessToken,
            refreshToken = body.refreshToken,
            expiresAt = clock.now().epochSeconds + body.expiresIn,
        )
    }
}
