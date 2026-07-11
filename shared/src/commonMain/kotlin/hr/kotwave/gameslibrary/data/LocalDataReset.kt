package hr.kotwave.gameslibrary.data

import hr.kotwave.gameslibrary.secure.GOG_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.PSN_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.STEAM_ID_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage

/**
 * Wipes all local state: the game library ([GameRepository]) and the stored Steam/GOG/PSN secrets
 * ([SecureStorage]). One seam so both platforms share a single, testable implementation.
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
    }
}
