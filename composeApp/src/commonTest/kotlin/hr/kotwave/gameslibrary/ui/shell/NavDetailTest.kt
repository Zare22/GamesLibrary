package hr.kotwave.gameslibrary.ui.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import hr.kotwave.gameslibrary.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NavDetailTest {

    /** Detail is a pushed route (carries a gameId, no top-nav tab) — what the immersive shell keys off. */
    @Test
    fun detail_route_is_recognised_and_is_not_a_top_destination() = runComposeUiTest {
        lateinit var nav: NavHostController
        setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = Route.Library) {
                composable<Route.Library> {}
                composable<Route.Detail> {}
            }
        }

        runOnUiThread { nav.navigate(Route.Detail(gameId = 42L)) }
        waitForIdle()

        val destination = nav.currentBackStackEntry?.destination
        assertTrue(destination?.hasRoute<Route.Detail>() == true)
        assertEquals(null, topDestinationForRoute(destination?.route))
    }
}
