package hr.kotwave.gameslibrary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    /** Owned Games (not Wishlisted) with their Ownerships, for Library tiles. */
    @Transaction
    @Query("SELECT * FROM game WHERE wishlist = 0 ORDER BY name COLLATE NOCASE")
    fun observeOwnedGames(): Flow<List<GameWithOwnerships>>

    @Query("SELECT * FROM game WHERE id = :id")
    suspend fun getGame(id: Long): Game?

    /** Case-insensitive title equality, for the soft "similar title exists" warning. */
    @Query("SELECT * FROM game WHERE name = :name COLLATE NOCASE")
    suspend fun gamesByTitle(name: String): List<Game>

    @Query("SELECT * FROM ownership WHERE gameId = :gameId")
    suspend fun ownershipsFor(gameId: Long): List<Ownership>

    @Insert
    suspend fun insertGame(game: Game): Long

    @Update
    suspend fun updateGame(game: Game)

    /** Ignores a duplicate (Game, Store): Ownership is unique per pair. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOwnership(ownership: Ownership): Long
}
