package hr.kotwave.gameslibrary.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.inter_bold
import hr.kotwave.gameslibrary.resources.inter_medium
import hr.kotwave.gameslibrary.resources.inter_regular
import hr.kotwave.gameslibrary.resources.inter_semibold
import hr.kotwave.gameslibrary.resources.sora_bold
import hr.kotwave.gameslibrary.resources.sora_extrabold
import hr.kotwave.gameslibrary.resources.sora_semibold

object AppTheme {
    val tokens: AppTokens
        @Composable @ReadOnlyComposable get() = LocalAppTokens.current
    val type: AppTypography
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current
}

@Composable
private fun soraFamily(): FontFamily = FontFamily(
    Font(Res.font.sora_semibold, FontWeight.SemiBold),
    Font(Res.font.sora_bold, FontWeight.Bold),
    Font(Res.font.sora_extrabold, FontWeight.ExtraBold),
)

@Composable
private fun interFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

private fun materialTypography(type: AppTypography): Typography = Typography(
    displayLarge = type.display,
    headlineLarge = type.display,
    titleLarge = type.display.copy(fontSize = 20.sp),
    titleMedium = type.bodyStrong,
    bodyLarge = type.body,
    bodyMedium = type.body,
    bodySmall = type.caption,
    labelLarge = type.button,
    labelMedium = type.navLabel,
    labelSmall = type.caption,
)

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Bg,
    onBackground = TextPrimary,
    surface = Bg2,
    onSurface = TextPrimary,
    surfaceVariant = Bg2,
    onSurfaceVariant = TextMuted,
    outline = BorderStrong,
    error = StatusDropped,
)

/** Dual radial glows over a vertical base gradient. */
private fun Modifier.appBackground(colors: AppColors): Modifier = drawBehind {
    drawRect(Brush.verticalGradient(colors.backdrop))
    drawRect(
        Brush.radialGradient(
            colors = listOf(colors.glowTopLeft, Color.Transparent),
            center = Offset(size.width * 0.15f, -size.height * 0.08f),
            radius = size.maxDimension * 0.6f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(colors.glowTopRight, Color.Transparent),
            center = Offset(size.width * 0.92f, 0f),
            radius = size.maxDimension * 0.55f,
        ),
    )
}

/** Material 3 + the Sora/Inter type ramp + design tokens + the app backdrop. */
@Composable
fun GamesLibraryTheme(content: @Composable () -> Unit) {
    val tokens = gamesLibraryTokens()
    val type = appTypography(sora = soraFamily(), inter = interFamily())
    CompositionLocalProvider(
        LocalAppTokens provides tokens,
        LocalAppTypography provides type,
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = materialTypography(type),
        ) {
            Box(Modifier.fillMaxSize().appBackground(tokens.colors)) {
                content()
            }
        }
    }
}
