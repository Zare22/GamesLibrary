package hr.kotwave.gameslibrary.data

import hr.kotwave.gameslibrary.secure.EPIC_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.GOG_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_HOST_ENDPOINT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_HOST_FINGERPRINT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_NEEDS_REPAIR_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_PAIRED_AT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_HOST_KEYSTORE_PASSWORD_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_HOST_TOKEN_HASH_KEY
import hr.kotwave.gameslibrary.secure.PSN_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.STEAM_ID_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage

/**
 * Wipes all local state: the game library ([GameRepository], Mirror Baselines included), the stored
 * Steam/GOG/PSN/Epic secrets, and the Mirror pairing keys ([SecureStorage]). Removing the keystore
 * password invalidates the host cert, so a reset forces a Mirror re-pair from either side.
 */
class LocalDataReset(
    private val repository: GameRepository,
    private val secureStorage: SecureStorage,
) {
    suspend fun reset() {
        repository.clearAllGames()
        secureStorage.remove(STEAM_ID_KEY)
        secureStorage.remove(GOG_TOKEN_KEY)
        secureStorage.remove(PSN_TOKEN_KEY)
        secureStorage.remove(EPIC_TOKEN_KEY)
        secureStorage.remove(MIRROR_CLIENT_TOKEN_KEY)
        secureStorage.remove(MIRROR_CLIENT_HOST_FINGERPRINT_KEY)
        secureStorage.remove(MIRROR_CLIENT_HOST_ENDPOINT_KEY)
        secureStorage.remove(MIRROR_CLIENT_PAIRED_AT_KEY)
        secureStorage.remove(MIRROR_CLIENT_NEEDS_REPAIR_KEY)
        secureStorage.remove(MIRROR_HOST_KEYSTORE_PASSWORD_KEY)
        secureStorage.remove(MIRROR_HOST_TOKEN_HASH_KEY)
    }
}
