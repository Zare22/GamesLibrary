package hr.kotwave.gameslibrary

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import hr.kotwave.gameslibrary.ui.shell.AppShell
import hr.kotwave.gameslibrary.ui.theme.GamesLibraryTheme

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }
    GamesLibraryTheme {
        AppShell()
    }
}
