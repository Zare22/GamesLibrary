package hr.kotwave.gameslibrary.steam

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Builds the OpenID `return_to` for Steam sign-in. Routed through the hosted bounce page, Steam's
 * consent screen shows the app's domain; the raw loopback URL is the fallback when the page is
 * unreachable — sign-in still completes, consent shows the IP.
 */
class SteamBounce internal constructor(
    private val httpClient: HttpClient,
    private val bounceUrl: String = STEAM_BOUNCE_URL,
    private val timeout: Duration = PREFLIGHT_TIMEOUT,
) {
    /** True when the hosted bounce page answers 2xx within [timeout]. */
    suspend fun reachable(): Boolean = try {
        withTimeoutOrNull(timeout) { httpClient.get(bounceUrl).status.isSuccess() } ?: false
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    /** The `return_to` for [port]: the bounce page carrying the port as a query param, or the loopback callback. */
    fun returnTo(port: Int, viaBounce: Boolean): String =
        if (viaBounce) "$bounceUrl?port=$port" else "http://127.0.0.1:$port/callback"
}

internal const val STEAM_BOUNCE_URL = "https://gameslibrary.kotwave.hr/callback"

/** How long the pre-flight waits for the bounce page before falling back to the loopback `return_to`. */
internal val PREFLIGHT_TIMEOUT: Duration = 3.seconds
