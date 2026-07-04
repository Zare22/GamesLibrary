package hr.kotwave.gameslibrary.data

import hr.kotwave.gameslibrary.transfer.ExportedGame
import hr.kotwave.gameslibrary.transfer.LibraryExport
import hr.kotwave.gameslibrary.transfer.LibraryImportDecision
import hr.kotwave.gameslibrary.transfer.LibraryImportRow
import hr.kotwave.gameslibrary.transfer.LibraryTransfer
import hr.kotwave.gameslibrary.transfer.classifyLibraryImport
import hr.kotwave.gameslibrary.transfer.externalEntities
import hr.kotwave.gameslibrary.transfer.ownershipEntities
import hr.kotwave.gameslibrary.transfer.toGame
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

/** IGDB's integer external-game category for Steam; the Steam appid is its `uid`. */
private const val STEAM_EXTERNAL_CATEGORY = 1

/** IGDB's integer external-game category for GOG; the GOG product id is its `uid`. */
private const val GOG_EXTERNAL_CATEGORY = 5

class GameRepository(
    private val gameDao: GameDao,
    private val clock: Clock = Clock.System,
) {

    /** Owned Games (not Wishlisted) with their Ownerships, for the Library. */
    val ownedGames: Flow<List<GameWithOwnerships>> = gameDao.observeOwnedGames()

    /** Orphaned Games, for the bulk "re-match all orphaned" entry. */
    val orphanedGames: Flow<List<Game>> = gameDao.observeOrphanedGames()

    /** Wishlisted Games, for the Wishlist view. */
    val wishlistGames: Flow<List<Game>> = gameDao.observeWishlistGames()

    /** A single Game with its Ownerships, for the detail screen; emits null once it is deleted. */
    fun observeGame(gameId: Long): Flow<GameWithOwnerships?> = gameDao.observeGame(gameId)

    /** Stamps `addedAt` at insertion time (only if unset), so the Library "recently added" sort has an order. */
    private fun Game.stamped(): Game =
        if (addedAt == null) copy(addedAt = clock.now().toEpochMilliseconds()) else this

    private suspend fun insertStampedGame(game: Game): Long = gameDao.insertGame(game.stamped())

    private suspend fun insertStampedMatchedGame(
        game: Game,
        stores: Set<Store>,
        externals: List<ExternalGame>,
        source: Source = Source.MANUAL,
    ): Long = gameDao.insertMatchedGame(game.stamped(), stores, externals, source)

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
        val gameId = insertStampedGame(
            Game(name = name, igdbId = igdbId, wishlist = false, status = status),
        )
        stores.forEach { store ->
            gameDao.insertOwnership(Ownership(gameId = gameId, store = store, source = Source.MANUAL))
        }
        return gameId
    }

    /** Adds a Wishlisted Game: no Status, no Ownership. Returns the new Game id. */
    suspend fun addWishlistGame(name: String, igdbId: Long? = null): Long =
        insertStampedGame(Game(name = name, igdbId = igdbId, wishlist = true, status = null))

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
        val gameId = insertStampedMatchedGame(
            game = igdb.toGame(wishlist = wishlist, status = if (wishlist) null else status),
            stores = if (wishlist) emptySet() else stores,
            externals = igdb.externalGames.map { it.toEntity() },
        )
        return MatchedAddResult(gameId, alreadyExisted = false)
    }

    /** Sets (or clears, with null) the user's own 0.0–10.0 score. Local-only; never from IGDB. */
    suspend fun setUserRating(gameId: Long, rating: Double?) {
        val game = gameDao.getGame(gameId) ?: return
        gameDao.updateGame(game.copy(userRating = rating))
    }

    /** Sets the play Status of an owned Game. A no-op on a Wishlisted Game (it has no Status). */
    suspend fun setStatus(gameId: Long, status: Status) {
        val game = gameDao.getGame(gameId) ?: return
        if (game.wishlist) return
        gameDao.updateGame(game.copy(status = status))
    }

    /**
     * Removes an Ownership on a Store. Removing the last Ownership leaves an owned-but-untracked
     * Game (it does not become Wishlisted — that is a separate, explicit state).
     */
    suspend fun removeOwnership(gameId: Long, store: Store) = gameDao.deleteOwnership(gameId, store)

    /** Deletes a Game; its Ownerships and external references cascade away. */
    suspend fun deleteGame(gameId: Long) = gameDao.deleteGame(gameId)

    /** Empties the whole library — every Game, Ownership, and external reference. */
    suspend fun clearAllGames() = gameDao.clearAll()

    /**
     * Applies a manual metadata refresh from a fresh [fetched] IGDB result, overwriting IGDB-sourced
     * fields but never local state (`userRating`/Status/Wishlist/Ownership). A null fetch — the
     * `igdb_id` no longer resolves — flags the Game Orphaned and keeps its last-known metadata.
     */
    suspend fun applyRefresh(gameId: Long, fetched: IgdbGame?) {
        val game = gameDao.getGame(gameId) ?: return
        if (fetched == null) {
            if (!game.orphaned) gameDao.updateGame(game.copy(orphaned = true))
            return
        }
        gameDao.replaceMetadata(
            game.withMetadataFrom(fetched).copy(orphaned = false),
            fetched.externalGames.map { it.toEntity() },
        )
    }

    /**
     * Additively syncs the Steam library from already-resolved [entries] (the ViewModel does the
     * Steam + IGDB networking). Adds Games it hasn't seen, ensures a Steam Ownership tagged
     * `STEAM_SYNC` on ones it has, and never removes anything or overwrites cached metadata or local
     * state (Status/userRating/Wishlist). Matched entries dedup by `igdbId`;
     * unmatched ones by their Steam appid in `external_game`.
     */
    suspend fun syncSteamGames(entries: List<SteamSyncEntry>): SteamSyncSummary {
        var added = 0
        var updated = 0
        entries.forEach { entry ->
            val existing = when (entry) {
                is SteamSyncEntry.Matched -> gameDao.getGameByIgdbId(entry.igdb.igdbId)
                is SteamSyncEntry.Unmatched -> gameDao.getGameByExternalUid(STEAM_EXTERNAL_CATEGORY, entry.appid)
            }
            if (existing != null) {
                ensureSteamOwnership(existing)
                updated++
                return@forEach
            }
            when (entry) {
                is SteamSyncEntry.Matched -> insertStampedMatchedGame(
                    game = entry.igdb.toGame(wishlist = false, status = Status.BACKLOG),
                    stores = setOf(Store.STEAM),
                    externals = entry.igdb.externalGames.map { it.toEntity() },
                    source = Source.STEAM_SYNC,
                )
                is SteamSyncEntry.Unmatched -> insertStampedMatchedGame(
                    game = Game(name = entry.name, igdbId = null, wishlist = false, status = Status.BACKLOG),
                    stores = setOf(Store.STEAM),
                    externals = listOf(ExternalGame(gameId = 0, category = STEAM_EXTERNAL_CATEGORY, uid = entry.appid)),
                    source = Source.STEAM_SYNC,
                )
            }
            added++
        }
        return SteamSyncSummary(added = added, updated = updated)
    }

    /**
     * Guarantees a Steam Ownership tagged `STEAM_SYNC` on an existing Game, clearing Wishlist if set.
     * A pre-existing (Game, Steam) Ownership is left in place by the IGNORE insert, then re-tagged
     * `STEAM_SYNC` — Steam is the authority on Steam ownership. Never touches Status/userRating.
     */
    private suspend fun ensureSteamOwnership(game: Game) {
        if (game.wishlist) {
            gameDao.updateGame(game.copy(wishlist = false, status = game.status ?: Status.BACKLOG))
        }
        gameDao.insertOwnership(Ownership(gameId = game.id, store = Store.STEAM, source = Source.STEAM_SYNC))
        gameDao.setOwnershipSource(game.id, Store.STEAM, Source.STEAM_SYNC)
    }

    /**
     * Additively syncs the GOG library from already-resolved [entries] (the ViewModel does the GOG +
     * IGDB networking). Adds Games it hasn't seen, ensures a GOG Ownership tagged `GOG_SYNC` on ones it
     * has, and never removes anything or overwrites cached metadata or local state (Status/userRating/
     * Wishlist). Matched entries dedup by `igdbId`; unmatched ones by their GOG id in
     * `external_game`.
     */
    suspend fun syncGogGames(entries: List<GogSyncEntry>): GogSyncSummary {
        var added = 0
        var updated = 0
        entries.forEach { entry ->
            val existing = when (entry) {
                is GogSyncEntry.Matched -> gameDao.getGameByIgdbId(entry.igdb.igdbId)
                is GogSyncEntry.Unmatched -> gameDao.getGameByExternalUid(GOG_EXTERNAL_CATEGORY, entry.gogId)
            }
            if (existing != null) {
                ensureGogOwnership(existing)
                updated++
                return@forEach
            }
            when (entry) {
                is GogSyncEntry.Matched -> insertStampedMatchedGame(
                    game = entry.igdb.toGame(wishlist = false, status = Status.BACKLOG),
                    stores = setOf(Store.GOG),
                    externals = entry.igdb.externalGames.map { it.toEntity() },
                    source = Source.GOG_SYNC,
                )
                is GogSyncEntry.Unmatched -> insertStampedMatchedGame(
                    game = Game(name = entry.name, igdbId = null, wishlist = false, status = Status.BACKLOG),
                    stores = setOf(Store.GOG),
                    externals = listOf(ExternalGame(gameId = 0, category = GOG_EXTERNAL_CATEGORY, uid = entry.gogId)),
                    source = Source.GOG_SYNC,
                )
            }
            added++
        }
        return GogSyncSummary(added = added, updated = updated)
    }

    /**
     * Guarantees a GOG Ownership tagged `GOG_SYNC` on an existing Game, clearing Wishlist if set. A
     * pre-existing (Game, GOG) Ownership is left in place by the IGNORE insert, then re-tagged `GOG_SYNC`
     * — GOG is the authority on GOG ownership. Never touches Status/userRating.
     */
    private suspend fun ensureGogOwnership(game: Game) {
        if (game.wishlist) {
            gameDao.updateGame(game.copy(wishlist = false, status = game.status ?: Status.BACKLOG))
        }
        gameDao.insertOwnership(Ownership(gameId = game.id, store = Store.GOG, source = Source.GOG_SYNC))
        gameDao.setOwnershipSource(game.id, Store.GOG, Source.GOG_SYNC)
    }

    /**
     * Confirms a paste Import from already-resolved [entries] (the ViewModel does the IGDB networking).
     * Additive only, tagged `PASTE_IMPORT`: a [ImportEntry.Matched] dedups by `igdbId` — attaching an
     * Ownership on its Store to the existing Game (clearing Wishlist) or adding the Game new; an
     * [ImportEntry.Unmatched] adds an `igdb_id`-null Game with that Ownership. Never removes anything or
     * overwrites cached metadata or local state.
     */
    suspend fun confirmImport(entries: List<ImportEntry>): ImportSummary {
        var added = 0
        var attached = 0
        entries.forEach { entry ->
            when (entry) {
                is ImportEntry.Matched -> {
                    val existing = gameDao.getGameByIgdbId(entry.igdb.igdbId)
                    if (existing != null) {
                        if (existing.wishlist) {
                            gameDao.updateGame(existing.copy(wishlist = false, status = existing.status ?: Status.BACKLOG))
                        }
                        gameDao.insertOwnership(Ownership(gameId = existing.id, store = entry.store, source = Source.PASTE_IMPORT))
                        attached++
                    } else {
                        insertStampedMatchedGame(
                            game = entry.igdb.toGame(wishlist = false, status = Status.BACKLOG),
                            stores = setOf(entry.store),
                            externals = entry.igdb.externalGames.map { it.toEntity() },
                            source = Source.PASTE_IMPORT,
                        )
                        added++
                    }
                }
                is ImportEntry.Unmatched -> {
                    val gameId = insertStampedGame(
                        Game(name = entry.name, igdbId = null, wishlist = false, status = Status.BACKLOG),
                    )
                    gameDao.insertOwnership(Ownership(gameId = gameId, store = entry.store, source = Source.PASTE_IMPORT))
                    added++
                }
            }
        }
        return ImportSummary(added = added, attached = attached)
    }

    /** Serializes the whole library (Games, Ownerships, external refs) to the export file format. */
    suspend fun exportLibrary(): String =
        LibraryTransfer.encode(gameDao.allGamesWithOwnerships(), gameDao.allExternalGames().groupBy { it.gameId })

    /** Classifies a decoded [export] against the current library (live keys) for the import Review. */
    suspend fun classifyImport(export: LibraryExport): List<LibraryImportRow> {
        val games = gameDao.allGamesWithOwnerships().map { it.game }
        return classifyLibraryImport(
            export,
            existingIgdbIds = games.mapNotNull { it.igdbId }.toSet(),
            existingTitlesLower = games.map { it.name.trim().lowercase() }.toSet(),
        )
    }

    /**
     * Applies a library import from already-reviewed [decisions]. Additive only: an `igdbId`
     * row dedups by id; an `igdb`-null row merges onto a same-titled Game when [LibraryImportDecision.mergeByTitle]
     * is set, else adds new. Merging unions Ownerships (clearing Wishlist if it brings one) and never
     * overwrites an existing Game's cached metadata or local state. New Games carry their full imported metadata.
     */
    suspend fun importLibrary(decisions: List<LibraryImportDecision>): ImportSummary {
        var added = 0
        var attached = 0
        decisions.forEach { decision ->
            val imported = decision.game
            val existing = when {
                imported.igdbId != null -> gameDao.getGameByIgdbId(imported.igdbId)
                decision.mergeByTitle -> gameDao.gamesByTitle(imported.name).firstOrNull()
                else -> null
            }
            if (existing != null) {
                mergeImportedInto(existing, imported)
                attached++
            } else {
                insertImported(imported)
                added++
            }
        }
        return ImportSummary(added = added, attached = attached)
    }

    /** Unions an imported game's Ownerships onto an existing Game, clearing Wishlist if it brings one. */
    private suspend fun mergeImportedInto(existing: Game, imported: ExportedGame) {
        val ownerships = imported.ownershipEntities(existing.id)
        if (ownerships.isNotEmpty() && existing.wishlist) {
            gameDao.updateGame(existing.copy(wishlist = false, status = existing.status ?: Status.BACKLOG))
        }
        ownerships.forEach { gameDao.insertOwnership(it) }
    }

    /** Inserts an imported Game new, with its full cached metadata, Ownerships, and external references. */
    private suspend fun insertImported(imported: ExportedGame) {
        val gameId = insertStampedGame(imported.toGame())
        imported.ownershipEntities(gameId).forEach { gameDao.insertOwnership(it) }
        imported.externalEntities(gameId).takeIf { it.isNotEmpty() }?.let { gameDao.insertExternalGames(it) }
    }

    /**
     * Re-match: repoints a Game's `igdb_id` to a chosen IGDB entry and overwrites its metadata,
     * clearing Orphaned. Blocked if another Game already holds that `igdb_id` (Match is unique).
     */
    suspend fun applyRematch(gameId: Long, fetched: IgdbGame): RematchResult {
        val game = gameDao.getGame(gameId) ?: return RematchResult.Success
        val holder = gameDao.getGameByIgdbId(fetched.igdbId)
        if (holder != null && holder.id != gameId) return RematchResult.AlreadyInLibrary(holder.id)
        gameDao.replaceMetadata(
            game.withMetadataFrom(fetched).copy(orphaned = false),
            fetched.externalGames.map { it.toEntity() },
        )
        return RematchResult.Success
    }
}
