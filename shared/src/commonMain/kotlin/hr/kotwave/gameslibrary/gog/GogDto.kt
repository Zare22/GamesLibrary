package hr.kotwave.gameslibrary.gog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `auth.gog.com/token` response — both the authorization_code and refresh_token grants. */
@Serializable
internal data class GogTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

/** One page of `getFilteredProducts` (the owned library). */
@Serializable
internal data class FilteredProductsResponse(
    val page: Int = 1,
    val totalPages: Int = 1,
    val products: List<ProductDto> = emptyList(),
)

@Serializable
internal data class ProductDto(val id: Long, val title: String? = null)

internal fun ProductDto.toOwnedGame(): GogOwnedGame? =
    title?.takeIf { it.isNotBlank() }?.let { GogOwnedGame(id = id, title = it) }
