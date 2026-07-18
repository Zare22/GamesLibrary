package hr.kotwave.gameslibrary.mirror

import hr.kotwave.gameslibrary.mirror.wire.MIRROR_PROTOCOL_VERSION
import hr.kotwave.gameslibrary.mirror.wire.MirrorPairRequest
import hr.kotwave.gameslibrary.mirror.wire.MirrorPairResponse
import hr.kotwave.gameslibrary.mirror.wire.MirrorPullResponse
import hr.kotwave.gameslibrary.mirror.wire.MirrorPushRequest
import hr.kotwave.gameslibrary.mirror.wire.MirrorWireJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/** The Ktor engine for Mirror calls: OkHttp trusting exactly the cert with [certFingerprint]. */
internal expect fun mirrorEngine(certFingerprint: String): HttpClientEngine

internal fun buildMirrorHttpClient(
    engine: HttpClientEngine,
    requestTimeoutMillis: Long = 30_000,
    connectTimeoutMillis: Long = 10_000,
): HttpClient = HttpClient(engine) {
    install(Logging) { level = LogLevel.INFO }
    install(HttpTimeout) {
        this.requestTimeoutMillis = requestTimeoutMillis
        this.socketTimeoutMillis = requestTimeoutMillis
        this.connectTimeoutMillis = connectTimeoutMillis
    }
    expectSuccess = false
}

/** The host rejected the credentials (401): the secret, token, or pairing is no longer valid. */
class MirrorNotPairedException : Exception("The Mirror host rejected the pairing")

/** The two ends run incompatible Mirror protocol versions. */
class MirrorProtocolException(theirs: Int) :
    Exception("Mirror protocol mismatch: theirs $theirs, ours $MIRROR_PROTOCOL_VERSION")

/** An unexpected host response. */
class MirrorTransportException(status: Int, path: String) :
    Exception("Mirror $path failed with HTTP $status")

sealed interface MirrorPushOutcome {
    /** The host verified the pulled hash, applied the changes, and persisted the Baseline. */
    data object Applied : MirrorPushOutcome

    /** The host's state changed since the pull (409) — pull again and retry. */
    data object HostChanged : MirrorPushOutcome
}

/** The client's HTTP leg against one Mirror host at `https://ip:port`. */
class MirrorClient internal constructor(private val http: HttpClient, private val baseUrl: String) {

    suspend fun pair(secret: String): MirrorPairResponse {
        val response = http.post("$baseUrl/pair") {
            contentType(ContentType.Application.Json)
            setBody(MirrorWireJson.encodeToString(MirrorPairRequest.serializer(), MirrorPairRequest(secret = secret)))
        }
        return when (response.status) {
            HttpStatusCode.OK ->
                MirrorWireJson.decodeFromString(MirrorPairResponse.serializer(), response.bodyAsText())
                    .also { checkProtocol(it.protocolVersion) }

            HttpStatusCode.Unauthorized -> throw MirrorNotPairedException()
            else -> throw MirrorTransportException(response.status.value, "/pair")
        }
    }

    suspend fun pull(token: String): MirrorPullResponse {
        val response = http.get("$baseUrl/library") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return when (response.status) {
            HttpStatusCode.OK ->
                MirrorWireJson.decodeFromString(MirrorPullResponse.serializer(), response.bodyAsText())
                    .also { checkProtocol(it.protocolVersion) }

            HttpStatusCode.Unauthorized -> throw MirrorNotPairedException()
            else -> throw MirrorTransportException(response.status.value, "/library")
        }
    }

    suspend fun push(token: String, request: MirrorPushRequest): MirrorPushOutcome {
        val response = http.post("$baseUrl/library") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(MirrorWireJson.encodeToString(MirrorPushRequest.serializer(), request))
        }
        return when (response.status) {
            HttpStatusCode.OK -> MirrorPushOutcome.Applied
            HttpStatusCode.Conflict -> MirrorPushOutcome.HostChanged
            HttpStatusCode.Unauthorized -> throw MirrorNotPairedException()
            else -> throw MirrorTransportException(response.status.value, "/library")
        }
    }

    private fun checkProtocol(theirs: Int) {
        if (theirs != MIRROR_PROTOCOL_VERSION) throw MirrorProtocolException(theirs)
    }
}

/** A [MirrorClient] over the real pinned engine. [endpoint] is `ip:port`. */
fun mirrorClient(endpoint: String, certFingerprint: String): MirrorClient =
    MirrorClient(buildMirrorHttpClient(mirrorEngine(certFingerprint)), "https://$endpoint")
