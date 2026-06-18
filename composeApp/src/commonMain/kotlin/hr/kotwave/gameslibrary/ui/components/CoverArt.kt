package hr.kotwave.gameslibrary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import hr.kotwave.gameslibrary.igdb.IgdbImage

/** A game cover: the IGDB image when present, over a deterministic gradient stand-in. */
@Composable
fun CoverArt(
    title: String,
    coverImageId: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
) {
    Box(modifier.clip(shape).background(Brush.linearGradient(coverGradient(title)))) {
        if (coverImageId != null) {
            AsyncImage(
                model = IgdbImage.coverUrl(coverImageId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val CoverGradients = listOf(
    listOf(Color(0xFFCAA24A), Color(0xFF231405)),
    listOf(Color(0xFFB03A2E), Color(0xFF1C0A0A)),
    listOf(Color(0xFF3A5A8C), Color(0xFF05070F)),
    listOf(Color(0xFFFF5A3C), Color(0xFF1A0820)),
    listOf(Color(0xFF6FB04A), Color(0xFF234D2C)),
    listOf(Color(0xFF8A4DFF), Color(0xFF2A1240)),
    listOf(Color(0xFF2AA8D9), Color(0xFF0A2A4A)),
    listOf(Color(0xFFD9356A), Color(0xFF140510)),
)

/** Deterministic gradient stand-in for missing IGDB cover art (manual Games / while loading). */
internal fun coverGradient(title: String): List<Color> {
    val index = (title.hashCode() % CoverGradients.size + CoverGradients.size) % CoverGradients.size
    return CoverGradients[index]
}
