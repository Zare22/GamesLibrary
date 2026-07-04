package hr.kotwave.gameslibrary.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.ui.components.GlowBox
import hr.kotwave.gameslibrary.ui.theme.AppTheme

private val NavTopGradient = listOf(Color(0x660B0D12), Color(0xEB080A0F))

/** Phone bottom nav; the active item shows an accent pip. */
@Composable
fun BottomNavBar(
    current: TopDestination?,
    onSelect: (TopDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = AppTheme.tokens
    Row(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Brush.verticalGradient(NavTopGradient))
            .drawBehind {
                drawLine(
                    color = tokens.colors.border,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 1f,
                )
            }
            .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        TopDestination.entries.forEach { destination ->
            NavItem(
                destination = destination,
                active = destination == current,
                onClick = { onSelect(destination) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavItem(
    destination: TopDestination,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = AppTheme.tokens
    val color = if (active) tokens.colors.text else tokens.colors.faint
    Column(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.micro),
    ) {
        GlowBox(glow = if (active) tokens.colors.accent else null, shape = CircleShape, glowRadius = 7.dp, glowAlpha = 0.7f) {
            Icon(destination.icon, destination.label(), Modifier.size(21.dp), tint = color)
        }
        Text(destination.label(), style = AppTheme.type.navLabel, color = color)
        if (active) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(tokens.colors.accent))
        }
    }
}
