package hr.kotwave.gameslibrary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.ui.theme.AppTheme

/** Library cover tile: gradient placeholder cover, store badges, and a status pip. */
@Composable
fun GameTile(
    title: String,
    stores: List<Store>,
    status: Status?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val tokens = AppTheme.tokens
    Box(
        modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(tokens.radii.tile))
            .border(1.dp, tokens.colors.border, RoundedCornerShape(tokens.radii.tile))
            .background(Brush.linearGradient(coverGradient(title)))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.38f to Color.Transparent, 1f to CoverScrim)))

        if (stores.isNotEmpty()) {
            StoreBadgeRow(stores = stores, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
        }
        if (status != null) {
            StatusDot(status = status, modifier = Modifier.align(Alignment.TopEnd).padding(9.dp))
        }
        Text(
            title,
            style = AppTheme.type.tileTitle,
            color = tokens.colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(9.dp),
        )
    }
}

private val CoverScrim = Color(0xC7000000) // black .78

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

/** Deterministic gradient stand-in for missing IGDB cover art (manual Games). */
private fun coverGradient(title: String): List<Color> {
    val index = (title.hashCode() % CoverGradients.size + CoverGradients.size) % CoverGradients.size
    return CoverGradients[index]
}
