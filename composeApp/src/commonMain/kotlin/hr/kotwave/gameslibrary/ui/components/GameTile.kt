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

/** Library cover tile: IGDB cover (or a gradient stand-in), store badges, and a status pip. */
@Composable
fun GameTile(
    title: String,
    stores: List<Store>,
    status: Status?,
    modifier: Modifier = Modifier,
    coverImageId: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.tile)
    Box(
        modifier
            .aspectRatio(3f / 4f)
            .clip(shape)
            .border(1.dp, tokens.colors.border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        CoverArt(title = title, coverImageId = coverImageId, modifier = Modifier.matchParentSize(), shape = shape)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to TopScrim, 0.28f to Color.Transparent)))
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
private val TopScrim = Color(0x70000000) // black .44 — backs the top-corner badges
