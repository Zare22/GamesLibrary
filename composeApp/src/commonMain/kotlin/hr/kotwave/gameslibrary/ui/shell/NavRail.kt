package hr.kotwave.gameslibrary.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.action_add_game
import hr.kotwave.gameslibrary.resources.nav_rail_section
import hr.kotwave.gameslibrary.ui.components.BrandWordmark
import hr.kotwave.gameslibrary.ui.components.GlowBox
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

private val RailItemShape = RoundedCornerShape(12.dp)

/** Desktop left rail: brand, Add CTA, destinations. */
@Composable
fun NavRail(
    current: TopDestination?,
    onSelect: (TopDestination) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = AppTheme.tokens
    Column(
        modifier
            .width(232.dp)
            .fillMaxHeight()
            .background(Brush.verticalGradient(listOf(Color(0x04FFFFFF), Color.Transparent)))
            .drawBehind {
                drawLine(tokens.colors.border, Offset(size.width, 0f), Offset(size.width, size.height), 1f)
            }
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        BrandWordmark(Modifier.padding(start = 8.dp), style = AppTheme.type.brand.copy(fontSize = 22.sp))
        Spacer(Modifier.height(18.dp))
        PrimaryButton(stringResource(Res.string.action_add_game), onAdd, Modifier.fillMaxWidth(), leadingIcon = AppIcons.Plus)
        Spacer(Modifier.height(22.dp))
        Text(
            stringResource(Res.string.nav_rail_section).uppercase(),
            style = AppTheme.type.navLabel.copy(letterSpacing = 1.1.sp),
            color = tokens.colors.faint,
            modifier = Modifier.padding(start = 10.dp, bottom = 8.dp),
        )
        TopDestination.entries.forEachIndexed { index, destination ->
            if (index > 0) Spacer(Modifier.height(4.dp))
            RailItem(destination, active = destination == current, onClick = { onSelect(destination) })
        }
    }
}

@Composable
private fun RailItem(
    destination: TopDestination,
    active: Boolean,
    onClick: () -> Unit,
) {
    val tokens = AppTheme.tokens
    val activeBackground = Brush.linearGradient(
        listOf(tokens.colors.accent.copy(alpha = 0.16f), tokens.colors.brandGradient.last().copy(alpha = 0.08f)),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RailItemShape)
            .then(
                if (active) {
                    Modifier
                        .background(activeBackground)
                        .border(1.dp, tokens.colors.accent.copy(alpha = 0.32f), RailItemShape)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        GlowBox(glow = if (active) tokens.colors.accent else null, shape = CircleShape, glowRadius = 7.dp, glowAlpha = 0.7f) {
            Icon(
                destination.icon,
                destination.label(),
                Modifier.size(20.dp),
                tint = if (active) tokens.colors.accent else tokens.colors.muted,
            )
        }
        Text(
            destination.label(),
            style = AppTheme.type.bodyStrong,
            color = if (active) tokens.colors.text else tokens.colors.muted,
        )
    }
}
