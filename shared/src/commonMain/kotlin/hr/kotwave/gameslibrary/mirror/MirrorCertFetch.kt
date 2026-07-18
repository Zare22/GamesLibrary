package hr.kotwave.gameslibrary.mirror

/**
 * Fetches the TLS certificate serving at [ip]:[port] WITHOUT pinning and returns its normalized
 * SHA-256 fingerprint. The typed pairing path shows this fingerprint's verify code for the user to
 * confirm against the hosting screen before anything pins or pairs. Throws when the host is
 * unreachable or the handshake fails.
 */
expect suspend fun fetchMirrorCertFingerprint(ip: String, port: Int, timeoutMillis: Long = 10_000): String
