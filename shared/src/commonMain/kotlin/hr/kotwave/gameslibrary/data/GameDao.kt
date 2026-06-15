package hr.kotwave.gameslibrary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM game ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Game>>

    @Insert
    suspend fun insert(game: Game): Long
}
