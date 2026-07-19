package hr.kotwave.gameslibrary.mirror

import java.io.IOException
import java.security.cert.CertificateException
import javax.net.ssl.SSLHandshakeException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MirrorPinningFailureTest {

    @Test
    fun certificateExceptionAnywhereInTheChainIsAPinningFailure() {
        val wrapped = SSLHandshakeException("handshake failed").apply {
            initCause(CertificateException("Presented certificate is not the paired Mirror host"))
        }
        assertTrue(isMirrorPinningFailure(IOException("request failed", wrapped)))
        assertTrue(isMirrorPinningFailure(CertificateException("bare")))
    }

    @Test
    fun plainNetworkAndTlsFailuresAreNot() {
        assertFalse(isMirrorPinningFailure(IOException("connection refused")))
        assertFalse(isMirrorPinningFailure(SSLHandshakeException("protocol mismatch")))
    }
}
