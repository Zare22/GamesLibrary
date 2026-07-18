package hr.kotwave.gameslibrary.mirror.host

import hr.kotwave.gameslibrary.mirror.MirrorLocalStore
import hr.kotwave.gameslibrary.mirror.MirrorOutcome
import hr.kotwave.gameslibrary.mirror.MirrorSideChanges
import hr.kotwave.gameslibrary.mirror.wire.MIRROR_DEFAULT_PORT
import hr.kotwave.gameslibrary.mirror.wire.MIRROR_PORT_ATTEMPTS
import hr.kotwave.gameslibrary.mirror.wire.MIRROR_PROTOCOL_VERSION
import hr.kotwave.gameslibrary.mirror.wire.MirrorPairRequest
import hr.kotwave.gameslibrary.mirror.wire.MirrorPairResponse
import hr.kotwave.gameslibrary.mirror.wire.MirrorPairingPayload
import hr.kotwave.gameslibrary.mirror.wire.MirrorPullResponse
import hr.kotwave.gameslibrary.mirror.wire.MirrorPushRequest
import hr.kotwave.gameslibrary.mirror.wire.MirrorWireJson
import hr.kotwave.gameslibrary.mirror.wire.toDismissal
import hr.kotwave.gameslibrary.mirror.wire.toSideChanges
import hr.kotwave.gameslibrary.mirror.wire.toWire
import hr.kotwave.gameslibrary.secure.MIRROR_HOST_TOKEN_HASH_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One hosting session's coordinates; [payload] is what the hosting screen encodes into the QR. */
class MirrorHosting internal constructor(
    val port: Int,
    val fingerprint: String,
    val secret: String,
) {
    fun payload(ip: String): MirrorPairingPayload =
        MirrorPairingPayload(ip = ip, port = port, secret = secret, fingerprint = fingerprint)
}

/**
 * The desktop's Mirror host: a Netty HTTPS server behind the self-signed cert, serving pairing
 * (secret → token), snapshot pull, and the hash-guarded converged push (ADR 0037). The push applies
 * the wire's `hostChanges` through [MirrorLocalStore.apply] under the cert-fingerprint pairingId.
 */
class MirrorHost(
    private val store: MirrorLocalStore,
    private val secureStorage: SecureStorage,
    private val certificates: MirrorHostCertificates,
    private val basePort: Int = MIRROR_DEFAULT_PORT,
) {

    private var server: EmbeddedServer<*, *>? = null
    private val pushMutex = Mutex()
    private val pairingFailures = AtomicInteger(0)

    /** Binds the first free port in the [basePort]+[MIRROR_PORT_ATTEMPTS] walk and starts serving. */
    suspend fun start(): MirrorHosting {
        check(server == null) { "Mirror host is already running" }
        val identity = certificates.loadOrGenerate()
        val secret = randomPairingSecret()
        pairingFailures.set(0)
        var lastFailure: Throwable? = null
        for (candidate in basePort until basePort + MIRROR_PORT_ATTEMPTS) {
            val attempt = embeddedServer(
                Netty,
                applicationEnvironment { },
                configure = {
                    sslConnector(
                        keyStore = identity.keyStore,
                        keyAlias = MIRROR_KEY_ALIAS,
                        keyStorePassword = { identity.password.toCharArray() },
                        privateKeyPassword = { identity.password.toCharArray() },
                    ) {
                        port = candidate
                        host = "0.0.0.0"
                    }
                },
            ) { mirrorRoutes(identity.fingerprint, secret) }
            try {
                attempt.start(wait = false)
                server = attempt
                return MirrorHosting(candidate, identity.fingerprint, secret)
            } catch (failure: Exception) {
                lastFailure = failure
                attempt.stop(0, 0)
            }
        }
        throw IllegalStateException(
            "No free Mirror port in $basePort..${basePort + MIRROR_PORT_ATTEMPTS - 1}",
            lastFailure,
        )
    }

    fun stop() {
        server?.stop(100, 1_000)
        server = null
    }

    private fun Application.mirrorRoutes(fingerprint: String, secret: String) {
        routing {
            post("/pair") {
                val request = runCatching {
                    MirrorWireJson.decodeFromString(MirrorPairRequest.serializer(), call.receiveText())
                }.getOrNull()
                when {
                    request == null || request.protocolVersion != MIRROR_PROTOCOL_VERSION ->
                        call.respond(HttpStatusCode.BadRequest)

                    pairingFailures.get() >= MAX_PAIRING_FAILURES ->
                        call.respond(HttpStatusCode.Unauthorized)

                    !constantTimeEquals(request.secret, secret) -> {
                        pairingFailures.incrementAndGet()
                        call.respond(HttpStatusCode.Unauthorized)
                    }

                    else -> {
                        val token = randomHex(32)
                        secureStorage.put(MIRROR_HOST_TOKEN_HASH_KEY, sha256Hex(token))
                        call.respondJson(MirrorWireJson.encodeToString(MirrorPairResponse.serializer(), MirrorPairResponse(token = token)))
                    }
                }
            }

            get("/library") {
                if (!authorized(call.request.headers[HttpHeaders.Authorization])) {
                    return@get call.respond(HttpStatusCode.Unauthorized)
                }
                val snapshot = store.snapshot()
                val response = MirrorPullResponse(
                    snapshot = snapshot,
                    dismissals = store.dismissals().map { it.toWire() },
                    snapshotHash = snapshotContentHash(snapshot),
                )
                call.respondJson(MirrorWireJson.encodeToString(MirrorPullResponse.serializer(), response))
            }

            post("/library") {
                if (!authorized(call.request.headers[HttpHeaders.Authorization])) {
                    return@post call.respond(HttpStatusCode.Unauthorized)
                }
                val request = runCatching {
                    MirrorWireJson.decodeFromString(MirrorPushRequest.serializer(), call.receiveText())
                }.getOrNull()
                if (request == null || request.protocolVersion != MIRROR_PROTOCOL_VERSION) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }
                pushMutex.withLock {
                    if (snapshotContentHash(store.snapshot()) != request.pulledHash) {
                        return@post call.respond(HttpStatusCode.Conflict)
                    }
                    store.apply(
                        pairingId = fingerprint,
                        outcome = MirrorOutcome(
                            converged = request.converged.games,
                            mineChanges = request.hostChanges.toSideChanges(),
                            theirsChanges = MirrorSideChanges(),
                            dismissals = request.dismissals.map { it.toDismissal() },
                        ),
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }

    private suspend fun authorized(header: String?): Boolean {
        if (header == null || !header.startsWith("Bearer ")) return false
        val stored = secureStorage.get(MIRROR_HOST_TOKEN_HASH_KEY) ?: return false
        return constantTimeEquals(sha256Hex(header.removePrefix("Bearer ").trim()), stored)
    }

    private companion object {
        /** Wrong-secret attempts before pairing locks until the next hosting start. */
        const val MAX_PAIRING_FAILURES = 5
    }
}

private suspend fun ApplicationCall.respondJson(body: String) =
    respondText(body, ContentType.Application.Json)
