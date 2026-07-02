package hr.kotwave.gameslibrary.igdb

import hr.kotwave.gameslibrary.data.ExternalRef
import hr.kotwave.gameslibrary.data.IgdbGame
import hr.kotwave.gameslibrary.data.IgdbSearchResult
import hr.kotwave.gameslibrary.data.Platform
import kotlinx.serialization.Serializable

/** IGDB `games` response. Fields are nullable: search asks for a subset, full fetch for all. */
@Serializable
internal data class GameDto(
    val id: Long,
    val name: String,
    val slug: String? = null,
    val firstReleaseDate: Long? = null,
    val cover: CoverDto? = null,
    val involvedCompanies: List<InvolvedCompanyDto>? = null,
    val platforms: List<PlatformDto>? = null,
    val totalRating: Double? = null,
    val totalRatingCount: Int? = null,
    val externalGames: List<ExternalGameDto>? = null,
    val alternativeNames: List<AlternativeNameDto>? = null,
)

@Serializable internal data class CoverDto(val imageId: String? = null)
@Serializable internal data class InvolvedCompanyDto(val company: CompanyDto? = null, val developer: Boolean = false)
@Serializable internal data class CompanyDto(val name: String? = null)
@Serializable internal data class PlatformDto(val name: String? = null, val abbreviation: String? = null)
@Serializable internal data class ExternalGameDto(val uid: String? = null, val externalGameSource: Int? = null, val url: String? = null)
@Serializable internal data class AlternativeNameDto(val name: String? = null)

internal fun GameDto.toSearchResult(): IgdbSearchResult = IgdbSearchResult(
    igdbId = id,
    name = name,
    coverImageId = cover?.imageId,
    firstReleaseDate = firstReleaseDate,
    developer = involvedCompanies?.firstOrNull { it.developer }?.company?.name,
)

internal fun GameDto.toIgdbGame(): IgdbGame = IgdbGame(
    igdbId = id,
    name = name,
    slug = slug,
    firstReleaseDate = firstReleaseDate,
    coverImageId = cover?.imageId,
    developer = involvedCompanies?.firstOrNull { it.developer }?.company?.name,
    totalRating = totalRating,
    totalRatingCount = totalRatingCount,
    platforms = platforms.orEmpty().mapNotNull { dto -> dto.name?.let { Platform(it, dto.abbreviation) } },
    alternativeNames = alternativeNames.orEmpty().mapNotNull { it.name },
    externalGames = externalGames.orEmpty().mapNotNull { dto ->
        if (dto.uid != null && dto.externalGameSource != null) ExternalRef(dto.externalGameSource, dto.uid, dto.url) else null
    },
)
