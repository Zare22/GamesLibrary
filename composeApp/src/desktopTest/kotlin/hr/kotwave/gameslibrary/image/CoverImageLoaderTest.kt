package hr.kotwave.gameslibrary.image

import coil3.PlatformContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CoverImageLoaderTest {
    @Test
    fun configuresBoundedDiskAndMemoryCache() {
        val loader = newImageLoader(PlatformContext.INSTANCE)
        assertNotNull(loader.memoryCache, "memory cache should be configured")
        val disk = assertNotNull(loader.diskCache, "disk cache should be configured")
        assertEquals(COVER_DISK_CACHE_MAX_BYTES, disk.maxSize)
    }

    @Test
    fun cacheDirectoryIsAppScopedAndNonRoaming() {
        val dir = coverCacheDirectory()
        assertEquals("covers", dir.name)
        assertEquals("GamesLibrary", dir.parentFile.name)
    }
}
