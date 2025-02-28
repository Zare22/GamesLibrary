package hr.kotwave.gameslibrary

import android.os.Bundle
import android.provider.CalendarContract.Colors
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import hr.kotwave.gameslibrary.game.Game
import hr.kotwave.gameslibrary.game.GameListViewModel
import hr.kotwave.gameslibrary.game.SelectedGameViewModel
import hr.kotwave.gameslibrary.game.ui.GameList
import hr.kotwave.gameslibrary.navigation.Route
import hr.kotwave.gameslibrary.platform.Platform
import hr.kotwave.gameslibrary.ui.theme.GamesLibraryTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GamesLibraryTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Route.GamesGraph
                ) {
                    navigation<Route.GamesGraph>(
                        startDestination = Route.GamesList
                    ) {
                        composable<Route.GamesList> {
                            val gameListViewModel = koinViewModel<GameListViewModel>()
                            val selectedGameViewModel = it.sharedKoinViewModel<SelectedGameViewModel>(navController)
                            GameScreenWrapper(gameListViewModel) {
                                navController.navigate(Route.GameDetails)
                            }
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun GameScreenWrapper(gameListViewModel: GameListViewModel, onAddGameClick: () -> Unit) {
    val searchQuery by gameListViewModel.searchQuery.collectAsState()
    val games by gameListViewModel.filteredGames.collectAsState(initial = emptyList())

    GameList(
        games = games,
        searchQuery = searchQuery,
        onSearchQueryChange = { gameListViewModel.setSearchQuery(it) },
        onAddGameClick = { onAddGameClick },
        onGameClick = { },
        onDelete = {  },
        onCompleted = {  }
    )
}

@Composable
private inline fun <reified T: ViewModel> NavBackStackEntry.sharedKoinViewModel(
    navController: NavController
): T {
    val navGraphRoute = destination.parent?.route ?: return koinViewModel<T>()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return koinViewModel(
        viewModelStoreOwner = parentEntry
    )
}


@Preview(showBackground = true)
@Composable
fun PreviewGameScreen() {
    GamesLibraryTheme {
        GameList(
            games = listOf(
                Game(1, "Game 1", Platform.Steam),
                Game(2, "Game 2", Platform.Epic)
            ),
            searchQuery = "",
            onSearchQueryChange = {},
            onAddGameClick = {},
            onGameClick = {},
            onDelete = {},
            onCompleted = {}
        )
    }
}