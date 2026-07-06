package hr.kotwave.gameslibrary.steam

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import hr.kotwave.gameslibrary.ui.theme.GamesLibraryTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SteamSyncStageMessageTest {

    @Test
    fun eachStageRendersItsOwnMessage() = runComposeUiTest {
        setContent {
            GamesLibraryTheme {
                Column {
                    SteamSyncStage.entries.forEach { Text(it.message()) }
                }
            }
        }
        onNodeWithText("Couldn't reach Steam", substring = true).assertExists()
        onNodeWithText("game database (IGDB)", substring = true).assertExists()
        onNodeWithText("Couldn't save the synced games", substring = true).assertExists()
    }
}
