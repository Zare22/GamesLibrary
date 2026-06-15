package hr.kotwave.gameslibrary

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import hr.kotwave.gameslibrary.library.LibraryScreen
import hr.kotwave.gameslibrary.navigation.Route

@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = Route.LIBRARY,
        ) {
            composable(Route.LIBRARY) {
                LibraryScreen()
            }
        }
    }
}
