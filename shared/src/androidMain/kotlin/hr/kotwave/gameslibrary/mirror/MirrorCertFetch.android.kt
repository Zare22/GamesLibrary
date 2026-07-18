package hr.kotwave.gameslibrary.mirror

import hr.kotwave.gameslibrary.mirror.wire.normalizeFingerprint
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun fetchMirrorCertFingerprint(ip: String, port: Int, timeoutMillis: Long): String =
    withContext(Dispatchers.IO) {
        val context = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(TrustAllManager()), null)
        }
        (context.socketFactory.createSocket() as SSLSocket).use { socket ->
            socket.soTimeout = timeoutMillis.toInt()
            socket.connect(InetSocketAddress(ip, port), timeoutMillis.toInt())
            socket.startHandshake()
            val leaf = socket.session.peerCertificates.first() as X509Certificate
            normalizeFingerprint(
                MessageDigest.getInstance("SHA-256").digest(leaf.encoded).joinToString("") { "%02x".format(it) },
            )
        }
    }

/** Accepts any server cert — this fetch only reads the identity; nothing is sent over the socket. */
private class TrustAllManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
