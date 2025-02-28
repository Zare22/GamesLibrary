package hr.kotwave.gameslibrary.game

import android.util.Log
import hr.kotwave.gameslibrary.network.ApiResponse
import hr.kotwave.gameslibrary.platform.Platform
import hr.kotwave.gameslibrary.steam.SteamService
import kotlinx.coroutines.flow.Flow


class GameRepository(
    private val gameDao: GameDao,
    private val steamService: SteamService
) {
    val allGames: Flow<List<Game>> = gameDao.getAllGames()

    suspend fun refreshSteamGames() {
        val steamResponse = steamService.getOwnedGames()
        if (steamResponse is ApiResponse.Success) {
            val games = steamResponse.data.response.games.map { Game(name = it.name, platform = Platform.Steam) }
            gameDao.insertAll(games)
        }
    }
}