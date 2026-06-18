package hr.kotwave.gameslibrary.data

/**
 * A Game discovered via IGDB search, with the v1 metadata set, ready to add. The IGDB client maps
 * its responses into this; [GameRepository.addMatchedGame] turns it into a local [Game].
 */
data class IgdbGame(
    val igdbId: Long,
    val name: String,
    val slug: String? = null,
    val firstReleaseDate: Long? = null,
    val coverImageId: String? = null,
    val developer: String? = null,
    val totalRating: Double? = null,
    val totalRatingCount: Int? = null,
    val platforms: List<Platform> = emptyList(),
    val alternativeNames: List<String> = emptyList(),
    val externalGames: List<ExternalRef> = emptyList(),
)

/** An IGDB external-game reference before it is attached to a local Game. */
data class ExternalRef(val category: Int, val uid: String, val url: String? = null)

internal fun IgdbGame.toGame(wishlist: Boolean, status: Status?): Game = Game(
    name = name,
    igdbId = igdbId,
    wishlist = wishlist,
    status = status,
    slug = slug,
    firstReleaseDate = firstReleaseDate,
    coverImageId = coverImageId,
    developer = developer,
    totalRating = totalRating,
    totalRatingCount = totalRatingCount,
    platforms = platforms,
    alternativeNames = alternativeNames,
)

internal fun ExternalRef.toEntity(): ExternalGame =
    ExternalGame(gameId = 0, category = category, uid = uid, url = url)
