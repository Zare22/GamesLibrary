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

    /** The Game holding this `igdbId`, if any — the dedup key for a matched add. */
    @Query("SELECT * FROM game WHERE igdbId = :igdbId")
    suspend fun getGameByIgdbId(igdbId: Long): Game?

    /** Case-insensitive title equality, for the soft "similar title exists" warning. */
    @Query("SELECT * FROM game WHERE name = :name COLLATE NOCASE")
    suspend fun gamesByTitle(name: String): List<Game>

    @Query("SELECT * FROM ownership WHERE gameId = :gameId")
    suspend fun ownershipsFor(gameId: Long): List<Ownership>

    @Query("SELECT * FROM external_game WHERE gameId = :gameId")
    suspend fun externalGamesFor(gameId: Long): List<ExternalGame>

    @Insert
    suspend fun insertGame(game: Game): Long

    @Update
    suspend fun updateGame(game: Game)

    /** Ignores a duplicate (Game, Store): Ownership is unique per pair. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOwnership(ownership: Ownership): Long

    @Insert
    suspend fun insertExternalGames(externals: List<ExternalGame>)

    /** Inserts a matched Game with its Ownerships and external references atomically. */
    @Transaction
    suspend fun insertMatchedGame(game: Game, stores: Set<Store>, externals: List<ExternalGame>): Long {
        val gameId = insertGame(game)
        stores.forEach { store ->
            insertOwnership(Ownership(gameId = gameId, store = store, source = Source.MANUAL))
        }
        if (externals.isNotEmpty()) {
            insertExternalGames(externals.map { it.copy(gameId = gameId) })
        }
        return gameId
    }
}
