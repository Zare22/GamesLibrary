package hr.kotwave.gameslibrary.steam

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder

/**
 * Android Steam sign-in: a Custom Tab opens Steam's OpenID page with a loopback `return_to`; a one-shot
 * `127.0.0.1` server catches the redirect (RFC 8252). Cancelling closes the socket to unblock `accept()`.
 */
class AndroidSteamAuthFlow(private val context: Context) : SteamAuthFlow {

    override suspend fun authenticate(buildAuthUrl: (returnTo: String) -> String): Map<String, String>? =
        withTimeoutOrNull(AUTH_TIMEOUT) {
            withContext(Dispatchers.IO) {
                ServerSocket().use { server ->
                    server.bind(InetSocketAddress(InetAddress.getLoopbackAddress(), 0))
                    val returnTo = "http://127.0.0.1:${server.localPort}/callback"
                    launchCustomTab(buildAuthUrl(returnTo))
                    // Cancel/timeout closes the socket so the blocking accept() throws instead of hanging.
                    val closer = currentCoroutineContext().job.invokeOnCompletion { runCatching { server.close() } }
                    try {
                        server.accept().use { socket -> respondAndParse(socket) }
                    } catch (e: SocketException) {
                        currentCoroutineContext().ensureActive()
                        throw e
                    } finally {
                        closer.dispose()
                    }
                }
            }
        }

    private fun launchCustomTab(url: String) {
        val tabs = CustomTabsIntent.Builder().build()
        tabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        tabs.launchUrl(context, Uri.parse(url))
    }
}

/** Reads the redirect's request line, serves the done page, and returns its query params (null if empty). */
private fun respondAndParse(socket: Socket): Map<String, String>? {
    val requestLine = socket.getInputStream().bufferedReader().readLine() ?: return null
    val query = requestLine.substringAfter('?', "").substringBefore(' ')
    socket.getOutputStream().bufferedWriter().run {
        write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\n\r\n")
        write(STEAM_DONE_PAGE)
        flush()
    }
    return parseQuery(query).ifEmpty { null }
}

private fun parseQuery(query: String): Map<String, String> =
    query.split('&').filter { it.isNotEmpty() }.associate {
        URLDecoder.decode(it.substringBefore('='), "UTF-8") to URLDecoder.decode(it.substringAfter('=', ""), "UTF-8")
    }
