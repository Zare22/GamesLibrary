package hr.kotwave.gameslibrary.steam

import org.koin.core.module.Module
import org.koin.dsl.module

val steamModule: Module = module {
    single { SteamConfig(apiKey = SteamCredentials.API_KEY) }
    // Each HttpClient is built inline (not a bare single) so it never collides with IGDB's HttpClient.
    single { SteamClient(buildSteamHttpClient(steamEngine()), get()) }
    single { SteamOpenId(buildSteamHttpClient(steamEngine())) }
    single { SteamBounce(buildSteamHttpClient(steamEngine())) }
}
