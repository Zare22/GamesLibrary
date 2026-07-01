package hr.kotwave.gameslibrary.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.ui.theme.GamesLibraryTheme
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class StatusDotPlacementTest {

    /**
     * Guards the GlowBox-swallowed-modifier bug: a positioning modifier on [StatusDot] must reach the
     * placed element, so `align(TopEnd)` lands the pip on the right — not at the default top-left.
     */
    @Test
    fun statusDotHonorsTopEndAlignment() = runComposeUiTest {
        setContent {
            GamesLibraryTheme {
                Box(Modifier.size(100.dp)) {
                    StatusDot(
                        status = Status.PLAYING,
                        modifier = Modifier.align(Alignment.TopEnd).testTag("dot"),
                    )
                }
            }
        }
        val left = onNodeWithTag("dot").getBoundsInRoot().left
        assertTrue(left > 50.dp, "status pip should sit in the right half (was at $left) — align(TopEnd) was swallowed")
    }
}
