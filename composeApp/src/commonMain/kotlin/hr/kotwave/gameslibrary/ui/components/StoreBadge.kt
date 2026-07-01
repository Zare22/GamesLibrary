package hr.kotwave.gameslibrary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.ui.model.glyph
import hr.kotwave.gameslibrary.ui.theme.AppTheme

private val BadgeFill = Color(0x8C080A10) // rgba(8,10,16,.55)
private val BadgeShape = RoundedCornerShape(7.dp)

/** Glyph badge in the store's accent color, with its glow. */
@Composable
fun StoreBadge(
    store: Store,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    val tokens = AppTheme.tokens
    val accent = tokens.store.accent(store)
    val glow = if (tokens.store.glows(store)) accent else null
    GlowBox(glow = glow, shape = BadgeShape, modifier = modifier, glowRadius = 8.dp, glowAlpha = 0.7f) {
        Box(
            Modifier
                .size(size)
                .clip(BadgeShape)
                .background(BadgeFill)
                .border(1.dp, accent.copy(alpha = 0.45f), BadgeShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(store.glyph, style = AppTheme.type.brand.copy(fontSize = 10.sp), color = tokens.store.glyph(store))
        }
    }
}

/** The muted "+N" overflow badge. */
@Composable
fun MoreBadge(
    count: Int,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    val tokens = AppTheme.tokens
    Box(
        modifier
            .size(size)
            .clip(BadgeShape)
            .background(BadgeFill)
            .border(1.dp, tokens.colors.border, BadgeShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("+$count", style = AppTheme.type.brand.copy(fontSize = 9.sp), color = tokens.colors.muted)
    }
}

/** Store badges, collapsing anything past [max] into a "+N" badge. */
@Composable
fun StoreBadgeRow(
    stores: List<Store>,
    modifier: Modifier = Modifier,
    max: Int = 2,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val shown = stores.take(max)
        shown.forEach { StoreBadge(it) }
        val overflow = stores.size - shown.size
        if (overflow > 0) MoreBadge(overflow)
    }
}
