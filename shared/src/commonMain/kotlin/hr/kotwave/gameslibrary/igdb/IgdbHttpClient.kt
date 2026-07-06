package hr.kotwave.gameslibrary.igdb

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@OptIn(ExperimentalSerializationApi::class)
internal val IgdbJson = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

/** The Ktor engine for IGDB calls (OkHttp on Android + Desktop; Darwin when iOS arrives). */
internal expect fun igdbEngine(): HttpClientEngine

internal fun buildIgdbHttpClient(
    engine: HttpClientEngine,
    requestTimeoutMillis: Long = 20_000,
    socketTimeoutMillis: Long = 20_000,
    connectTimeoutMillis: Long = 15_000,
): HttpClient = HttpClient(engine) {
    install(Logging) { level = LogLevel.INFO }
    install(HttpTimeout) {
        this.requestTimeoutMillis = requestTimeoutMillis
        this.socketTimeoutMillis = socketTimeoutMillis
        this.connectTimeoutMillis = connectTimeoutMillis
    }
    expectSuccess = false
}
