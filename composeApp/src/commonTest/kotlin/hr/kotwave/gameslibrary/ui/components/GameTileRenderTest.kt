package hr.kotwave.gameslibrary.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.ui.theme.GamesLibraryTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class GameTileRenderTest {

    @Test
    fun rendersTitleWithCoverImageId() = runComposeUiTest {
        setContent {
            GamesLibraryTheme {
                GameTile(
                    title = "Elden Ring",
                    stores = listOf(Store.STEAM),
                    status = Status.PLAYING,
                    coverImageId = "co4jni",
                )
            }
        }
        onNodeWithText("Elden Ring").assertExists()
    }

    @Test
    fun rendersTitleWithoutCover() = runComposeUiTest {
        setContent {
            GamesLibraryTheme {
                GameTile(title = "Manual Game", stores = emptyList(), status = null, coverImageId = null)
            }
        }
        onNodeWithText("Manual Game").assertExists()
    }
}
