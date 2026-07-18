package hr.kotwave.gameslibrary.mirror

import hr.kotwave.gameslibrary.mirror.wire.normalizeFingerprint
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

internal actual fun mirrorEngine(certFingerprint: String): HttpClientEngine = OkHttp.create {
    val pinned = PinnedCertTrustManager(normalizeFingerprint(certFingerprint))
    val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(pinned), null) }
    config {
        sslSocketFactory(ssl.socketFactory, pinned)
        // The pinned-cert check replaces hostname verification: the QR fingerprint is the identity.
        hostnameVerifier { _, _ -> true }
    }
}

/** Trusts exactly the paired cert: the presented leaf's SHA-256 must equal the pinned fingerprint. */
private class PinnedCertTrustManager(private val fingerprint: String) : X509TrustManager {

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String): Unit =
        throw CertificateException("Mirror does not accept client certificates")

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        val presented = MessageDigest.getInstance("SHA-256").digest(chain[0].encoded)
            .joinToString("") { "%02x".format(it) }
        if (presented != fingerprint) {
            throw CertificateException("Presented certificate is not the paired Mirror host")
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
