package hr.kotwave.gameslibrary.mirror.host

import java.security.MessageDigest
import java.security.SecureRandom

internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

internal fun sha256Hex(text: String): String = sha256Hex(text.encodeToByteArray())

internal fun randomHex(byteCount: Int): String =
    ByteArray(byteCount).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }

/** A 6-digit pairing secret — short enough for the typed no-QR fallback. */
internal fun randomPairingSecret(): String = (SecureRandom().nextInt(900_000) + 100_000).toString()

internal fun constantTimeEquals(a: String, b: String): Boolean =
    MessageDigest.isEqual(a.encodeToByteArray(), b.encodeToByteArray())
