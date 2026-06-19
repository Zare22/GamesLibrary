package hr.kotwave.gameslibrary

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import hr.kotwave.gameslibrary.di.appModule
import hr.kotwave.gameslibrary.di.platformAppModule
import hr.kotwave.gameslibrary.di.sharedModules
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(sharedModules + appModule + platformAppModule)
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "GamesLibrary",
        ) {
            App()
        }
    }
}
