package hr.kotwave.gameslibrary.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import hr.kotwave.gameslibrary.library.LibraryScreen
import hr.kotwave.gameslibrary.navigation.Route
import hr.kotwave.gameslibrary.ui.gallery.ComponentGalleryScreen
import hr.kotwave.gameslibrary.ui.screens.PlaceholderScreen
import hr.kotwave.gameslibrary.ui.screens.SettingsScreen

/** True on phone-width layouts (bottom nav); false on wide layouts (left rail). */
val LocalIsCompact = staticCompositionLocalOf { true }

enum class NavChrome { BottomNav, Rail }

/** Width at/above which the shell switches from bottom nav to the left rail. */
internal val ExpandedWidthThreshold = 720.dp

internal fun navChromeFor(width: Dp): NavChrome =
    if (width < ExpandedWidthThreshold) NavChrome.BottomNav else NavChrome.Rail

/** Hosts the [NavHost] with form-factor nav chrome: bottom nav (compact) or left rail (expanded). */
@Composable
fun AppShell() {
    val navController = rememberNavController()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val chrome = navChromeFor(maxWidth)
        CompositionLocalProvider(LocalIsCompact provides (chrome == NavChrome.BottomNav)) {
            when (chrome) {
                NavChrome.BottomNav -> CompactShell(navController)
                NavChrome.Rail -> ExpandedShell(navController)
            }
        }
    }
}

@Composable
private fun CompactShell(navController: NavHostController) {
    val current = navController.currentTopDestination()
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Box(Modifier.fillMaxSize().weight(1f)) {
            AppNavHost(navController, Modifier.fillMaxSize())
        }
        BottomNavBar(current = current, onSelect = navController::navigateTop)
    }
}

@Composable
private fun ExpandedShell(navController: NavHostController) {
    val current = navController.currentTopDestination()
    Row(Modifier.fillMaxSize().safeDrawingPadding()) {
        NavRail(
            current = current,
            onSelect = navController::navigateTop,
            onAdd = { navController.navigate(Route.Add) },
        )
        Box(Modifier.fillMaxSize().weight(1f)) {
            AppNavHost(navController, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination = Route.Library, modifier = modifier) {
        composable<Route.Library> {
            LibraryScreen(onAdd = { navController.navigate(Route.Add) })
        }
        composable<Route.Wishlist> {
            PlaceholderScreen("Wishlist", "Games you want but don't own yet.")
        }
        composable<Route.Import> {
            PlaceholderScreen("Import", "Paste a store library to bulk-add.")
        }
        composable<Route.Settings> {
            SettingsScreen(onOpenGallery = { navController.navigate(Route.Gallery) })
        }
        composable<Route.Add> {
            PlaceholderScreen("Add a game", "Add a game by hand.")
        }
        composable<Route.Gallery> {
            ComponentGalleryScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** The top-level destination currently shown, or null when on a pushed (Add/Gallery) screen. */
@Composable
private fun NavController.currentTopDestination(): TopDestination? {
    val entry by currentBackStackEntryAsState()
    return topDestinationForRoute(entry?.destination?.route)
}

internal fun topDestinationForRoute(route: String?): TopDestination? = when (route) {
    Route.Library.serializer().descriptor.serialName -> TopDestination.LIBRARY
    Route.Wishlist.serializer().descriptor.serialName -> TopDestination.WISHLIST
    Route.Import.serializer().descriptor.serialName -> TopDestination.IMPORT
    Route.Settings.serializer().descriptor.serialName -> TopDestination.SETTINGS
    else -> null
}

/** Navigate to a top-level destination with single-top + saved/restored state, as for tabs. */
private fun NavController.navigateTop(destination: TopDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
