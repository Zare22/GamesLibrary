package hr.kotwave.gameslibrary.secure

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileSecureStorageTest {

    private lateinit var dir: File
    private lateinit var storage: FileSecureStorage

    @BeforeTest
    fun setUp() {
        dir = File.createTempFile("secure_store_test", "").apply { delete(); mkdirs() }
        storage = FileSecureStorage(dir)
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun putThenGetRoundTrips() = runTest {
        assertNull(storage.get(STEAM_ID_KEY))

        storage.put(STEAM_ID_KEY, "76561198000000000")
        assertEquals("76561198000000000", storage.get(STEAM_ID_KEY))
    }

    @Test
    fun putOverwritesAndRemoveClears() = runTest {
        storage.put(STEAM_ID_KEY, "first")
        storage.put(STEAM_ID_KEY, "second")
        assertEquals("second", storage.get(STEAM_ID_KEY))

        storage.remove(STEAM_ID_KEY)
        assertNull(storage.get(STEAM_ID_KEY))
    }

    @Test
    fun valuesPersistAcrossInstances() = runTest {
        storage.put(STEAM_ID_KEY, "persisted")

        assertEquals("persisted", FileSecureStorage(dir).get(STEAM_ID_KEY))
    }
}
