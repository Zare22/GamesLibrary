package hr.kotwave.gameslibrary.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import hr.kotwave.gameslibrary.navigation.Route
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.nav_import
import hr.kotwave.gameslibrary.resources.nav_library
import hr.kotwave.gameslibrary.resources.nav_settings
import hr.kotwave.gameslibrary.resources.nav_wishlist
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import org.jetbrains.compose.resources.stringResource

/** Top-level destinations shown in the nav chrome. */
enum class TopDestination(val icon: ImageVector, val route: Route) {
    LIBRARY(AppIcons.Grid, Route.Library),
    WISHLIST(AppIcons.Heart, Route.Wishlist),
    IMPORT(AppIcons.Import, Route.Import),
    SETTINGS(AppIcons.Settings, Route.Settings),
}

/** Display name for a [TopDestination]. */
@Composable
fun TopDestination.label(): String = when (this) {
    TopDestination.LIBRARY -> stringResource(Res.string.nav_library)
    TopDestination.WISHLIST -> stringResource(Res.string.nav_wishlist)
    TopDestination.IMPORT -> stringResource(Res.string.nav_import)
    TopDestination.SETTINGS -> stringResource(Res.string.nav_settings)
}
