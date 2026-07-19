package hr.kotwave.gameslibrary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.action_delete
import hr.kotwave.gameslibrary.resources.cd_close
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    GlowBox(
        glow = if (enabled) tokens.colors.brandGradient.last() else null,
        shape = shape,
        glowRadius = 20.dp,
        glowAlpha = 0.45f,
    ) {
        Row(
            modifier
                .clip(shape)
                .background(Brush.linearGradient(tokens.colors.brandGradient))
                .clickable(enabled = enabled, onClick = onClick)
                .heightIn(min = 50.dp)
                .padding(horizontal = tokens.spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs, Alignment.CenterHorizontally),
        ) {
            if (leadingIcon != null) Icon(leadingIcon, null, Modifier.size(18.dp), tint = Color.White)
            Text(text, style = AppTheme.type.button.copy(fontSize = 15.sp), color = Color.White)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Row(
        modifier
            .clip(shape)
            .background(tokens.colors.surfaceRaised)
            .border(1.dp, tokens.colors.borderStrong, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 50.dp)
            .padding(horizontal = tokens.spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs, Alignment.CenterHorizontally),
    ) {
        if (leadingIcon != null) Icon(leadingIcon, null, Modifier.size(17.dp), tint = tokens.colors.text)
        Text(text, style = AppTheme.type.bodyStrong, color = tokens.colors.text)
    }
}

@Composable
fun DestructiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = AppIcons.Trash,
) {
    val shape = RoundedCornerShape(AppTheme.tokens.radii.md)
    Box(
        modifier
            .size(width = 60.dp, height = 50.dp)
            .clip(shape)
            .background(AppTheme.tokens.colors.error.copy(alpha = 0.08f))
            .border(1.dp, AppTheme.tokens.colors.error.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, stringResource(Res.string.action_delete), Modifier.size(17.dp), tint = Color(0xFFF4A3AA))
    }
}

@Composable
fun AppIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    accent: Boolean = false,
) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    if (accent) {
        GlowBox(glow = tokens.colors.brandGradient.last(), shape = shape, glowRadius = 16.dp, glowAlpha = 0.5f) {
            Box(
                modifier
                    .size(38.dp)
                    .clip(shape)
                    .background(Brush.linearGradient(tokens.colors.brandGradient))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription, Modifier.size(19.dp), tint = Color.White)
            }
        }
    } else {
        Box(
            modifier
                .size(38.dp)
                .clip(shape)
                .background(tokens.colors.surface)
                .border(1.dp, tokens.colors.border, shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, Modifier.size(19.dp), tint = tokens.colors.muted)
        }
    }
}

@Composable
fun CircularButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val tokens = AppTheme.tokens
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x8C080A10))
            .border(1.dp, tokens.colors.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(19.dp), tint = tokens.colors.text)
    }
}

@Composable
fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Box(
        modifier
            .size(36.dp)
            .clip(shape)
            .background(tokens.colors.surface)
            .border(1.dp, tokens.colors.border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.Close, stringResource(Res.string.cd_close), Modifier.size(17.dp), tint = tokens.colors.muted)
    }
}
