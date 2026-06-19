package hr.kotwave.gameslibrary.steam

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.parameters

/**
 * "Sign in through Steam" via OpenID 2.0. [authUrl] is the browser destination; [verify] confirms the
 * redirect's assertion back with Steam (signature check) so a forged callback can't claim another id.
 * Pure logic over the Steam [HttpClient] — the browser/redirect leg lives in `:composeApp` (ADR 0001/0003).
 */
class SteamOpenId internal constructor(
    private val httpClient: HttpClient,
    private val endpoint: String = STEAM_OPENID_ENDPOINT,
) {
    /** The `checkid_setup` URL to open in a browser; Steam redirects back to [returnTo]. */
    fun authUrl(returnTo: String): String =
        URLBuilder(endpoint).apply {
            parameters.append("openid.ns", OPENID_NS)
            parameters.append("openid.mode", "checkid_setup")
            parameters.append("openid.return_to", returnTo)
            parameters.append("openid.realm", realmOf(returnTo))
            parameters.append("openid.identity", OPENID_IDENTIFIER_SELECT)
            parameters.append("openid.claimed_id", OPENID_IDENTIFIER_SELECT)
        }.buildString()

    /**
     * Verifies the redirect [params] with Steam (POST `check_authentication`) and returns the public
     * SteamID64, or null if the callback isn't a positive assertion or Steam reports it invalid. The
     * signed fields are echoed back verbatim with only `openid.mode` flipped, per OpenID 2.0.
     */
    suspend fun verify(params: Map<String, String>): String? {
        if (params["openid.mode"] != "id_res") return null
        val steamId = params["openid.claimed_id"]?.let { CLAIMED_ID.matchEntire(it)?.groupValues?.get(1) }
            ?: return null

        val form = parameters {
            params.filterKeys { it.startsWith("openid.") }.forEach { (key, value) -> append(key, value) }
            this["openid.mode"] = "check_authentication"
        }
        val response = httpClient.submitForm(url = endpoint, formParameters = form)
        if (!response.status.isSuccess()) return null
        val valid = response.bodyAsText().lineSequence().any { it.trim() == "is_valid:true" }
        return steamId.takeIf { valid }
    }
}

/** OpenID realm = the origin of [returnTo] (scheme://host[:port]); Steam requires return_to within it. */
private fun realmOf(returnTo: String): String {
    val schemeEnd = returnTo.indexOf("://")
    if (schemeEnd < 0) return returnTo
    val pathStart = returnTo.indexOf('/', startIndex = schemeEnd + 3)
    return if (pathStart < 0) returnTo else returnTo.substring(0, pathStart)
}

private const val STEAM_OPENID_ENDPOINT = "https://steamcommunity.com/openid/login"
private const val OPENID_NS = "http://specs.openid.net/auth/2.0"
private const val OPENID_IDENTIFIER_SELECT = "http://specs.openid.net/auth/2.0/identifier_select"
private val CLAIMED_ID = Regex("""https?://steamcommunity\.com/openid/id/(\d+)""")
