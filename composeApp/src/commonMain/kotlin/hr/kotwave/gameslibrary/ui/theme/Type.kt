package hr.kotwave.gameslibrary.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class AppTypography(
    val brand: TextStyle,
    val display: TextStyle,
    val tileTitle: TextStyle,
    val section: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val caption: TextStyle,
    val numeric: TextStyle,
    val navLabel: TextStyle,
    val button: TextStyle,
)

fun appTypography(sora: FontFamily, inter: FontFamily): AppTypography = AppTypography(
    brand = TextStyle(fontFamily = sora, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = (-0.5).sp),
    display = TextStyle(fontFamily = sora, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = (-1).sp),
    tileTitle = TextStyle(fontFamily = sora, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = (-0.2).sp),
    section = TextStyle(fontFamily = sora, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.2.sp),
    body = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyStrong = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    caption = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
    numeric = TextStyle(fontFamily = sora, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = (-0.5).sp),
    navLabel = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp),
    button = TextStyle(fontFamily = sora, fontWeight = FontWeight.Bold, fontSize = 14.sp),
)

val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("AppTypography not provided — wrap content in GamesLibraryTheme")
}
