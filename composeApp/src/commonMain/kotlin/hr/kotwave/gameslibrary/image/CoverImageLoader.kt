package hr.kotwave.gameslibrary.image

import coil3.ImageLoader
import coil3.PlatformContext

/** Bytes the on-disk cover cache retains before Coil evicts least-recently-used entries. */
internal const val COVER_DISK_CACHE_MAX_BYTES = 128L * 1024 * 1024

/**
 * The singleton cover [ImageLoader]: a Ktor network fetcher, a memory cache, a bounded disk cache, and
 * crossfade. The disk-cache directory is the only platform-specific part — Android uses `cacheDir`, Desktop
 * an OS cache directory.
 */
internal expect fun newImageLoader(context: PlatformContext): ImageLoader
