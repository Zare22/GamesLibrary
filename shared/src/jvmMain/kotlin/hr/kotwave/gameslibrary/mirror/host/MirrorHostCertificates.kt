package hr.kotwave.gameslibrary.mirror.host

import hr.kotwave.gameslibrary.secure.MIRROR_HOST_KEYSTORE_PASSWORD_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.network.tls.extensions.HashAlgorithm
import io.ktor.network.tls.extensions.SignatureAlgorithm
import java.io.File
import java.security.KeyStore

internal const val MIRROR_KEY_ALIAS = "mirror"

/** The Mirror keystore file name, stored in the per-OS app data directory. */
const val MIRROR_KEYSTORE_FILE_NAME = "mirror.jks"

/** How this hosting start obtained the TLS identity. */
enum class MirrorCertProvenance {
    /** The existing keystore opened with the stored password. */
    LOADED,

    /** No keystore existed — this machine's first hosting. */
    GENERATED_FIRST,

    /** A keystore existed but could not be opened (the password is gone after a data reset) — the fingerprint changed and paired phones must re-pair. */
    REGENERATED,
}

/** The host's TLS identity: its keystore, the key password, and the cert's SHA-256 fingerprint. */
class MirrorHostIdentity internal constructor(
    internal val keyStore: KeyStore,
    internal val password: String,
    val fingerprint: String,
    val provenance: MirrorCertProvenance,
)

/**
 * Loads the host keystore, generating it on first hosting or whenever the stored password
 * ([MIRROR_HOST_KEYSTORE_PASSWORD_KEY]) is gone — [hr.kotwave.gameslibrary.data.LocalDataReset]
 * removes it, so a reset regenerates the cert, changes the fingerprint, and forces a re-pair.
 */
class MirrorHostCertificates(
    private val secureStorage: SecureStorage,
    private val keystoreFile: File,
) {

    suspend fun loadOrGenerate(): MirrorHostIdentity {
        val password = secureStorage.get(MIRROR_HOST_KEYSTORE_PASSWORD_KEY)
        if (password != null && keystoreFile.exists()) {
            load(password)?.let { return it }
        }
        val provenance =
            if (keystoreFile.exists()) MirrorCertProvenance.REGENERATED else MirrorCertProvenance.GENERATED_FIRST
        return generate(provenance)
    }

    private fun load(password: String): MirrorHostIdentity? = runCatching {
        val keyStore = KeyStore.getInstance("JKS").apply {
            keystoreFile.inputStream().use { load(it, password.toCharArray()) }
        }
        MirrorHostIdentity(keyStore, password, keyStore.certFingerprint(), MirrorCertProvenance.LOADED)
    }.getOrNull()

    private suspend fun generate(provenance: MirrorCertProvenance): MirrorHostIdentity {
        val password = randomHex(32)
        val keyStore = buildKeyStore {
            certificate(MIRROR_KEY_ALIAS) {
                hash = HashAlgorithm.SHA256
                sign = SignatureAlgorithm.RSA
                keySizeInBits = 3072
                daysValid = 3650
                this.password = password
            }
        }
        keystoreFile.parentFile?.mkdirs()
        keyStore.saveToFile(keystoreFile, password)
        secureStorage.put(MIRROR_HOST_KEYSTORE_PASSWORD_KEY, password)
        return MirrorHostIdentity(keyStore, password, keyStore.certFingerprint(), provenance)
    }

    private fun KeyStore.certFingerprint(): String = sha256Hex(getCertificate(MIRROR_KEY_ALIAS).encoded)
}
