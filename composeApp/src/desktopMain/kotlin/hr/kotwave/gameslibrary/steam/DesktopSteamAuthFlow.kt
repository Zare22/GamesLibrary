package hr.kotwave.gameslibrary.steam

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Desktop
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder

/**
 * Desktop Steam sign-in: the system browser opens Steam's OpenID page with a loopback `return_to`; a
 * one-shot `127.0.0.1` HttpServer catches the redirect, serves a done page, then stops (RFC 8252).
 */
class DesktopSteamAuthFlow : SteamAuthFlow {

    override suspend fun authenticate(buildAuthUrl: (returnTo: String) -> String): Map<String, String>? {
        val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        val callback = CompletableDeferred<Map<String, String>>()
        server.createContext("/callback") { exchange ->
            val params = parseQuery(exchange.requestURI.rawQuery ?: "")
            val bytes = STEAM_DONE_PAGE.encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            callback.complete(params)
        }
        server.start()
        try {
            val returnTo = "http://127.0.0.1:${server.address.port}/callback"
            openBrowser(buildAuthUrl(returnTo))
            return withTimeoutOrNull(AUTH_TIMEOUT) { callback.await() }?.ifEmpty { null }
        } finally {
            server.stop(0)
        }
    }

    private fun openBrowser(url: String) {
        check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            "No system browser available for Steam sign-in"
        }
        Desktop.getDesktop().browse(URI(url))
    }
}

private fun parseQuery(query: String): Map<String, String> =
    query.split('&').filter { it.isNotEmpty() }.associate {
        URLDecoder.decode(it.substringBefore('='), "UTF-8") to URLDecoder.decode(it.substringAfter('=', ""), "UTF-8")
    }
