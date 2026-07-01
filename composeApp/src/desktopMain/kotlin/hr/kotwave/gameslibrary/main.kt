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
        ) {
            App()
        }
    }
}
