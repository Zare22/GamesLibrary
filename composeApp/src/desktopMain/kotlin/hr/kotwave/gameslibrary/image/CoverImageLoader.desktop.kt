package hr.kotwave.gameslibrary.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import okio.Path.Companion.toPath
import java.io.File

internal actual fun newImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory()) }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(coverCacheDirectory().absolutePath.toPath())
                .maxSizeBytes(COVER_DISK_CACHE_MAX_BYTES)
                .build()
        }
        .crossfade(true)
        .build()

/** Per-OS, non-roaming cache directory for downloaded cover art; Coil creates it on first use. */
internal fun coverCacheDirectory(): File {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")
    val base = when {
        os.contains("win") -> System.getenv("LOCALAPPDATA") ?: "$userHome\\AppData\\Local"
        os.contains("mac") -> "$userHome/Library/Caches"
        else -> System.getenv("XDG_CACHE_HOME") ?: "$userHome/.cache"
    }
    return File(base, "GamesLibrary/covers")
}
