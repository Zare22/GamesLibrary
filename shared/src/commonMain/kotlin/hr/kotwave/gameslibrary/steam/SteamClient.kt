package hr.kotwave.gameslibrary.steam

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

/** The Steam Web API: pulls the games a public profile owns. Authorized by the developer key (ADR 0003). */
class SteamClient internal constructor(
    private val httpClient: HttpClient,
    private val config: SteamConfig,
) {
    /**
     * The games owned by [steamId]. A private (or game-details-private) profile yields an empty
     * list, not an error — that drives the privacy-empty state. Games with no name are skipped.
     */
    suspend fun getOwnedGames(steamId: String): List<SteamOwnedGame> {
        val response = httpClient.get("${config.baseUrl}/IPlayerService/GetOwnedGames/v1/") {
            parameter("key", config.apiKey)
            parameter("steamid", steamId)
            parameter("include_appinfo", 1)
            parameter("include_played_free_games", 1)
            parameter("format", "json")
        }
        if (!response.status.isSuccess()) {
            throw SteamException("Steam GetOwnedGames failed: ${response.status}")
        }
        val body = SteamJson.decodeFromString<OwnedGamesResponse>(response.bodyAsText())
        return body.response.games.orEmpty().mapNotNull { it.toOwnedGame() }
    }

    /**
     * The public persona (display name + avatar) for [steamId], or null if unavailable. Persona is
     * cosmetic, so a failed/empty response degrades to null rather than throwing.
     */
    suspend fun getPlayerSummary(steamId: String): SteamPlayerSummary? {
        val response = httpClient.get("${config.baseUrl}/ISteamUser/GetPlayerSummaries/v2/") {
            parameter("key", config.apiKey)
            parameter("steamids", steamId)
            parameter("format", "json")
        }
        if (!response.status.isSuccess()) return null
        val body = SteamJson.decodeFromString<PlayerSummariesResponse>(response.bodyAsText())
        return body.response.players.firstOrNull()?.toPlayerSummary()
    }
}

class SteamException(message: String) : Exception(message)
