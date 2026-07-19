package hr.kotwave.gameslibrary.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class TokenSemanticColorsTest {
    @Test
    fun errorAndWarningTokensCarryThePaletteValues() {
        val colors = gamesLibraryTokens().colors
        assertEquals(Color(0xFFF4707A), colors.error)
        assertEquals(Color(0xFFFFD24A), colors.warning)
    }

    @Test
    fun droppedStatusSharesTheErrorColor() {
        val tokens = gamesLibraryTokens()
        assertEquals(tokens.colors.error, tokens.status.dropped)
    }
}
