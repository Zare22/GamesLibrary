package hr.kotwave.gameslibrary.mirror

import androidx.compose.ui.graphics.toAwtImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import hr.kotwave.gameslibrary.mirror.wire.MirrorPairingPayload
import kotlin.test.Test
import kotlin.test.assertEquals

class QrImageTest {

    @Test
    fun qrRoundTripsThePairingPayload() {
        val payload = MirrorPairingPayload(
            ip = "192.168.1.24",
            port = 56789,
            secret = "743291",
            fingerprint = "1f4a9c21b8d3aabbccdd00112233445566778899aabbccdd0011223344556677",
        )
        val size = 320
        val bitmap = qrImageBitmap(payload.encode(), size)
        val pixels = IntArray(size * size)
        bitmap.toAwtImage().getRGB(0, 0, size, size, pixels, 0, size)

        val decoded = QRCodeReader().decode(
            BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels))),
        )

        assertEquals(payload.encode(), decoded.text)
        assertEquals(payload, MirrorPairingPayload.decode(decoded.text))
    }
}
