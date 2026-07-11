package hr.kotwave.gameslibrary.psn

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import kotlinx.serialization.json.Json

internal val PsnJson = Json {
    ignoreUnknownKeys = true
}

/** The Ktor engine for PSN calls (OkHttp on Android + Desktop; Darwin when iOS arrives). */
internal expect fun psnEngine(): HttpClientEngine

/** [followRedirects] false keeps the authorize 302 unfollowed so its Location (a mobile deep link) can be read. */
internal fun buildPsnHttpClient(
    engine: HttpClientEngine,
    requestTimeoutMillis: Long = 20_000,
    socketTimeoutMillis: Long = 20_000,
    connectTimeoutMillis: Long = 15_000,
    followRedirects: Boolean = true,
): HttpClient = HttpClient(engine) {
    this.followRedirects = followRedirects
    install(Logging) { level = LogLevel.INFO }
    install(HttpTimeout) {
        this.requestTimeoutMillis = requestTimeoutMillis
        this.socketTimeoutMillis = socketTimeoutMillis
        this.connectTimeoutMillis = connectTimeoutMillis
    }
    expectSuccess = false
}
