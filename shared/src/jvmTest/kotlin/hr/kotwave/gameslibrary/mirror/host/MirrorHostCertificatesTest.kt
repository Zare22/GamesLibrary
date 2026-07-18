package hr.kotwave.gameslibrary.mirror.host

import hr.kotwave.gameslibrary.secure.MIRROR_HOST_KEYSTORE_PASSWORD_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MirrorHostCertificatesTest {

    private lateinit var dir: File
    private lateinit var storage: MapSecureStorage
    private lateinit var certificates: MirrorHostCertificates

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("mirror_certs_test").toFile()
        storage = MapSecureStorage()
        certificates = MirrorHostCertificates(storage, File(dir, MIRROR_KEYSTORE_FILE_NAME))
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun generatePersistsKeystoreAndReloadKeepsTheFingerprint() = runTest {
        val first = certificates.loadOrGenerate()

        assertTrue(File(dir, MIRROR_KEYSTORE_FILE_NAME).exists())
        assertNotNull(storage.values[MIRROR_HOST_KEYSTORE_PASSWORD_KEY])
        assertTrue(Regex("[0-9a-f]{64}").matches(first.fingerprint))

        val second = certificates.loadOrGenerate()
        assertEquals(first.fingerprint, second.fingerprint)
    }

    @Test
    fun removedPasswordRegeneratesWithANewFingerprint() = runTest {
        val first = certificates.loadOrGenerate()
        storage.remove(MIRROR_HOST_KEYSTORE_PASSWORD_KEY)

        val regenerated = certificates.loadOrGenerate()

        assertNotEquals(first.fingerprint, regenerated.fingerprint)
        assertNotNull(storage.values[MIRROR_HOST_KEYSTORE_PASSWORD_KEY])
    }
}

internal class MapSecureStorage : SecureStorage {
    val values = mutableMapOf<String, String>()
    override suspend fun get(key: String): String? = values[key]
    override suspend fun put(key: String, value: String) {
        values[key] = value
    }

    override suspend fun remove(key: String) {
        values.remove(key)
    }
}
