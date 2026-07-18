package hr.kotwave.gameslibrary.mirror.wire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The pairing payload the hosting screen presents (QR or typed fallback): where to connect, the
 * pairing secret, and the host cert's SHA-256 fingerprint (lowercase hex) that the client pins.
 */
@Serializable
data class MirrorPairingPayload(
    @SerialName("v") val version: Int = MIRROR_PROTOCOL_VERSION,
    val ip: String,
    val port: Int,
    val secret: String,
    @SerialName("fp") val fingerprint: String,
) {
    fun encode(): String = MirrorWireJson.encodeToString(serializer(), this)

    companion object {
        /** Null on malformed or incomplete input — a scanned QR is untrusted. */
        fun decode(text: String): MirrorPairingPayload? =
            runCatching { MirrorWireJson.decodeFromString(serializer(), text) }.getOrNull()
    }
}
