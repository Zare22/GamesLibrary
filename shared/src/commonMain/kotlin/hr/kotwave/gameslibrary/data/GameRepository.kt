package hr.kotwave.gameslibrary.data

import hr.kotwave.gameslibrary.mirror.MirrorOutcome
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

/** IGDB's integer external-game category for the Playstation Store; the PSN conceptId is its `uid`. */
private const val PSN_EXTERNAL_CATEGORY = 36

/** IGDB's integer external-game category for the Epic Games Store; the Epic store offerId is its `uid`. */
private const val EPIC_EXTERNAL_CATEGORY = 26

/** Uids per `externalUidsWithIgdbMatch` query — under SQLite's 999 bound-parameter limit. */
private const val UID_QUERY_CHUNK = 500

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
     * `STEAM_SYNC` on ones it has (recording the entry's appids as external references), and never
     * removes anything or overwrites cached metadata or local state (Status/userRating/Wishlist).
     * Matched entries dedup by `igdbId`; unmatched ones by their Steam appid in `external_game`.
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
                val uids = when (entry) {
                    is SteamSyncEntry.Matched -> entry.appids
                    is SteamSyncEntry.Unmatched -> listOf(entry.appid)
                }
                ensureExternalUids(existing.id, STEAM_EXTERNAL_CATEGORY, uids)
                updated++
                return@forEach
            }
            when (entry) {
                is SteamSyncEntry.Matched -> insertStampedMatchedGame(
                    game = entry.igdb.toGame(wishlist = false, status = Status.BACKLOG),
                    stores = setOf(Store.STEAM),
                    externals = externalsWithStoreUids(entry.igdb, STEAM_EXTERNAL_CATEGORY, entry.appids),
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
     * has (recording the entry's product ids as external references), and never removes anything or
     * overwrites cached metadata or local state (Status/userRating/Wishlist). Matched entries dedup by
     * `igdbId`; unmatched ones by their GOG id in `external_game`.
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
                val uids = when (entry) {
                    is GogSyncEntry.Matched -> entry.gogIds
                    is GogSyncEntry.Unmatched -> listOf(entry.gogId)
                }
                ensureExternalUids(existing.id, GOG_EXTERNAL_CATEGORY, uids)
                updated++
                return@forEach
            }
            when (entry) {
                is GogSyncEntry.Matched -> insertStampedMatchedGame(
                    game = entry.igdb.toGame(wishlist = false, status = Status.BACKLOG),
                    stores = setOf(Store.GOG),
                    externals = externalsWithStoreUids(entry.igdb, GOG_EXTERNAL_CATEGORY, entry.gogIds),
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
     * Additively syncs the PSN library from already-resolved [entries] (the ViewModel does the PSN +
     * IGDB networking). Adds Games it hasn't seen, ensures a PSN Ownership tagged `PSN_SYNC` on ones it
     * has, and never removes anything or overwrites cached metadata or local state (Status/userRating/
     * Wishlist). Entries dedup by `igdbId` and by every PSN uid in `external_game`; a Matched entry
     * landing on an `igdbId`-null Game from an earlier sync upgrades its metadata in place (the
     * re-match rule: IGDB-sourced fields only, local state untouched).
     */
    suspend fun syncPsnGames(entries: List<PsnSyncEntry>): PsnSyncSummary {
        var added = 0
        var updated = 0
        entries.forEach { entry ->
            val existing = when (entry) {
                is PsnSyncEntry.Matched -> gameDao.getGameByIgdbId(entry.igdb.igdbId)
                    ?: entry.psnUids.firstNotNullOfOrNull { gameDao.getGameByExternalUid(PSN_EXTERNAL_CATEGORY, it) }
                is PsnSyncEntry.Unmatched ->
                    entry.psnUids.firstNotNullOfOrNull { gameDao.getGameByExternalUid(PSN_EXTERNAL_CATEGORY, it) }
            }
            if (existing != null) {
                ensurePsnOwnership(existing)
                if (entry is PsnSyncEntry.Matched && existing.igdbId == null) {
                    val current = gameDao.getGame(existing.id) ?: return@forEach
                    gameDao.replaceMetadata(
                        current.withMetadataFrom(entry.igdb),
                        externalsWithStoreUids(entry.igdb, PSN_EXTERNAL_CATEGORY, entry.psnUids),
                    )
                } else {
                    ensureExternalUids(existing.id, PSN_EXTERNAL_CATEGORY, entry.psnUids)
                }
                updated++
                return@forEach
            }
            when (entry) {
                is PsnSyncEntry.Matched -> insertStampedMatchedGame(
                    game = entry.igdb.toGame(wishlist = false, status = Status.BACKLOG),
                    stores = setOf(Store.PSN),
                    externals = externalsWithStoreUids(entry.igdb, PSN_EXTERNAL_CATEGORY, entry.psnUids),
                    source = Source.PSN_SYNC,
                )
                is PsnSyncEntry.Unmatched -> insertStampedMatchedGame(
                    game = Game(name = entry.name, igdbId = null, wishlist = false, status = Status.BACKLOG),
                    stores = setOf(Store.PSN),
                    externals = entry.psnUids.map { ExternalGame(gameId = 0, category = PSN_EXTERNAL_CATEGORY, uid = it) },
                    source = Source.PSN_SYNC,
                )
            }
            added++
        }
        return PsnSyncSummary(added = added, updated = updated)
    }

    /**
     * Which of these PSN [uids] already belong to an IGDB-matched Game — their rows need no concept
     * resolution on a re-sync. Chunked to respect SQLite's bound-parameter limit.
     */
    suspend fun psnUidsAlreadyMatched(uids: List<String>): Set<String> =
        uids.distinct().chunked(UID_QUERY_CHUNK)
            .flatMap { gameDao.externalUidsWithIgdbMatch(PSN_EXTERNAL_CATEGORY, it) }
            .toSet()

    /**
     * Guarantees a PSN Ownership tagged `PSN_SYNC` on an existing Game, clearing Wishlist if set. A
     * pre-existing (Game, PSN) Ownership is left in place by the IGNORE insert, then re-tagged `PSN_SYNC`
     * — PSN is the authority on PSN ownership. Never touches Status/userRating.
     */
    private suspend fun ensurePsnOwnership(game: Game) {
        if (game.wishlist) {
            gameDao.updateGame(game.copy(wishlist = false, status = game.status ?: Status.BACKLOG))
        }
        gameDao.insertOwnership(Ownership(gameId = game.id, store = Store.PSN, source = Source.PSN_SYNC))
        gameDao.setOwnershipSource(game.id, Store.PSN, Source.PSN_SYNC)
    }

    /**
     * Additively syncs the Epic library from already-resolved [entries] (the ViewModel does the Epic +
     * IGDB networking). Adds Games it hasn't seen, ensures an Epic Ownership tagged `EPIC_SYNC` on ones
     * it has, and never removes anything or overwrites cached metadata or local state (Status/userRating/
     * Wishlist). Entries dedup by `igdbId` and by every Epic uid in `external_game`; a Matched entry
     * landing on an `igdbId`-null Game from an earlier sync upgrades its metadata in place (the
     * re-match rule: IGDB-sourced fields only, local state untouched).
     */
    suspend fun syncEpicGames(entries: List<EpicSyncEntry>): EpicSyncSummary {
        var added = 0
        var updated = 0
        entries.forEach { entry ->
            val existing = when (entry) {
                is EpicSyncEntry.Matched -> gameDao.getGameByIgdbId(entry.igdb.igdbId)
                    ?: entry.epicUids.firstNotNullOfOrNull { gameDao.getGameByExternalUid(EPIC_EXTERNAL_CATEGORY, it) }
                is EpicSyncEntry.Unmatched ->
                    entry.epicUids.firstNotNullOfOrNull { gameDao.getGameByExternalUid(EPIC_EXTERNAL_CATEGORY, it) }
            }
            if (existing != null) {
                ensureEpicOwnership(existing)
                if (entry is EpicSyncEntry.Matched && existing.igdbId == null) {
                    val current = gameDao.getGame(existing.id) ?: return@forEach
                    gameDao.replaceMetadata(
                        current.withMetadataFrom(entry.igdb),
                        externalsWithStoreUids(entry.igdb, EPIC_EXTERNAL_CATEGORY, entry.epicUids),
                    )
                } else {
                    ensureExternalUids(existing.id, EPIC_EXTERNAL_CATEGORY, entry.epicUids)
                }
                updated++
                return@forEach
            }
            when (entry) {
                is EpicSyncEntry.Matched -> insertStampedMatchedGame(
                    game = entry.igdb.toGame(wishlist = false, status = Status.BACKLOG),
                    stores = setOf(Store.EPIC),
                    externals = externalsWithStoreUids(entry.igdb, EPIC_EXTERNAL_CATEGORY, entry.epicUids),
                    source = Source.EPIC_SYNC,
                )
                is EpicSyncEntry.Unmatched -> insertStampedMatchedGame(
                    game = Game(name = entry.name, igdbId = null, wishlist = false, status = Status.BACKLOG),
                    stores = setOf(Store.EPIC),
                    externals = entry.epicUids.map { ExternalGame(gameId = 0, category = EPIC_EXTERNAL_CATEGORY, uid = it) },
                    source = Source.EPIC_SYNC,
                )
            }
            added++
        }
        return EpicSyncSummary(added = added, updated = updated)
    }

    /**
     * The IGDB external references plus a [category] reference for every [uids] entry IGDB didn't
     * already carry — so re-syncs can dedup (and skip resolution legs) by every store uid behind the
     * entry, even ones IGDB doesn't know.
     */
    private fun externalsWithStoreUids(igdb: IgdbGame, category: Int, uids: List<String>): List<ExternalGame> {
        val fromIgdb = igdb.externalGames.map { it.toEntity() }
        val known = fromIgdb.filter { it.category == category }.map { it.uid }.toSet()
        return fromIgdb + uids.filter { it !in known }
            .map { ExternalGame(gameId = 0, category = category, uid = it) }
    }

    /** Adds the [uids] a Game doesn't hold yet as [category] external references. Additive only. */
    private suspend fun ensureExternalUids(gameId: Long, category: Int, uids: List<String>) {
        if (uids.isEmpty()) return
        val known = gameDao.externalUidsFor(gameId, category).toSet()
        val missing = uids.distinct().filterNot { it in known }
        if (missing.isNotEmpty()) {
            gameDao.insertExternalGames(missing.map { ExternalGame(gameId = gameId, category = category, uid = it) })
        }
    }

    /**
     * Which of these Epic [uids] already belong to an IGDB-matched Game — their namespaces need no
     * offer resolution on a re-sync. Chunked to respect SQLite's bound-parameter limit.
     */
    suspend fun epicUidsAlreadyMatched(uids: List<String>): Set<String> =
        uids.distinct().chunked(UID_QUERY_CHUNK)
            .flatMap { gameDao.externalUidsWithIgdbMatch(EPIC_EXTERNAL_CATEGORY, it) }
            .toSet()

    /**
     * Guarantees an Epic Ownership tagged `EPIC_SYNC` on an existing Game, clearing Wishlist if set. A
     * pre-existing (Game, Epic) Ownership is left in place by the IGNORE insert, then re-tagged `EPIC_SYNC`
     * — Epic is the authority on Epic ownership. Never touches Status/userRating.
     */
    private suspend fun ensureEpicOwnership(game: Game) {
        if (game.wishlist) {
            gameDao.updateGame(game.copy(wishlist = false, status = game.status ?: Status.BACKLOG))
        }
        gameDao.insertOwnership(Ownership(gameId = game.id, store = Store.EPIC, source = Source.EPIC_SYNC))
        gameDao.setOwnershipSource(game.id, Store.EPIC, Source.EPIC_SYNC)
    }

    /**
     * Partitions a sync's id-unmatched tail for the Review picker: rows with any uid already on a Game
     * (matched or bare — the user gave a verdict once) are [SyncTailSplit.known] and only need their
     * ownership ensured; rows with any uid dismissed from an earlier Review are dropped; the rest are
     * [SyncTailSplit.needsReview]. Chunked to respect SQLite's bound-parameter limit.
     */
    suspend fun splitSyncTail(store: Store, rows: List<SyncTailRow>): SyncTailSplit {
        val category = syncCategoryFor(store)
        val uids = rows.flatMap { it.uids }.distinct()
        val known = uids.chunked(UID_QUERY_CHUNK).flatMap { gameDao.knownExternalUids(category, it) }.toSet()
        val dismissed = uids.chunked(UID_QUERY_CHUNK).flatMap { gameDao.dismissedSyncUids(category, it) }.toSet()
        val knownRows = ArrayList<SyncTailRow>()
        val needsReview = ArrayList<SyncTailRow>()
        rows.forEach { row ->
            when {
                row.uids.any { it in known } -> knownRows += row
                row.uids.any { it in dismissed } -> Unit
                else -> needsReview += row
            }
        }
        return SyncTailSplit(known = knownRows, needsReview = needsReview)
    }

    /**
     * Applies a confirmed sync Review: records [dismissed] rows' uids in `sync_dismissal`, then merges
     * the [picks] (user-chosen IGDB matches, carrying their store uids) and [bare] rows (added as
     * `igdbId`-null Games) through the store's sync merge — same `*_SYNC` tagging, dedup, and
     * uid recording as the sync itself.
     */
    suspend fun confirmSyncReview(
        store: Store,
        picks: List<SyncReviewPick>,
        bare: List<SyncTailRow>,
        dismissed: List<SyncTailRow>,
    ): SyncReviewOutcome {
        val category = syncCategoryFor(store)
        dismissed.flatMap { it.uids }.distinct()
            .takeIf { it.isNotEmpty() }
            ?.let { uids -> gameDao.insertSyncDismissals(uids.map { SyncDismissal(category = category, uid = it) }) }
        if (picks.isEmpty() && bare.isEmpty()) return SyncReviewOutcome(added = 0, updated = 0)
        return when (store) {
            Store.STEAM -> syncSteamGames(
                picks.map { SteamSyncEntry.Matched(it.igdb, it.uids) } +
                    bare.map { SteamSyncEntry.Unmatched(appid = it.uids.first(), name = it.name) },
            ).let { SyncReviewOutcome(it.added, it.updated) }
            Store.GOG -> syncGogGames(
                picks.map { GogSyncEntry.Matched(it.igdb, it.uids) } +
                    bare.map { GogSyncEntry.Unmatched(gogId = it.uids.first(), name = it.name) },
            ).let { SyncReviewOutcome(it.added, it.updated) }
            Store.PSN -> syncPsnGames(
                picks.map { PsnSyncEntry.Matched(it.igdb, it.uids) } +
                    bare.map { PsnSyncEntry.Unmatched(psnUids = it.uids, name = it.name) },
            ).let { SyncReviewOutcome(it.added, it.updated) }
            Store.EPIC -> syncEpicGames(
                picks.map { EpicSyncEntry.Matched(it.igdb, it.uids) } +
                    bare.map { EpicSyncEntry.Unmatched(epicUids = it.uids, name = it.name) },
            ).let { SyncReviewOutcome(it.added, it.updated) }
            else -> error("Store $store has no sync")
        }
    }

    /** The IGDB external-game category a store's sync keys uids by. Only the four synced stores have one. */
    private fun syncCategoryFor(store: Store): Int = when (store) {
        Store.STEAM -> STEAM_EXTERNAL_CATEGORY
        Store.GOG -> GOG_EXTERNAL_CATEGORY
        Store.PSN -> PSN_EXTERNAL_CATEGORY
        Store.EPIC -> EPIC_EXTERNAL_CATEGORY
        else -> error("Store $store has no sync")
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

    /** The stored Mirror Baseline for a pairing, or null before its first completed Mirror. */
    suspend fun mirrorBaseline(pairingId: String): LibraryExport? =
        gameDao.mirrorBaseline(pairingId)?.let { LibraryTransfer.decode(it) }

    /**
     * Applies a resolved Mirror outcome to the local library and persists its converged snapshot as
     * the pairing's new Baseline, in one transaction. `addedAt` travels as-is: a null stamp stays
     * null (the row sorts as oldest on both replicas) rather than re-stamping at apply time.
     */
    suspend fun applyMirrorMerge(pairingId: String, outcome: MirrorOutcome) {
        gameDao.applyMirror(
            deletes = outcome.mineChanges.deletes.map { it.toMirrorWrite() },
            upserts = (outcome.mineChanges.adds + outcome.mineChanges.updates).map { it.toMirrorWrite() },
            dismissals = outcome.dismissals,
            baseline = MirrorBaseline(
                pairingId = pairingId,
                snapshot = LibraryTransfer.encode(LibraryExport(games = outcome.converged)),
            ),
        )
    }

    private fun ExportedGame.toMirrorWrite(): MirrorGameWrite = MirrorGameWrite(
        game = toGame(),
        ownerships = ownershipEntities(gameId = 0),
        externals = externalEntities(gameId = 0),
    )

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
