package hr.kotwave.gameslibrary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple200,
    onPrimary = Color.Black,
    primaryContainer = Purple700,
    onPrimaryContainer = Color.White,
    inversePrimary = Purple500,
    secondary = Teal200,
    onSecondary = Color.Black,
    secondaryContainer = Teal700,
    onSecondaryContainer = Color.White,
    tertiary = RedError,
    onTertiary = Color.White,
    tertiaryContainer = Purple700,
    onTertiaryContainer = Color.White,
    background = DarkGrayBackground,
    onBackground = LightGrayText,
    surface = DarkGraySurface,
    onSurface = LightGrayText,
    surfaceVariant = SurfaceVariantGray,
    onSurfaceVariant = OutlineGray,
    surfaceTint = Purple200,
    inverseSurface = LightGrayText,
    inverseOnSurface = DarkGrayBackground,
    error = RedErrorContainer,
    onError = Color.Black,
    errorContainer = RedError,
    onErrorContainer = Color.White,
    outline = OutlineGray,
    outlineVariant = SurfaceVariantGray,
    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Purple500,
    onPrimary = LightSurface,
    primaryContainer = Purple200,
    onPrimaryContainer = DarkGrayText,
    inversePrimary = Purple700,
    secondary = Teal200,
    onSecondary = DarkGrayText,
    secondaryContainer = Teal700,
    onSecondaryContainer = LightSurface,
    tertiary = RedError,
    onTertiary = LightSurface,
    tertiaryContainer = RedErrorContainer,
    onTertiaryContainer = DarkGrayText,
    background = LightBackground,
    onBackground = DarkGrayText,
    surface = LightSurface,
    onSurface = DarkGrayText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = SurfaceVariantText,
    surfaceTint = Purple500,
    inverseSurface = DarkGrayText,
    inverseOnSurface = LightBackground,
    error = RedError,
    onError = LightSurface,
    errorContainer = RedErrorContainer,
    onErrorContainer = DarkGrayText,
    outline = OutlineGray,
    outlineVariant = LightSurfaceVariant,
    scrim = Color.Black
)

@Composable
fun GamesLibraryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}