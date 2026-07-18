package hr.kotwave.gameslibrary.mirror

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.image.BufferedImage

/**
 * Renders [text] as a QR code of [sizePx]×[sizePx] with a two-module quiet zone baked in;
 * error correction M pairs with the typed fallback as the recovery path.
 */
fun qrImageBitmap(text: String, sizePx: Int): ImageBitmap {
    val matrix = QRCodeWriter().encode(
        text,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 2),
    )
    val image = BufferedImage(sizePx, sizePx, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until sizePx) {
        for (x in 0 until sizePx) {
            image.setRGB(x, y, if (matrix.get(x, y)) BLACK else WHITE)
        }
    }
    return image.toComposeImageBitmap()
}

private const val BLACK = 0x000000
private const val WHITE = 0xFFFFFF
