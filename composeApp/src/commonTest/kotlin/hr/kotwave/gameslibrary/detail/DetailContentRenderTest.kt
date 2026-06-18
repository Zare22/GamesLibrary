package hr.kotwave.gameslibrary.detail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import hr.kotwave.gameslibrary.data.Game
import hr.kotwave.gameslibrary.data.GameWithOwnerships
import hr.kotwave.gameslibrary.data.Ownership
import hr.kotwave.gameslibrary.data.Platform
import hr.kotwave.gameslibrary.data.Source
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.ui.theme.GamesLibraryTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DetailContentRenderTest {

    private val noOp = DetailActions(
        onBack = {}, onRefresh = {}, onRematch = {}, onDelete = {},
        setRating = {}, setStatus = {}, addStore = {}, removeStore = {},
    )

    private fun owned(game: Game, stores: List<Store> = emptyList()) = GameWithOwnerships(
        game = game,
        ownerships = stores.map { Ownership(gameId = game.id, store = it, source = Source.MANUAL) },
    )

    @Test
    fun ownedGameShowsRatingStatusStoresAndIgdbScore() = runComposeUiTest {
        setContent {
            GamesLibraryTheme {
                DetailContent(
                    owned = owned(
                        Game(
                            id = 1, name = "Hades", igdbId = 7L, status = Status.PLAYING, userRating = 8.5,
                            totalRating = 93.0, platforms = listOf(Platform("PC", "PC")),
                        ),
                        stores = listOf(Store.STEAM),
                    ),
                    igdbUnreachable = false,
                    actions = noOp,
                )
            }
        }
        onNodeWithText("Hades").assertExists()
        onNodeWithText("Your rating").assertExists()
        onNodeWithText("8.5").assertExists()
        onNodeWithText("Completion status", ignoreCase = true).assertExists()
        onNodeWithText("Steam").assertExists()
        onNodeWithText("93").assertExists()
    }

    @Test
    fun wishlistGameHidesRatingAndStatusAndShowsNotOwned() = runComposeUiTest {
        setContent {
            GamesLibraryTheme {
                DetailContent(
                    owned = owned(Game(id = 2, name = "Silksong", igdbId = 9L, wishlist = true, totalRating = 92.0)),
                    igdbUnreachable = false,
                    actions = noOp,
                )
            }
        }
        onNodeWithText("Silksong").assertExists()
        onNodeWithText("Not owned yet · On your wishlist").assertExists()
        onNodeWithText("Your rating").assertDoesNotExist()
        onNodeWithText("Completion status").assertDoesNotExist()
    }

    @Test
    fun orphanedGameShowsRematchBanner() = runComposeUiTest {
        setContent {
            GamesLibraryTheme {
                DetailContent(
                    owned = owned(
                        Game(id = 3, name = "Lost Game", igdbId = 5L, status = Status.BACKLOG, orphaned = true),
                        stores = listOf(Store.GOG),
                    ),
                    igdbUnreachable = false,
                    actions = noOp,
                )
            }
        }
        onNodeWithText("IGDB link broke").assertExists()
        onNodeWithText("Re-match").assertExists()
    }
}
