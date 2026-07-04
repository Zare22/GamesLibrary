package hr.kotwave.gameslibrary

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import hr.kotwave.gameslibrary.di.appModule
import hr.kotwave.gameslibrary.di.platformAppModule
import hr.kotwave.gameslibrary.di.sharedModules
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.app_icon
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(sharedModules + appModule + platformAppModule)
    }
    application {
        val windowState = rememberWindowState(
            placement = WindowPlacement.Maximized,
            size = DpSize(1200.dp, 800.dp),
        )
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "GamesLibrary",
            icon = painterResource(Res.drawable.app_icon),
        ) {
            App()
        }
    }
}
