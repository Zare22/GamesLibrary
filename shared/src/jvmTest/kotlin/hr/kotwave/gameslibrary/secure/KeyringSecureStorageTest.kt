package hr.kotwave.gameslibrary.secure

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyringSecureStorageTest {

    private lateinit var dir: File
    private lateinit var legacyFile: File
    private lateinit var fallback: FileSecureStorage

    @BeforeTest
    fun setUp() {
        dir = File.createTempFile("keyring_test", "").apply { delete(); mkdirs() }
        legacyFile = File(dir, FileSecureStorage.FILE_NAME)
        fallback = FileSecureStorage(dir)
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun storage(backend: KeyringBackend?) =
        KeyringSecureStorage(service = "GamesLibrary", backend = backend, fallback = fallback, legacyFile = legacyFile)

    @Test
    fun keychainPathRoundTrips() = runTest {
        val storage = storage(FakeKeyringBackend())
        assertNull(storage.get(GOG_TOKEN_KEY))

        storage.put(GOG_TOKEN_KEY, "secret-blob")
        assertEquals("secret-blob", storage.get(GOG_TOKEN_KEY))
    }

    @Test
    fun keychainPathOverwritesAndRemoves() = runTest {
        val storage = storage(FakeKeyringBackend())
        storage.put(STEAM_ID_KEY, "first")
        storage.put(STEAM_ID_KEY, "second")
        assertEquals("second", storage.get(STEAM_ID_KEY))

        storage.remove(STEAM_ID_KEY)
        assertNull(storage.get(STEAM_ID_KEY))
    }

    @Test
    fun largeValueChunksAndRoundTrips() = runTest {
        val backend = FakeKeyringBackend()
        val storage = storage(backend)
        val big = "y".repeat(3000)

        storage.put(GOG_TOKEN_KEY, big)

        assertEquals(big, storage.get(GOG_TOKEN_KEY))
        assertTrue(backend.accounts.count { it.startsWith("$GOG_TOKEN_KEY#") } > 1, "expected multiple chunk entries")
    }

    @Test
    fun overwritingLongValueWithShortClearsStaleChunks() = runTest {
        val backend = FakeKeyringBackend()
        val storage = storage(backend)
        storage.put(GOG_TOKEN_KEY, "z".repeat(3000))

        storage.put(GOG_TOKEN_KEY, "small")

        assertEquals("small", storage.get(GOG_TOKEN_KEY))
        assertEquals(1, backend.accounts.count { it.startsWith("$GOG_TOKEN_KEY#") }, "stale chunks left behind")
    }

    @Test
    fun delegatesToFileWhenNoBackend() = runTest {
        val storage = storage(null)
        storage.put(STEAM_ID_KEY, "via-file")

        assertEquals("via-file", storage.get(STEAM_ID_KEY))
        assertEquals("via-file", fallback.get(STEAM_ID_KEY))
    }

    @Test
    fun dropsLegacyPlaintextWhenKeychainAvailable() {
        legacyFile.writeText("steam.id64=old\n")

        storage(FakeKeyringBackend())

        assertFalse(legacyFile.exists())
    }

    @Test
    fun keepsLegacyFileWhenNoBackend() {
        legacyFile.writeText("steam.id64=old\n")

        storage(null)

        assertTrue(legacyFile.exists())
    }
}

private class FakeKeyringBackend : KeyringBackend {
    private val store = mutableMapOf<Pair<String, String>, String>()
    val accounts: Set<String> get() = store.keys.map { it.second }.toSet()
    override fun get(service: String, account: String): String? = store[service to account]
    override fun set(service: String, account: String, value: String) {
        store[service to account] = value
    }
    override fun delete(service: String, account: String) {
        store.remove(service to account)
    }
}
