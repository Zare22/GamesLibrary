package hr.kotwave.gameslibrary.transfer

import hr.kotwave.gameslibrary.data.ExternalGame
import hr.kotwave.gameslibrary.data.GameWithOwnerships
import kotlinx.serialization.json.Json

/**
 * Serializes the library to, and parses it from, the [LibraryExport] file format. Pure and
 * `:shared`-testable; the DB gathering lives in the repository. Tolerates unknown keys so a file
 * written by a newer app still loads in an older one.
 */
object LibraryTransfer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Encodes an already-built export snapshot (the Mirror Baseline / push payload path). */
    fun encode(export: LibraryExport): String = json.encodeToString(LibraryExport.serializer(), export)

    /** Encodes the full library. [externalsByGameId] groups `external_game` rows by their Game id. */
    fun encode(
        games: List<GameWithOwnerships>,
        externalsByGameId: Map<Long, List<ExternalGame>>,
    ): String {
        val export = LibraryExport(
            games = games.map { withOwnerships ->
                val game = withOwnerships.game
                ExportedGame(
                    name = game.name,
                    igdbId = game.igdbId,
                    wishlist = game.wishlist,
                    status = game.status?.name,
                    userRating = game.userRating,
                    slug = game.slug,
                    firstReleaseDate = game.firstReleaseDate,
                    coverImageId = game.coverImageId,
                    developer = game.developer,
                    totalRating = game.totalRating,
                    totalRatingCount = game.totalRatingCount,
                    platforms = game.platforms,
                    alternativeNames = game.alternativeNames,
                    orphaned = game.orphaned,
                    addedAt = game.addedAt,
                    ownerships = withOwnerships.ownerships.map { ExportedOwnership(it.store.name, it.source.name) },
                    externals = (externalsByGameId[game.id] ?: emptyList()).map {
                        ExportedExternal(it.category, it.uid, it.url)
                    },
                )
            },
        )
        return json.encodeToString(LibraryExport.serializer(), export)
    }

    /** Parses an export file; throws [kotlinx.serialization.SerializationException] on malformed JSON. */
    fun decode(text: String): LibraryExport = json.decodeFromString(LibraryExport.serializer(), text)
}
