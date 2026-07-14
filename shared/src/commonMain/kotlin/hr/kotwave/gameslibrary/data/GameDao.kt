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

    /** A single Game with its Ownerships, for the detail screen; emits null once it is deleted. */
    @Transaction
    @Query("SELECT * FROM game WHERE id = :id")
    fun observeGame(id: Long): Flow<GameWithOwnerships?>

    /** Orphaned Games (igdb_id no longer resolves), for the bulk re-match entry. */
    @Query("SELECT * FROM game WHERE orphaned = 1 ORDER BY name COLLATE NOCASE")
    fun observeOrphanedGames(): Flow<List<Game>>

    /** Wishlisted Games (no Ownerships by invariant), for the Wishlist view. */
    @Query("SELECT * FROM game WHERE wishlist = 1 ORDER BY name COLLATE NOCASE")
    fun observeWishlistGames(): Flow<List<Game>>

    /** Every Game (owned + wishlisted) with its Ownerships, for a full-library export. */
    @Transaction
    @Query("SELECT * FROM game ORDER BY name COLLATE NOCASE")
    suspend fun allGamesWithOwnerships(): List<GameWithOwnerships>

    /** Every external reference, grouped by Game on export. */
    @Query("SELECT * FROM external_game")
    suspend fun allExternalGames(): List<ExternalGame>

    @Query("SELECT * FROM game WHERE id = :id")
    suspend fun getGame(id: Long): Game?

    /** The Game holding this `igdbId`, if any — the dedup key for a matched add. */
    @Query("SELECT * FROM game WHERE igdbId = :igdbId")
    suspend fun getGameByIgdbId(igdbId: Long): Game?

    /** The Game holding this external reference, if any — the dedup key for an unmatched Steam sync. */
    @Query(
        "SELECT game.* FROM game JOIN external_game ON external_game.gameId = game.id " +
            "WHERE external_game.category = :category AND external_game.uid = :uid LIMIT 1",
    )
    suspend fun getGameByExternalUid(category: Int, uid: String): Game?

    /** Of these external uids, the ones already attached to an IGDB-matched Game. */
    @Query(
        "SELECT external_game.uid FROM external_game JOIN game ON external_game.gameId = game.id " +
            "WHERE external_game.category = :category AND external_game.uid IN (:uids) AND game.igdbId IS NOT NULL",
    )
    suspend fun externalUidsWithIgdbMatch(category: Int, uids: List<String>): List<String>

    /** Of these external uids, the ones attached to any Game (matched or bare) — the needs-review filter. */
    @Query("SELECT uid FROM external_game WHERE category = :category AND uid IN (:uids)")
    suspend fun knownExternalUids(category: Int, uids: List<String>): List<String>

    /** One Game's external uids for a category, for the additive uid backfill on a sync Review pick. */
    @Query("SELECT uid FROM external_game WHERE gameId = :gameId AND category = :category")
    suspend fun externalUidsFor(gameId: Long, category: Int): List<String>

    /** Of these external uids, the ones the user dismissed from the sync Review. */
    @Query("SELECT uid FROM sync_dismissal WHERE category = :category AND uid IN (:uids)")
    suspend fun dismissedSyncUids(category: Int, uids: List<String>): List<String>

    /** Ignores an already-dismissed (category, uid): dismissal is idempotent. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSyncDismissals(dismissals: List<SyncDismissal>)

    /** Case-insensitive title equality, for the soft "similar title exists" warning. */
    @Query("SELECT * FROM game WHERE name = :name COLLATE NOCASE")
    suspend fun gamesByTitle(name: String): List<Game>

    /** Per-game reads the repository test-suite uses to assert what a write produced. */
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

    @Query("DELETE FROM ownership WHERE gameId = :gameId AND store = :store")
    suspend fun deleteOwnership(gameId: Long, store: Store)

    /** Re-tags an existing Ownership's Source (Steam sync claims a Store it now vouches for). */
    @Query("UPDATE ownership SET source = :source WHERE gameId = :gameId AND store = :store")
    suspend fun setOwnershipSource(gameId: Long, store: Store, source: Source)

    @Insert
    suspend fun insertExternalGames(externals: List<ExternalGame>)

    @Query("DELETE FROM external_game WHERE gameId = :gameId")
    suspend fun deleteExternalGamesFor(gameId: Long)

    /** Removes a Game; its Ownerships and external references cascade away (FK ON DELETE CASCADE). */
    @Query("DELETE FROM game WHERE id = :id")
    suspend fun deleteGame(id: Long)

    @Query("DELETE FROM external_game")
    suspend fun deleteAllExternalGames()

    @Query("DELETE FROM ownership")
    suspend fun deleteAllOwnerships()

    @Query("DELETE FROM game")
    suspend fun deleteAllGames()

    @Query("DELETE FROM sync_dismissal")
    suspend fun deleteAllSyncDismissals()

    /** Empties every table (child → parent) in one transaction — the local-data wipe. */
    @Transaction
    suspend fun clearAll() {
        deleteAllExternalGames()
        deleteAllOwnerships()
        deleteAllGames()
        deleteAllSyncDismissals()
    }

    /** Overwrites a Game's row and replaces its external references in one transaction (refresh/re-match). */
    @Transaction
    suspend fun replaceMetadata(game: Game, externals: List<ExternalGame>) {
        updateGame(game)
        deleteExternalGamesFor(game.id)
        if (externals.isNotEmpty()) {
            insertExternalGames(externals.map { it.copy(gameId = game.id) })
        }
    }

    /** Inserts a matched Game with its Ownerships and external references atomically. */
    @Transaction
    suspend fun insertMatchedGame(
        game: Game,
        stores: Set<Store>,
        externals: List<ExternalGame>,
        source: Source = Source.MANUAL,
    ): Long {
        val gameId = insertGame(game)
        stores.forEach { store ->
            insertOwnership(Ownership(gameId = gameId, store = store, source = source))
        }
        if (externals.isNotEmpty()) {
            insertExternalGames(externals.map { it.copy(gameId = gameId) })
        }
        return gameId
    }
}
