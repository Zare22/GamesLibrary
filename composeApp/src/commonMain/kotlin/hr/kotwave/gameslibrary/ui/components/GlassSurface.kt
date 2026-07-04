package hr.kotwave.gameslibrary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.ui.theme.AppTheme

/**
 * Draws a blurred colored halo behind [content], matching its size. The element inside [content]
 * carries the sizing modifier. A null [glow] makes this a plain wrapping Box.
 */
@Composable
fun GlowBox(
    glow: Color?,
    shape: Shape,
    modifier: Modifier = Modifier,
    glowRadius: Dp = 16.dp,
    glowAlpha: Float = 0.55f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        if (glow != null) {
            Box(
                Modifier
                    .matchParentSize()
                    .blur(glowRadius, BlurredEdgeTreatment.Unbounded)
                    .background(glow.copy(alpha = glowAlpha), shape),
            )
        }
        content()
    }
}

/** Translucent fill + hairline border + optional colored glow. [modifier] sizes the surface. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    fill: Color = AppTheme.tokens.colors.surface,
    borderColor: Color = AppTheme.tokens.colors.border,
    glow: Color? = null,
    glowRadius: Dp = AppTheme.tokens.glass.blurRadius,
    content: @Composable BoxScope.() -> Unit = {},
) {
    GlowBox(glow = glow, shape = shape, glowRadius = glowRadius) {
        Box(
            modifier
                .clip(shape)
                .background(fill)
                .border(1.dp, borderColor, shape),
            content = content,
        )
    }
}
