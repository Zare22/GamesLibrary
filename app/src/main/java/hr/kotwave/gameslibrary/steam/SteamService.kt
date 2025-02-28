package hr.kotwave.gameslibrary.steam

import hr.kotwave.gameslibrary.network.ApiResponse
import kotlinx.serialization.Serializable

interface SteamService {
    suspend fun getOwnedGames(): ApiResponse<SteamResponse>
}

@Serializable
data class SteamResponse(
    val response: SteamGamesResponse
)

@Serializable
data class SteamGamesResponse(
    val game_count: Int,
    val games: List<SteamGameDto>
)

@Serializable
data class SteamGameDto(
    val appid: Int,
    val name: String,
    val playtime_forever: Int
)