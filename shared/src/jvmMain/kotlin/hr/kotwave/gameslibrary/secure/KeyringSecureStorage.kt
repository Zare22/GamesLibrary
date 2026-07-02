package hr.kotwave.gameslibrary.secure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Desktop [SecureStorage] backed by the OS keychain (Windows Credential Manager / macOS Keychain)
 * via [KeyringBackend]. Secrets live under one [service]; the [SecureStorage] key is the account.
 *
 * A value is split into [MAX_CHUNK_CHARS]-character chunks stored under `key#0`, `key#1`, … : Windows
 * Credential Manager caps one credential blob at 2560 bytes (~1280 UTF-16 chars) and the GOG token
 * blob exceeds that. Chunking keeps the [SecureStorage] contract intact for callers.
 *
 * When no keychain backend is available (e.g. headless Linux), delegates to [fallback], the plaintext
 * file store. On construction, deletes the pre-keychain plaintext [legacyFile] once the keychain is in
 * use, so old secrets don't linger on disk.
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
        val backend = backend ?: return@withContext fallback.get(key)
        val first = backend.get(service, chunkAccount(key, 0)) ?: return@withContext null
        buildString {
            append(first)
            var i = 1
            var next = backend.get(service, chunkAccount(key, i))
            while (next != null) {
                append(next)
                next = backend.get(service, chunkAccount(key, ++i))
            }
        }
    }

    override suspend fun put(key: String, value: String) = withContext(Dispatchers.IO) {
        val backend = backend ?: return@withContext fallback.put(key, value)
        clearChunks(backend, key)
        value.toChunks().forEachIndexed { i, chunk -> backend.set(service, chunkAccount(key, i), chunk) }
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        val backend = backend ?: return@withContext fallback.remove(key)
        clearChunks(backend, key)
    }

    private fun clearChunks(backend: KeyringBackend, key: String) {
        var i = 0
        while (backend.get(service, chunkAccount(key, i)) != null) {
            backend.delete(service, chunkAccount(key, i))
            i++
        }
    }
}

/** Under the ~1280 UTF-16-char Windows Credential Manager per-blob ceiling, with margin. */
private const val MAX_CHUNK_CHARS = 1024

private fun chunkAccount(key: String, index: Int) = "$key#$index"

private fun String.toChunks(): List<String> = if (isEmpty()) listOf("") else chunked(MAX_CHUNK_CHARS)

/** The keychain operations [KeyringSecureStorage] needs, behind a seam so tests fake the native calls. */
internal interface KeyringBackend {
    fun get(service: String, account: String): String?
    fun set(service: String, account: String, value: String)
    fun delete(service: String, account: String)
}
