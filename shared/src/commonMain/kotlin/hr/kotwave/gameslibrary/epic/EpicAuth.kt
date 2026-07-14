package hr.kotwave.gameslibrary.epic

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlin.time.Clock

/**
 * Epic OAuth2 via the launcher's public client: the user signs in at [signInUrl] (a real browser —
 * Epic's hCaptcha breaks WebViews), the redirect page renders `{"authorizationCode":"…"}`, and
 * [exchangeCode] swaps the pasted code for an [EpicToken]; [refresh] renews an expired one. Every
 * refresh response carries a NEW refresh token — callers persist the returned token, always.
 */
class EpicAuth internal constructor(
    private val httpClient: HttpClient,
    private val config: EpicConfig,
    private val clock: Clock = Clock.System,
) {
    /** Epic sign-in, bouncing to the page that shows `{"authorizationCode":"…"}`; its value is the paste. */
    fun signInUrl(): String = config.signInUrl

    /**
     * Pulls the authorizationCode out of a paste — the page's whole JSON or the bare 32-hex value.
     * Null if the input carries nothing code-shaped (e.g. the signed-out error JSON).
     */
    fun extractAuthorizationCode(raw: String): String? =
        AUTHORIZATION_CODE_JSON.find(raw)?.groupValues?.get(1)
            ?: BARE_CODE.find(raw)?.value

    /** Exchanges a pasted authorization [code] (single-use, ~5 min lifetime) for an [EpicToken]. */
    suspend fun exchangeCode(code: String): EpicToken =
        token(
            listOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "token_type" to "eg1",
            ),
            rejectStatus = HttpStatusCode.BadRequest,
        )

    /** Renews a token from its [refreshToken]; the result carries the ROTATED refresh token — persist it. */
    suspend fun refresh(refreshToken: String): EpicToken =
        token(
            listOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "token_type" to "eg1",
            ),
        )

    private suspend fun token(
        grantParams: List<Pair<String, String>>,
        rejectStatus: HttpStatusCode? = null,
    ): EpicToken {
        val response = httpClient.submitForm(
            url = config.tokenUrl,
            formParameters = parameters { grantParams.forEach { (key, value) -> append(key, value) } },
        ) {
            header(HttpHeaders.Authorization, "Basic ${config.basicAuth}")
        }
        if (response.status == rejectStatus) throw EpicCodeRejectedException()
        if (!response.status.isSuccess()) {
            throw EpicException("Epic token request failed: ${response.status}")
        }
        val body = EpicJson.decodeFromString<EpicTokenResponse>(response.bodyAsText())
        return EpicToken(
            accessToken = body.accessToken,
            refreshToken = body.refreshToken,
            expiresAt = clock.now().epochSeconds + body.expiresIn,
        )
    }
}

private val AUTHORIZATION_CODE_JSON = Regex("\"authorizationCode\"\\s*:\\s*\"([0-9a-f]{32})\"")

private val BARE_CODE = Regex("\\b[0-9a-f]{32}\\b")
