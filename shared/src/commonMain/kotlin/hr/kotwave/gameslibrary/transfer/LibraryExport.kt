package hr.kotwave.gameslibrary.transfer

import hr.kotwave.gameslibrary.data.Platform
import kotlinx.serialization.Serializable

/** The export schema version written to and expected from a library file. */
const val LIBRARY_EXPORT_VERSION = 1

/**
 * A versioned, lossless snapshot of the whole library, decoupled from the Room schema. Enum-valued
 * fields (status/store/source) are plain strings, so a newer export naming a Store/Status this build
 * doesn't know loads instead of failing — unknown values are dropped row-by-row on import, never fatal.
 */
@Serializable
data class LibraryExport(
    val schemaVersion: Int = LIBRARY_EXPORT_VERSION,
    val games: List<ExportedGame> = emptyList(),
)

/** One Game with its full cached metadata, Ownerships, and external references. Omits the local id. */
@Serializable
data class ExportedGame(
    val name: String,
    val igdbId: Long? = null,
    val wishlist: Boolean = false,
    val status: String? = null,
    val userRating: Double? = null,
    val slug: String? = null,
    val firstReleaseDate: Long? = null,
    val coverImageId: String? = null,
    val developer: String? = null,
    val totalRating: Double? = null,
    val totalRatingCount: Int? = null,
    val platforms: List<Platform> = emptyList(),
    val alternativeNames: List<String> = emptyList(),
    val orphaned: Boolean = false,
    val addedAt: Long? = null,
    val ownerships: List<ExportedOwnership> = emptyList(),
    val externals: List<ExportedExternal> = emptyList(),
)

@Serializable
data class ExportedOwnership(val store: String, val source: String)

@Serializable
data class ExportedExternal(val category: Int, val uid: String, val url: String? = null)
