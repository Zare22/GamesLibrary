package hr.kotwave.gameslibrary

import androidx.compose.runtime.Composable
import coil3.compose.setSingletonImageLoaderFactory
import hr.kotwave.gameslibrary.image.newImageLoader
import hr.kotwave.gameslibrary.ui.shell.AppShell
import hr.kotwave.gameslibrary.ui.theme.GamesLibraryTheme

@Composable
fun App() {
    setSingletonImageLoaderFactory { context -> newImageLoader(context) }
    GamesLibraryTheme {
        AppShell()
    }
}
