package hr.kotwave.gameslibrary.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import hr.kotwave.gameslibrary.add.AddGameModal
import hr.kotwave.gameslibrary.add.AddGameScreen
import hr.kotwave.gameslibrary.battlenet.BattleNetScreen
import hr.kotwave.gameslibrary.detail.DetailScreen
import hr.kotwave.gameslibrary.importer.ImportScreen
import hr.kotwave.gameslibrary.importer.SharedTextInbox
import hr.kotwave.gameslibrary.gog.GogScreen
import hr.kotwave.gameslibrary.library.LibraryScreen
import hr.kotwave.gameslibrary.navigation.Route
import hr.kotwave.gameslibrary.epic.EpicScreen
import hr.kotwave.gameslibrary.psn.PsnScreen
import hr.kotwave.gameslibrary.steam.SteamScreen
import hr.kotwave.gameslibrary.transfer.LibraryImportScreen
import hr.kotwave.gameslibrary.ui.components.ContentColumn
import hr.kotwave.gameslibrary.ui.gallery.ComponentGalleryScreen
import hr.kotwave.gameslibrary.ui.screens.SettingsScreen
import hr.kotwave.gameslibrary.wishlist.WishlistScreen
import org.koin.compose.koinInject

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
    // A share-sheet intake (Android) lands text in the inbox; jump to the Import tab to consume it.
    val sharedText by koinInject<SharedTextInbox>().pending.collectAsState()
    LaunchedEffect(sharedText) {
        if (sharedText != null) navController.navigateTop(TopDestination.IMPORT)
    }
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
    val entry by navController.currentBackStackEntryAsState()
    // Detail is immersive: it draws its hero under the status bar and hides the bottom nav.
    val immersive = entry?.destination?.hasRoute<Route.Detail>() == true
    Column(Modifier.fillMaxSize().then(if (immersive) Modifier else Modifier.safeDrawingPadding())) {
        Box(Modifier.fillMaxSize().weight(1f)) {
            AppNavHost(navController, Modifier.fillMaxSize())
        }
        if (!immersive) {
            BottomNavBar(current = topDestinationForRoute(entry?.destination?.route), onSelect = navController::navigateTop)
        }
    }
}

@Composable
private fun ExpandedShell(navController: NavHostController) {
    val current = navController.currentTopDestination()
    var showAdd by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize().safeDrawingPadding()) {
            NavRail(
                current = current,
                onSelect = navController::navigateTop,
                onAdd = { showAdd = true },
            )
            Box(Modifier.fillMaxSize().weight(1f)) {
                AppNavHost(navController, Modifier.fillMaxSize())
            }
        }
        if (showAdd) {
            AddGameModal(onDismiss = { showAdd = false })
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination = Route.Library, modifier = modifier) {
        composable<Route.Library> {
            LibraryScreen(
                onAdd = { navController.navigate(Route.Add) },
                onOpenGame = { navController.navigate(Route.Detail(it)) },
            )
        }
        composable<Route.Wishlist> {
            WishlistScreen(onOpenGame = { navController.navigate(Route.Detail(it)) })
        }
        composable<Route.Import> {
            ContentColumn { ImportScreen() }
        }
        composable<Route.Settings> {
            ContentColumn {
                SettingsScreen(
                    onOpenGallery = { navController.navigate(Route.Gallery) },
                    onOpenGame = { navController.navigate(Route.Detail(it)) },
                    onOpenSteam = { navController.navigate(Route.Steam) },
                    onOpenGog = { navController.navigate(Route.Gog) },
                    onOpenPsn = { navController.navigate(Route.Psn) },
                    onOpenEpic = { navController.navigate(Route.Epic) },
                    onOpenBattleNet = { navController.navigate(Route.BattleNet) },
                    onOpenImport = { navController.navigate(Route.LibraryImport) },
                    onOpenPasteImport = { navController.navigate(Route.Import) },
                )
            }
        }
        composable<Route.Add> {
            AddGameScreen(onClose = { navController.popBackStack() })
        }
        composable<Route.Gallery> {
            ComponentGalleryScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Steam> {
            ContentColumn { SteamScreen(onBack = { navController.popBackStack() }) }
        }
        composable<Route.Gog> {
            ContentColumn { GogScreen(onBack = { navController.popBackStack() }) }
        }
        composable<Route.Psn> {
            ContentColumn { PsnScreen(onBack = { navController.popBackStack() }) }
        }
        composable<Route.Epic> {
            ContentColumn { EpicScreen(onBack = { navController.popBackStack() }) }
        }
        composable<Route.BattleNet> {
            ContentColumn { BattleNetScreen(onBack = { navController.popBackStack() }) }
        }
        composable<Route.LibraryImport> {
            LibraryImportScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Detail> {
            DetailScreen(onBack = { navController.popBackStack() })
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
