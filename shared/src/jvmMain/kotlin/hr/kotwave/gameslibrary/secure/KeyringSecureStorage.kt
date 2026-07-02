package hr.kotwave.gameslibrary.secure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Desktop [SecureStorage] backed by the OS keychain (Windows Credential Manager / macOS Keychain)
 * via [KeyringBackend]. Secrets live under one [service]; the [SecureStorage] key is the account.
 * When no keychain backend is available (e.g. headless Linux), delegates to [fallback], the
 * plaintext file store. On construction, deletes the pre-keychain plaintext [legacyFile] once the
 * keychain is in use, so old secrets don't linger on disk.
 */
internal class KeyringSecureStorage(
    private val service: String,
    private val backend: KeyringBackend?,
    private val fallback: SecureStorage,
    legacyFile: File,
) : SecureStorage {

    init {
        if (backend != null && legacyFile.exists()) legacyFile.delete()
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        val backend = backend
        if (backend != null) backend.get(service, key) else fallback.get(key)
    }

    override suspend fun put(key: String, value: String) = withContext(Dispatchers.IO) {
        val backend = backend
        if (backend != null) backend.set(service, key, value) else fallback.put(key, value)
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        val backend = backend
        if (backend != null) backend.delete(service, key) else fallback.remove(key)
    }
}

/** The keychain operations [KeyringSecureStorage] needs, behind a seam so tests fake the native calls. */
internal interface KeyringBackend {
    fun get(service: String, account: String): String?
    fun set(service: String, account: String, value: String)
    fun delete(service: String, account: String)
}
