package hr.kotwave.gameslibrary.gog

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

/** The GOG embed API: pulls the games the signed-in user owns, authorized by their bearer token. */
class GogClient internal constructor(
    private val httpClient: HttpClient,
    private val config: GogConfig,
) {
    /** Every owned game across all pages of getFilteredProducts. Products with no title are skipped. */
    suspend fun getOwnedProducts(accessToken: String): List<GogOwnedGame> {
        val first = fetchPage(accessToken, 1)
        val games = first.products.mapNotNull { it.toOwnedGame() }.toMutableList()
        for (page in 2..first.totalPages) {
            games += fetchPage(accessToken, page).products.mapNotNull { it.toOwnedGame() }
        }
        return games
    }

    private suspend fun fetchPage(accessToken: String, page: Int): FilteredProductsResponse {
        val response = httpClient.get("${config.embedUrl}/account/getFilteredProducts") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("mediaType", 1)
            parameter("page", page)
        }
        if (!response.status.isSuccess()) {
            throw GogException("GOG getFilteredProducts failed: ${response.status}")
        }
        return GogJson.decodeFromString<FilteredProductsResponse>(response.bodyAsText())
    }
}

class GogException(message: String) : Exception(message)
