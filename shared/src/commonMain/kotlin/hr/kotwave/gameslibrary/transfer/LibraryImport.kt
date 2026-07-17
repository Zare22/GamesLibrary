package hr.kotwave.gameslibrary.transfer

import hr.kotwave.gameslibrary.data.ExternalGame
import hr.kotwave.gameslibrary.data.Game
import hr.kotwave.gameslibrary.data.Ownership
import hr.kotwave.gameslibrary.data.Source
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store

/** How an imported [ExportedGame] relates to the current library — the initial Review classification. */
enum class ImportRowKind {
    /** Matched by `igdbId` to a Game already present; its Ownerships will be unioned in. */
    AlreadyById,

    /** A new `igdbId`-matched Game not yet in the library. */
    NewMatched,

    /** An `igdb`-null Game whose title collides with an existing one — merge onto it, or add new. */
    TitleCollision,

    /** An `igdb`-null Game with no title collision — adds as a manual Game. */
    NewManual,
}

/** One imported game with how it relates to the current library, for Review. */
data class LibraryImportRow(val game: ExportedGame, val kind: ImportRowKind)

/**
 * A confirmed Review decision handed to [hr.kotwave.gameslibrary.data.GameRepository.importLibrary].
 * [mergeByTitle] only governs a [ImportRowKind.TitleCollision] row: true merges Ownerships onto the
 * same-titled Game, false adds it as a new Game. Ignored for every other kind (`igdbId` rows always
 * dedup by id; new rows always add).
 */
data class LibraryImportDecision(val game: ExportedGame, val mergeByTitle: Boolean = true)

/**
 * Classifies each imported game against the current library so Review can show what each row will do.
 * Pure — the caller supplies the existing keys ([existingIgdbIds] and lower-cased [existingTitlesLower]).
 */
fun classifyLibraryImport(
    export: LibraryExport,
    existingIgdbIds: Set<Long>,
    existingTitlesLower: Set<String>,
): List<LibraryImportRow> = export.games.map { game ->
    val kind = when {
        game.igdbId != null && game.igdbId in existingIgdbIds -> ImportRowKind.AlreadyById
        game.igdbId != null -> ImportRowKind.NewMatched
        game.name.trim().lowercase() in existingTitlesLower -> ImportRowKind.TitleCollision
        else -> ImportRowKind.NewManual
    }
    LibraryImportRow(game, kind)
}

/** The Store named by this string, or null if this build doesn't know it (dropped on import). */
internal fun storeOrNull(name: String): Store? = Store.entries.firstOrNull { it.name == name }

/** The Status named by this string, or null if absent/unknown. */
internal fun statusOrNull(name: String?): Status? = name?.let { n -> Status.entries.firstOrNull { it.name == n } }

/** The Source named by this string, defaulting to MANUAL for an unknown value. */
internal fun sourceOrDefault(name: String): Source = Source.entries.firstOrNull { it.name == name } ?: Source.MANUAL

/**
 * Maps an imported game's cached fields to a Game row. An owned (non-wishlist) game with no known
 * Status defaults to Backlog (the invariant: an owned Game always has a Status); a wishlist game has none.
 */
fun ExportedGame.toGame(): Game = Game(
    name = name,
    igdbId = igdbId,
    wishlist = wishlist,
    status = if (wishlist) null else (statusOrNull(status) ?: Status.BACKLOG),
    userRating = userRating,
    slug = slug,
    firstReleaseDate = firstReleaseDate,
    coverImageId = coverImageId,
    developer = developer,
    totalRating = totalRating,
    totalRatingCount = totalRatingCount,
    platforms = platforms,
    alternativeNames = alternativeNames,
    orphaned = orphaned,
    addedAt = addedAt,
)

/** The Ownership rows for this imported game on [gameId], dropping any naming an unknown Store. */
fun ExportedGame.ownershipEntities(gameId: Long): List<Ownership> =
    ownerships.mapNotNull { owned ->
        storeOrNull(owned.store)?.let { store ->
            Ownership(gameId = gameId, store = store, source = sourceOrDefault(owned.source))
        }
    }

/** The external-reference rows for this imported game on [gameId]. */
fun ExportedGame.externalEntities(gameId: Long): List<ExternalGame> =
    externals.map { ExternalGame(gameId = gameId, category = it.category, uid = it.uid, url = it.url) }
