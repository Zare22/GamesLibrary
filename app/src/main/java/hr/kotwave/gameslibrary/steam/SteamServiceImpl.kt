package hr.kotwave.gameslibrary.steam

import hr.kotwave.gameslibrary.BuildConfig
import hr.kotwave.gameslibrary.network.ApiResponse
import hr.kotwave.gameslibrary.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.http.path

class SteamServiceImpl(private val client: HttpClient) : SteamService {
    override suspend fun getOwnedGames(): ApiResponse<SteamResponse> = safeApiCall {
        client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.steampowered.com"
                path("IPlayerService/GetOwnedGames/v0001/")
                parameters.append("key", BuildConfig.STEAM_API_KEY)
                parameters.append("steamid", BuildConfig.STEAM_ID)
                parameters.append("include_appinfo", "1")
                parameters.append("format", "json")
            }
        }.body<SteamResponse>()
    }
}