package hr.kotwave.gameslibrary.gog

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import kotlinx.serialization.json.Json

/** GOG mixes snake_case (token) and camelCase (products); fields are pinned by `@SerialName`, no global strategy. */
internal val GogJson = Json {
    ignoreUnknownKeys = true
}

/** The Ktor engine for GOG calls (OkHttp on Android + Desktop; Darwin when iOS arrives). */
internal expect fun gogEngine(): HttpClientEngine

internal fun buildGogHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(Logging) { level = LogLevel.INFO }
    expectSuccess = false
}
