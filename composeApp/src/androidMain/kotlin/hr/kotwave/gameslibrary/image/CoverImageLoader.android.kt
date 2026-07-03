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
                .directory(File(context.cacheDir, "image_cache").absolutePath.toPath())
                .maxSizeBytes(COVER_DISK_CACHE_MAX_BYTES)
                .build()
        }
        .crossfade(true)
        .build()
