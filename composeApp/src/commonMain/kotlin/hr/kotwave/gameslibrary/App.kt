package hr.kotwave.gameslibrary

import androidx.compose.runtime.Composable
import hr.kotwave.gameslibrary.ui.shell.AppShell
import hr.kotwave.gameslibrary.ui.theme.GamesLibraryTheme

@Composable
fun App() {
    GamesLibraryTheme {
        AppShell()
    }
}
