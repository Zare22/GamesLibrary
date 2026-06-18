package hr.kotwave.gameslibrary.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {

    /** Owned Games (not Wishlisted) with their Ownerships, for the Library. */
    val ownedGames: Flow<List<GameWithOwnerships>> = gameDao.observeOwnedGames()

    /**
     * Adds an owned Game: not Wishlisted, with a Status and zero or more Ownerships
     * (zero Stores = have-but-untracked). Returns the new Game id.
     */
    suspend fun addOwnedGame(
        name: String,
        status: Status = Status.BACKLOG,
        stores: Set<Store> = emptySet(),
        igdbId: Long? = null,
    ): Long {
        val gameId = gameDao.insertGame(
            Game(name = name, igdbId = igdbId, wishlist = false, status = status),
        )
        stores.forEach { store ->
            gameDao.insertOwnership(Ownership(gameId = gameId, store = store, source = Source.MANUAL))
        }
        return gameId
    }

    /** Adds a Wishlisted Game: no Status, no Ownership. Returns the new Game id. */
    suspend fun addWishlistGame(name: String, igdbId: Long? = null): Long =
        gameDao.insertGame(Game(name = name, igdbId = igdbId, wishlist = true, status = null))

    /**
     * Records an Ownership on a Store. If the Game was Wishlisted, that clears Wishlist and
     * gives it a Status (Backlog), since an owned Game is never Wishlisted.
     */
    suspend fun addOwnership(gameId: Long, store: Store, source: Source = Source.MANUAL) {
        val game = gameDao.getGame(gameId) ?: return
        if (game.wishlist) {
            gameDao.updateGame(game.copy(wishlist = false, status = game.status ?: Status.BACKLOG))
        }
        gameDao.insertOwnership(Ownership(gameId = gameId, store = store, source = source))
    }

    /** Existing Games whose title matches case-insensitively, for the soft duplicate warning. */
    suspend fun similarTitles(name: String): List<Game> = gameDao.gamesByTitle(name)

    /**
     * Adds a Game discovered via IGDB search, caching its metadata and keyed by `igdbId`. Dedups
     * by `igdbId` (Match): if the `igdbId` is already present, attaches to that Game instead of
     * duplicating — own-it adds the chosen Stores (clearing Wishlist if set); a Wishlist pick on an
     * already-added Game is a no-op. Never overwrites cached metadata. Returns the Game id and
     * whether it already existed.
     */
    suspend fun addMatchedGame(
        igdb: IgdbGame,
        wishlist: Boolean,
        status: Status = Status.BACKLOG,
        stores: Set<Store> = emptySet(),
    ): MatchedAddResult {
        gameDao.getGameByIgdbId(igdb.igdbId)?.let { existing ->
            if (!wishlist) {
                if (existing.wishlist) {
                    gameDao.updateGame(existing.copy(wishlist = false, status = existing.status ?: status))
                }
                stores.forEach { store ->
                    gameDao.insertOwnership(Ownership(gameId = existing.id, store = store, source = Source.MANUAL))
                }
            }
            return MatchedAddResult(existing.id, alreadyExisted = true)
        }
        val gameId = gameDao.insertMatchedGame(
            game = igdb.toGame(wishlist = wishlist, status = if (wishlist) null else status),
            stores = if (wishlist) emptySet() else stores,
            externals = igdb.externalGames.map { it.toEntity() },
        )
        return MatchedAddResult(gameId, alreadyExisted = false)
    }
}
