package hr.kotwave.gameslibrary.ui.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import hr.kotwave.gameslibrary.ui.theme.GamesLibraryTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NavChromeRenderTest {

    @Test
    fun rail_shows_add_cta() = runComposeUiTest {
        setContent {
            GamesLibraryTheme {
                NavRail(current = TopDestination.LIBRARY, onSelect = {}, onAdd = {})
            }
        }
        onNodeWithText("Add game").assertExists()
    }

    @Test
    fun bottom_nav_shows_destinations_without_add_cta() = runComposeUiTest {
        setContent {
            GamesLibraryTheme {
                BottomNavBar(current = TopDestination.LIBRARY, onSelect = {})
            }
        }
        onNodeWithText("Add game").assertDoesNotExist()
        onNodeWithText("Import").assertExists()
    }

    @Test
    fun tapping_a_destination_selects_it() = runComposeUiTest {
        var selected: TopDestination? = null
        setContent {
            GamesLibraryTheme {
                BottomNavBar(current = TopDestination.LIBRARY, onSelect = { selected = it })
            }
        }
        onNodeWithText("Wishlist").performClick()
        assertEquals(TopDestination.WISHLIST, selected)
    }
}
