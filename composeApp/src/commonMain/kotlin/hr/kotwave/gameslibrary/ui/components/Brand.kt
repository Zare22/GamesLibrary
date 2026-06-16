package hr.kotwave.gameslibrary.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import hr.kotwave.gameslibrary.ui.theme.AppTheme

/** The Games.Library wordmark — gradient fill + accent dot. */
@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.type.brand,
) {
    val tokens = AppTheme.tokens
    val fill = Brush.verticalGradient(tokens.colors.wordmark)
    Row(modifier) {
        Text("Games", style = style.copy(brush = fill))
        Text(".", style = style.copy(color = tokens.colors.accent))
        Text("Library", style = style.copy(brush = fill))
    }
}
