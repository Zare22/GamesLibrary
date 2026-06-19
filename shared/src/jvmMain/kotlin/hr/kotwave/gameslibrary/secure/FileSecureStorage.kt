package hr.kotwave.gameslibrary.secure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

/**
 * Desktop [SecureStorage]: a plain properties file under the app-data directory. The JVM has no
 * standard secure store, so this is the known weak point (ADR 0003) — fine for the public SteamID,
 * but a real secret design (OS keychain / encrypted store) is required before GOG/PSN secrets land.
 */
internal class FileSecureStorage(directory: File) : SecureStorage {

    private val file = File(directory, "secure_store.properties")

    private fun load(): Properties = Properties().apply {
        if (file.exists()) file.inputStream().use { load(it) }
    }

    private fun save(props: Properties) {
        file.outputStream().use { props.store(it, null) }
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) { load().getProperty(key) }

    override suspend fun put(key: String, value: String) = withContext(Dispatchers.IO) {
        save(load().apply { setProperty(key, value) })
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        save(load().apply { remove(key) })
    }
}
