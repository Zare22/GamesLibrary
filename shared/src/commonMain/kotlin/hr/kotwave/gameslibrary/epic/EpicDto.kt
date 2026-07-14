package hr.kotwave.gameslibrary.epic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `oauth/token` response — both the authorization_code and refresh_token grants. */
@Serializable
internal data class EpicTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

/** One page of `library/api/public/items` (the complete owned set, ids only). */
@Serializable
internal data class LibraryItemsResponse(
    val records: List<LibraryRecordDto> = emptyList(),
    val responseMetadata: LibraryResponseMetadata = LibraryResponseMetadata(),
)

@Serializable
internal data class LibraryRecordDto(
    val namespace: String? = null,
    val catalogItemId: String? = null,
    val recordType: String? = null,
)

@Serializable
internal data class LibraryResponseMetadata(val nextCursor: String? = null)

/**
 * One catalog item from `bulk/items` (the response is a map keyed by item id). A non-empty
 * [mainGameItemList] marks the item as DLC/an add-on of the games it points at.
 */
@Serializable
internal data class CatalogItemDto(
    val title: String? = null,
    val mainGameItemList: List<MainGameRefDto> = emptyList(),
)

@Serializable
internal data class MainGameRefDto(val id: String? = null)

/** One page of a namespace's store offers; paged via `start`/`count` against [OffersPaging.total]. */
@Serializable
internal data class OffersResponse(
    val elements: List<OfferDto> = emptyList(),
    val paging: OffersPaging = OffersPaging(),
)

@Serializable
internal data class OfferDto(
    val id: String? = null,
    val title: String? = null,
    val offerType: String? = null,
)

@Serializable
internal data class OffersPaging(
    val start: Int = 0,
    val count: Int = 0,
    val total: Int = 0,
)
