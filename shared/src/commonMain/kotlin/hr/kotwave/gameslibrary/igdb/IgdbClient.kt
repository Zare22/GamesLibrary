package hr.kotwave.gameslibrary.igdb

import hr.kotwave.gameslibrary.data.IgdbGame
import hr.kotwave.gameslibrary.data.IgdbSearchResult
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/** The IGDB search engine: lightweight name search, plus full metadata fetch on add. */
class IgdbClient internal constructor(
    private val httpClient: HttpClient,
    private val tokenProvider: TwitchTokenProvider,
    private val config: IgdbConfig,
) {
    suspend fun searchGames(query: String): List<IgdbSearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val body = "search \"${escape(trimmed)}\"; fields name,cover.image_id,first_release_date; limit 20;"
        return IgdbJson.decodeFromString<List<GameDto>>(post("games", body)).map { it.toSearchResult() }
    }

    suspend fun fetchGame(igdbId: Long): IgdbGame? {
        val body = "fields $FULL_FIELDS; where id = $igdbId;"
        return IgdbJson.decodeFromString<List<GameDto>>(post("games", body)).firstOrNull()?.toIgdbGame()
    }

    /** POSTs an APICalypse query, refreshing the token once on a 401. */
    private suspend fun post(endpoint: String, body: String): String {
        suspend fun attempt(token: String): HttpResponse =
            httpClient.post("${config.baseUrl}/$endpoint") {
                header("Client-ID", config.clientId)
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Text.Plain)
                setBody(body)
            }

        var response = attempt(tokenProvider.token())
        if (response.status == HttpStatusCode.Unauthorized) {
            tokenProvider.invalidate()
            response = attempt(tokenProvider.token())
        }
        if (!response.status.isSuccess()) {
            throw IgdbException("IGDB request to /$endpoint failed: ${response.status}")
        }
        return response.bodyAsText()
    }

    private fun escape(query: String): String = query.replace("\\", "\\\\").replace("\"", "\\\"")
}

class IgdbException(message: String) : Exception(message)

private const val FULL_FIELDS =
    "name,slug,first_release_date,cover.image_id," +
        "involved_companies.company.name,involved_companies.developer," +
        "platforms.name,platforms.abbreviation,total_rating,total_rating_count," +
        "external_games.uid,external_games.category,external_games.url,alternative_names.name"
