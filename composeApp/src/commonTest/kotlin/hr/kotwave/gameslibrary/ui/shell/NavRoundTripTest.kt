package hr.kotwave.gameslibrary.ui.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import hr.kotwave.gameslibrary.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NavRoundTripTest {

    @Test
    fun returning_to_library_start_destination_is_detected() = runComposeUiTest {
        lateinit var nav: NavHostController
        setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = Route.Library) {
                composable<Route.Library> {}
                composable<Route.Wishlist> {}
                composable<Route.Import> {}
                composable<Route.Settings> {}
            }
        }

        fun go(route: Route) = runOnUiThread {
            nav.navigate(route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        go(Route.Wishlist)
        waitForIdle()
        assertEquals(
            TopDestination.WISHLIST,
            topDestinationForRoute(nav.currentBackStackEntry?.destination?.route),
        )

        go(Route.Library)
        waitForIdle()
        val route = nav.currentBackStackEntry?.destination?.route
        assertEquals(TopDestination.LIBRARY, topDestinationForRoute(route), "after round-trip, route=$route")
    }
}
