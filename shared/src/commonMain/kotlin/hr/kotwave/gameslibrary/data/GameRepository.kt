package hr.kotwave.gameslibrary.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val allGames: Flow<List<Game>> = gameDao.observeAll()

    suspend fun addGame(name: String) {
        gameDao.insert(Game(name = name))
    }
}
