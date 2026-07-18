package hr.kotwave.gameslibrary.mirror.wire

import hr.kotwave.gameslibrary.data.SyncDismissal
import hr.kotwave.gameslibrary.transfer.ExportedGame
import hr.kotwave.gameslibrary.transfer.LibraryExport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MirrorWireTest {

    private val snapshot = LibraryExport(
        games = listOf(ExportedGame(name = "Hollow Knight", igdbId = 14593, status = "PLAYING")),
    )

    @Test
    fun pullResponseRoundTrips() {
        val original = MirrorPullResponse(
            snapshot = snapshot,
            dismissals = listOf(WireDismissal(1, "570")),
            snapshotHash = "a3f0",
        )
        val decoded = MirrorWireJson.decodeFromString(
            MirrorPullResponse.serializer(),
            MirrorWireJson.encodeToString(MirrorPullResponse.serializer(), original),
        )
        assertEquals(original, decoded)
    }

    @Test
    fun pushRequestRoundTrips() {
        val original = MirrorPushRequest(
            pulledHash = "a3f0",
            converged = snapshot,
            hostChanges = WireSideChanges(adds = snapshot.games),
            dismissals = listOf(WireDismissal(2, "1091500")),
        )
        val decoded = MirrorWireJson.decodeFromString(
            MirrorPushRequest.serializer(),
            MirrorWireJson.encodeToString(MirrorPushRequest.serializer(), original),
        )
        assertEquals(original, decoded)
    }

    @Test
    fun pairEnvelopesRoundTrip() {
        val request = MirrorPairRequest(secret = "123456")
        assertEquals(
            request,
            MirrorWireJson.decodeFromString(
                MirrorPairRequest.serializer(),
                MirrorWireJson.encodeToString(MirrorPairRequest.serializer(), request),
            ),
        )
        val response = MirrorPairResponse(token = "tok")
        assertEquals(
            response,
            MirrorWireJson.decodeFromString(
                MirrorPairResponse.serializer(),
                MirrorWireJson.encodeToString(MirrorPairResponse.serializer(), response),
            ),
        )
    }

    @Test
    fun decodeToleratesUnknownKeys() {
        val decoded = MirrorWireJson.decodeFromString(
            MirrorPullResponse.serializer(),
            """{"protocolVersion":1,"snapshot":{"schemaVersion":1,"games":[]},"snapshotHash":"x","futureField":true}""",
        )
        assertEquals("x", decoded.snapshotHash)
    }

    @Test
    fun pairingPayloadRoundTripsWithCompactKeys() {
        val payload = MirrorPairingPayload(ip = "192.168.1.10", port = 56789, secret = "123456", fingerprint = "aabb")
        val encoded = payload.encode()
        assertTrue("\"v\":" in encoded)
        assertTrue("\"fp\":" in encoded)
        assertEquals(payload, MirrorPairingPayload.decode(encoded))
    }

    @Test
    fun pairingPayloadDecodeReturnsNullOnGarbage() {
        assertNull(MirrorPairingPayload.decode("not json"))
        assertNull(MirrorPairingPayload.decode("""{"v":1,"ip":"192.168.1.10"}"""))
    }

    @Test
    fun normalizeFingerprintLowercasesAndStripsColons() {
        assertEquals("aabbcc", normalizeFingerprint(" AA:BB:cc "))
    }

    @Test
    fun mirrorVerifyCodeIsFirstTwelveHexUppercaseGrouped() {
        assertEquals("AB12 · CD34 · EF56", mirrorVerifyCode("ab12cd34ef567890aabbccdd"))
        assertEquals("AB12 · CD34 · EF56", mirrorVerifyCode(" AB:12:CD:34:EF:56:78:90 "))
    }

    @Test
    fun pairFailureRoundTrips() {
        val failure = MirrorPairFailure(remainingAttempts = 3)
        assertEquals(
            failure,
            MirrorWireJson.decodeFromString(
                MirrorPairFailure.serializer(),
                MirrorWireJson.encodeToString(MirrorPairFailure.serializer(), failure),
            ),
        )
    }

    @Test
    fun dismissalConvertersRoundTrip() {
        val dismissal = SyncDismissal(category = 1, uid = "570")
        assertEquals(dismissal, dismissal.toWire().toDismissal())
    }
}
