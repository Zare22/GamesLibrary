package hr.kotwave.gameslibrary.epic

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import kotlinx.serialization.json.Json

internal val EpicJson = Json {
    ignoreUnknownKeys = true
}

/** The Ktor engine for Epic calls (OkHttp on Android + Desktop; Darwin when iOS arrives). */
internal expect fun epicEngine(): HttpClientEngine

/** Epic's services expect a launcher User-Agent; requests without one can hit edge filtering. */
internal fun buildEpicHttpClient(
    engine: HttpClientEngine,
    requestTimeoutMillis: Long = 20_000,
    socketTimeoutMillis: Long = 20_000,
    connectTimeoutMillis: Long = 15_000,
): HttpClient = HttpClient(engine) {
    install(Logging) { level = LogLevel.INFO }
    install(UserAgent) { agent = EPIC_USER_AGENT }
    install(HttpTimeout) {
        this.requestTimeoutMillis = requestTimeoutMillis
        this.socketTimeoutMillis = socketTimeoutMillis
        this.connectTimeoutMillis = connectTimeoutMillis
    }
    expectSuccess = false
}

private const val EPIC_USER_AGENT =
    "UELauncher/11.0.1-14907503+++Portal+Release-Live Windows/10.0.19041.1.256.64bit"
