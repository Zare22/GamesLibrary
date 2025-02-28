package hr.kotwave.gameslibrary.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Companion.Default,
        fontWeight = FontWeight.Companion.Bold,
        fontSize = 30.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Companion.Default,
        fontWeight = FontWeight.Companion.Bold,
        fontSize = 24.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Companion.Default,
        fontWeight = FontWeight.Companion.Bold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Companion.Default,
        fontWeight = FontWeight.Companion.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Companion.Default,
        fontWeight = FontWeight.Companion.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Companion.Default,
        fontWeight = FontWeight.Companion.Bold,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Companion.Default,
        fontWeight = FontWeight.Companion.Normal,
        fontSize = 12.sp
    )
)