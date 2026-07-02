package hr.kotwave.gameslibrary.secure

import com.github.javakeyring.Keyring

/** [KeyringBackend] backed by java-keyring's [Keyring] (Windows Credential Manager / macOS Keychain). */
internal class JavaKeyringBackend(private val keyring: Keyring) : KeyringBackend {

    override fun get(service: String, account: String): String? =
        runCatching { keyring.getPassword(service, account) }.getOrNull()

    override fun set(service: String, account: String, value: String) {
        keyring.setPassword(service, account, value)
    }

    override fun delete(service: String, account: String) {
        // Best-effort: an absent entry is already gone.
        runCatching { keyring.deletePassword(service, account) }
    }
}

/**
 * A keychain-backed [KeyringBackend], or null when the OS has no supported backend — including a
 * Linux box with the D-Bus backend excluded — so the caller degrades to the file store.
 */
internal fun createKeyringBackend(): KeyringBackend? =
    runCatching { JavaKeyringBackend(Keyring.create()) }.getOrNull()
