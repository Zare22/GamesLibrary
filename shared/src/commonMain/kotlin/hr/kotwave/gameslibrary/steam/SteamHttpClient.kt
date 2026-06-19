package hr.kotwave.gameslibrary.steam

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@OptIn(ExperimentalSerializationApi::class)
internal val SteamJson = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

/** The Ktor engine for Steam calls (OkHttp on Android + Desktop; Darwin when iOS arrives). */
internal expect fun steamEngine(): HttpClientEngine

internal fun buildSteamHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(Logging) { level = LogLevel.INFO }
    expectSuccess = false
}
