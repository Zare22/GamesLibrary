package hr.kotwave.gameslibrary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.ui.model.Status
import hr.kotwave.gameslibrary.ui.theme.AppTheme

private val PipRing = Color(0xB3080A10) // rgba(8,10,16,.7)

/** Color-only status pip; glows for all but Backlog. */
@Composable
fun StatusDot(
    status: Status,
    modifier: Modifier = Modifier,
    size: Dp = 11.dp,
    bordered: Boolean = true,
) {
    val tokens = AppTheme.tokens
    val color = tokens.status.color(status)
    val glow = if (tokens.status.glows(status)) color else null
    GlowBox(glow = glow, shape = CircleShape, glowRadius = 5.dp, glowAlpha = 0.85f) {
        Box(
            modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
                .then(if (bordered) Modifier.border(1.5.dp, PipRing, CircleShape) else Modifier),
        )
    }
}
