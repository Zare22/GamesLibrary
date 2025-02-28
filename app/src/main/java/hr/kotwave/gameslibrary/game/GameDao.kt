package hr.kotwave.gameslibrary.game

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import hr.kotwave.gameslibrary.database.GamesLibraryDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM game")
    fun getAllGames(): Flow<List<Game>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<Game>)
}