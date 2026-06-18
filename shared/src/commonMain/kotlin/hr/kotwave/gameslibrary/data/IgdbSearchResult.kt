package hr.kotwave.gameslibrary.data

/** A lightweight IGDB search hit for the results list. Full metadata is fetched only on add. */
data class IgdbSearchResult(
    val igdbId: Long,
    val name: String,
    val coverImageId: String? = null,
    val firstReleaseDate: Long? = null,
)
