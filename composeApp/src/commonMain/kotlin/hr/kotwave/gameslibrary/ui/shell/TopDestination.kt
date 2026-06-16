package hr.kotwave.gameslibrary.ui.shell

import androidx.compose.ui.graphics.vector.ImageVector
import hr.kotwave.gameslibrary.navigation.Route
import hr.kotwave.gameslibrary.ui.icons.AppIcons

/** Top-level destinations shown in the nav chrome. */
enum class TopDestination(val label: String, val icon: ImageVector, val route: Route) {
    LIBRARY("Library", AppIcons.Grid, Route.Library),
    WISHLIST("Wishlist", AppIcons.Heart, Route.Wishlist),
    IMPORT("Import", AppIcons.Import, Route.Import),
    SETTINGS("Settings", AppIcons.Settings, Route.Settings),
}
