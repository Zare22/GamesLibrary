package hr.kotwave.gameslibrary.psn

import hr.kotwave.gameslibrary.importer.PsnParser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** `oauth/token` response — both the authorization_code and refresh_token grants. */
@Serializable
internal data class PsnTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

/** The `getPurchasedGameList` GraphQL envelope; Apollo reports errors with a 200. */
@Serializable
internal data class PurchasedGamesEnvelope(
    val data: PurchasedGamesData? = null,
    val errors: List<GraphqlError> = emptyList(),
)

@Serializable
internal data class PurchasedGamesData(val purchasedTitlesRetrieve: PurchasedTitles? = null)

@Serializable
internal data class PurchasedTitles(
    val games: List<PurchasedGameDto> = emptyList(),
    val pageInfo: PurchasedPageInfo = PurchasedPageInfo(),
)

@Serializable
internal data class PurchasedPageInfo(val offset: Int = 0, val totalCount: Int = 0)

/** `conceptId` arrives as a string, a number, or null depending on the entitlement — kept raw here. */
@Serializable
internal data class PurchasedGameDto(
    val conceptId: JsonPrimitive? = null,
    val titleId: String? = null,
    val name: String? = null,
)

@Serializable
internal data class GraphqlError(
    val message: String? = null,
    val extensions: GraphqlErrorExtensions? = null,
)

@Serializable
internal data class GraphqlErrorExtensions(val code: String? = null)

internal fun GraphqlError.isPersistedQueryNotFound(): Boolean =
    message == "PersistedQueryNotFound" || extensions?.code == "PERSISTED_QUERY_NOT_FOUND"

/** One page of `gamelist/v2/users/me/titles` (the played list). */
@Serializable
internal data class PlayedTitlesResponse(
    val titles: List<PlayedTitleDto> = emptyList(),
    val nextOffset: Int? = null,
)

@Serializable
internal data class PlayedTitleDto(
    val titleId: String? = null,
    val name: String? = null,
    val concept: PlayedConceptDto? = null,
)

@Serializable
internal data class PlayedConceptDto(val id: Long? = null)

/** One concept from `catalog/v2/titles/{titleId}/concepts`; only the id is read. */
@Serializable
internal data class ConceptDto(val id: JsonPrimitive? = null)

internal fun PurchasedGameDto.toOwnedGame(): PsnOwnedGame? = ownedGame(conceptId?.contentOrNull, titleId, name)

internal fun PlayedTitleDto.toOwnedGame(): PsnOwnedGame? = ownedGame(concept?.id?.toString(), titleId, name)

/** Rows without a titleId or a usable name are skipped; names shed `®`/`™`/`©` as in the paste parser. */
private fun ownedGame(conceptId: String?, titleId: String?, name: String?): PsnOwnedGame? {
    if (titleId.isNullOrBlank() || name == null) return null
    val cleaned = PsnParser.cleanTitle(name)
    if (cleaned.isEmpty()) return null
    return PsnOwnedGame(conceptId = conceptId?.takeIf { it.isNotBlank() }, titleId = titleId, name = cleaned)
}
