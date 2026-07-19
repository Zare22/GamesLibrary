package hr.kotwave.gameslibrary.mirror

import hr.kotwave.gameslibrary.data.SyncDismissal
import hr.kotwave.gameslibrary.mirror.wire.MirrorPairFailure
import hr.kotwave.gameslibrary.mirror.wire.MirrorPairResponse
import hr.kotwave.gameslibrary.mirror.wire.MirrorPairingPayload
import hr.kotwave.gameslibrary.mirror.wire.MirrorPullResponse
import hr.kotwave.gameslibrary.mirror.wire.MirrorPushRequest
import hr.kotwave.gameslibrary.mirror.wire.MirrorWireJson
import hr.kotwave.gameslibrary.mirror.wire.WireDismissal
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_HOST_ENDPOINT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_HOST_FINGERPRINT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_LAST_MIRROR_AT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_NEEDS_REPAIR_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_PAIRED_AT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import hr.kotwave.gameslibrary.transfer.ExportedGame
import hr.kotwave.gameslibrary.transfer.LibraryExport
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class MirrorSessionTest {

    private val hostGame = ExportedGame(name = "Host Game", igdbId = 1, status = "BACKLOG")
    private val phoneGame = ExportedGame(name = "Phone Game", igdbId = 2, status = "PLAYING")

    private fun pullJson(
        snapshot: LibraryExport,
        hash: String,
        dismissals: List<WireDismissal> = emptyList(),
        protocolVersion: Int = 1,
    ): String = MirrorWireJson.encodeToString(
        MirrorPullResponse.serializer(),
        MirrorPullResponse(
            protocolVersion = protocolVersion,
            snapshot = snapshot,
            dismissals = dismissals,
            snapshotHash = hash,
        ),
    )

    // No HttpTimeout plugin: under runTest its real-time budget elapses instantly on the virtual clock.
    private fun testClient(engine: MockEngine, endpoint: String): MirrorClient =
        MirrorClient(HttpClient(engine) { expectSuccess = false }, "https://$endpoint")

    private fun sessionWith(
        engine: MockEngine,
        store: FakeLocalStore,
        storage: FakeSecureStorage = pairedStorage(),
    ): MirrorSession = MirrorSession(store, storage) { endpoint, _ -> testClient(engine, endpoint) }

    @Test
    fun firstMirrorPullsMergesPushesThenApplies() = runTest {
        val store = FakeLocalStore(
            current = LibraryExport(games = listOf(phoneGame)),
            storedDismissals = listOf(SyncDismissal(2, "7")),
        )
        val pushes = mutableListOf<MirrorPushRequest>()
        val engine = MockEngine { request ->
            when {
                request.method == HttpMethod.Get ->
                    respond(pullJson(LibraryExport(games = listOf(hostGame)), "hash-1", listOf(WireDismissal(1, "5"))))

                else -> {
                    pushes += MirrorWireJson.decodeFromString(
                        MirrorPushRequest.serializer(),
                        (request.body as TextContent).text,
                    )
                    respond("", HttpStatusCode.OK)
                }
            }
        }

        val result = sessionWith(engine, store).mirror { fail("No Review rows expected") }

        assertIs<MirrorSessionResult.Completed>(result)
        assertEquals(1, pushes.size)
        val push = pushes.single()
        assertEquals("hash-1", push.pulledHash)
        assertEquals(listOf("Host Game", "Phone Game"), push.converged.games.map { it.name })
        // The host already holds its own game; only the phone's is new to it.
        assertEquals(listOf("Phone Game"), push.hostChanges.adds.map { it.name })
        assertEquals(setOf(WireDismissal(1, "5"), WireDismissal(2, "7")), push.dismissals.toSet())

        val (pairingId, outcome) = store.applied.single()
        assertEquals("aabb", pairingId)
        assertEquals(listOf("Host Game"), outcome.mineChanges.adds.map { it.name })
    }

    @Test
    fun hostChangedPushRepullsAndConverges() = runTest {
        val store = FakeLocalStore(current = LibraryExport(games = listOf(phoneGame)))
        var pulls = 0
        val pushes = mutableListOf<MirrorPushRequest>()
        val engine = MockEngine { request ->
            when {
                request.method == HttpMethod.Get -> {
                    pulls++
                    if (pulls == 1) {
                        respond(pullJson(LibraryExport(), "hash-1"))
                    } else {
                        respond(pullJson(LibraryExport(games = listOf(hostGame)), "hash-2"))
                    }
                }

                else -> {
                    pushes += MirrorWireJson.decodeFromString(
                        MirrorPushRequest.serializer(),
                        (request.body as TextContent).text,
                    )
                    if (pushes.size == 1) respond("", HttpStatusCode.Conflict) else respond("", HttpStatusCode.OK)
                }
            }
        }

        val result = sessionWith(engine, store).mirror { MirrorReviewDecisions() }

        assertIs<MirrorSessionResult.Completed>(result)
        assertEquals(listOf("hash-1", "hash-2"), pushes.map { it.pulledHash })
        val (_, outcome) = store.applied.single()
        assertEquals(listOf("Host Game", "Phone Game"), outcome.converged.map { it.name })
    }

    @Test
    fun everyPushRejectedGivesUpWithoutApplying() = runTest {
        val store = FakeLocalStore(current = LibraryExport(games = listOf(phoneGame)))
        var pulls = 0
        val engine = MockEngine { request ->
            when {
                request.method == HttpMethod.Get -> {
                    pulls++
                    respond(pullJson(LibraryExport(), "hash-$pulls"))
                }

                else -> respond("", HttpStatusCode.Conflict)
            }
        }

        val result = sessionWith(engine, store).mirror { MirrorReviewDecisions() }

        assertEquals(MirrorSessionResult.HostKeptChanging, result)
        assertEquals(3, pulls)
        assertTrue(store.applied.isEmpty())
    }

    @Test
    fun unauthorizedPullThrowsNotPaired() = runTest {
        val store = FakeLocalStore()
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }

        assertFailsWith<MirrorNotPairedException> {
            sessionWith(engine, store).mirror { MirrorReviewDecisions() }
        }
        assertTrue(store.applied.isEmpty())
    }

    @Test
    fun protocolMismatchFailsTheSession() = runTest {
        val engine = MockEngine { respond(pullJson(LibraryExport(), "x", protocolVersion = 99)) }

        assertFailsWith<MirrorProtocolException> {
            sessionWith(engine, FakeLocalStore()).mirror { MirrorReviewDecisions() }
        }
    }

    @Test
    fun missingPairingThrowsBeforeAnyRequest() = runTest {
        val engine = MockEngine { fail("No request expected without a pairing") }

        assertFailsWith<MirrorNotPairedException> {
            sessionWith(engine, FakeLocalStore(), FakeSecureStorage()).mirror { MirrorReviewDecisions() }
        }
    }

    @Test
    fun reviewDecidesConflictRows() = runTest {
        val mineVersion = hostGame.copy(status = "PLAYING")
        val theirsVersion = hostGame.copy(status = "COMPLETED")
        val store = FakeLocalStore(current = LibraryExport(games = listOf(mineVersion)))
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Get) {
                respond(pullJson(LibraryExport(games = listOf(theirsVersion)), "hash-1"))
            } else {
                respond("", HttpStatusCode.OK)
            }
        }
        var reviewed: MirrorReview? = null

        val result = sessionWith(engine, store).mirror { review ->
            reviewed = review
            MirrorReviewDecisions(
                conflictDecisions = review.conflicts.map { MirrorConflictDecision(it, keepMine = false) },
            )
        }

        assertIs<MirrorSessionResult.Completed>(result)
        assertEquals(1, reviewed?.conflicts?.size)
        assertEquals(setOf(MirrorField.STATUS), reviewed?.conflicts?.single()?.fields)
        assertEquals("COMPLETED", store.applied.single().second.converged.single().status)
    }

    @Test
    fun mirrorReportsProgressInOrderWithCounts() = runTest {
        val store = FakeLocalStore(current = LibraryExport(games = listOf(phoneGame)))
        val storage = pairedStorage()
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Get) {
                respond(pullJson(LibraryExport(games = listOf(hostGame)), "hash-1"))
            } else {
                respond("", HttpStatusCode.OK)
            }
        }
        val progress = mutableListOf<MirrorProgress>()

        sessionWith(engine, store, storage).mirror(onProgress = progress::add) { MirrorReviewDecisions() }

        assertEquals(
            listOf(
                MirrorProgress.Pulling(1),
                MirrorProgress.Pulled(1),
                MirrorProgress.Compared(rowsToDecide = 0, firstMirror = true),
                MirrorProgress.Pushing,
                MirrorProgress.Applied,
            ),
            progress,
        )
        assertNotNull(storage.values[MIRROR_CLIENT_LAST_MIRROR_AT_KEY]?.toLongOrNull())
    }

    @Test
    fun rejectedPushReportsTheNextAttemptAndSkipsLastMirrorAt() = runTest {
        val store = FakeLocalStore(
            current = LibraryExport(games = listOf(phoneGame)),
            storedBaseline = LibraryExport(),
        )
        val storage = pairedStorage()
        var pulls = 0
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Get) {
                pulls++
                respond(pullJson(LibraryExport(), "hash-$pulls"))
            } else {
                respond("", HttpStatusCode.Conflict)
            }
        }
        val progress = mutableListOf<MirrorProgress>()

        val result = sessionWith(engine, store, storage).mirror(onProgress = progress::add) { MirrorReviewDecisions() }

        assertEquals(MirrorSessionResult.HostKeptChanging, result)
        assertEquals(listOf(1, 2, 3), progress.filterIsInstance<MirrorProgress.Pulling>().map { it.attempt })
        assertEquals(
            MirrorProgress.Compared(rowsToDecide = 0, firstMirror = false),
            progress.first { it is MirrorProgress.Compared },
        )
        assertTrue(progress.none { it is MirrorProgress.Applied })
        assertNull(storage.values[MIRROR_CLIENT_LAST_MIRROR_AT_KEY])
    }

    @Test
    fun cancellationDuringThePushWindowStillAppliesBothSides() = runTest {
        val store = FakeLocalStore(current = LibraryExport(games = listOf(phoneGame)))
        val storage = pairedStorage()
        val pushStarted = CompletableDeferred<Unit>()
        val releasePush = CompletableDeferred<Unit>()
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Get) {
                respond(pullJson(LibraryExport(), "hash-1"))
            } else {
                pushStarted.complete(Unit)
                releasePush.await()
                respond("", HttpStatusCode.OK)
            }
        }
        val job = launch {
            sessionWith(engine, store, storage).mirror { MirrorReviewDecisions() }
        }
        pushStarted.await()

        job.cancel()
        releasePush.complete(Unit)
        job.join()

        assertEquals(1, store.applied.size)
        assertNotNull(storage.values[MIRROR_CLIENT_LAST_MIRROR_AT_KEY])
    }

    @Test
    fun pairStoresTokenNormalizedFingerprintAndEndpoint() = runTest {
        val storage = FakeSecureStorage()
        storage.values[MIRROR_CLIENT_NEEDS_REPAIR_KEY] = "true"
        storage.values[MIRROR_CLIENT_LAST_MIRROR_AT_KEY] = "1752800000000"
        var pairedSecret: String? = null
        val engine = MockEngine { request ->
            pairedSecret = MirrorWireJson.decodeFromString(
                hr.kotwave.gameslibrary.mirror.wire.MirrorPairRequest.serializer(),
                (request.body as TextContent).text,
            ).secret
            respond(MirrorWireJson.encodeToString(MirrorPairResponse.serializer(), MirrorPairResponse(token = "tok-9")))
        }
        val session = MirrorSession(FakeLocalStore(), storage) { endpoint, fingerprint ->
            assertEquals("aabbcc", fingerprint)
            testClient(engine, endpoint)
        }

        session.pair(MirrorPairingPayload(ip = "192.168.1.10", port = 56790, secret = "123456", fingerprint = "AA:BB:CC"))

        assertEquals("123456", pairedSecret)
        assertEquals("tok-9", storage.values[MIRROR_CLIENT_TOKEN_KEY])
        assertEquals("aabbcc", storage.values[MIRROR_CLIENT_HOST_FINGERPRINT_KEY])
        assertEquals("192.168.1.10:56790", storage.values[MIRROR_CLIENT_HOST_ENDPOINT_KEY])
        assertNotNull(storage.values[MIRROR_CLIENT_PAIRED_AT_KEY]?.toLongOrNull())
        assertNull(storage.values[MIRROR_CLIENT_NEEDS_REPAIR_KEY])
        assertNull(storage.values[MIRROR_CLIENT_LAST_MIRROR_AT_KEY])
    }

    @Test
    fun unpairRemovesEveryClientKey() = runTest {
        val storage = pairedStorage()
        storage.values[MIRROR_CLIENT_PAIRED_AT_KEY] = "1752800000000"
        storage.values[MIRROR_CLIENT_NEEDS_REPAIR_KEY] = "true"
        storage.values[MIRROR_CLIENT_LAST_MIRROR_AT_KEY] = "1752810000000"
        val session = MirrorSession(FakeLocalStore(), storage) { _, _ -> fail("No request expected on unpair") }

        session.unpair()

        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun pairRejectsAForeignPayloadVersion() = runTest {
        val engine = MockEngine { fail("No request expected on a version mismatch") }
        val session = MirrorSession(FakeLocalStore(), FakeSecureStorage()) { endpoint, _ ->
            testClient(engine, endpoint)
        }

        assertFailsWith<MirrorProtocolException> {
            session.pair(MirrorPairingPayload(version = 99, ip = "1.2.3.4", port = 1, secret = "s", fingerprint = "ff"))
        }
    }

    @Test
    fun wrongSecretPairSurfacesRemainingAttempts() = runTest {
        val engine = MockEngine {
            respond(
                MirrorWireJson.encodeToString(MirrorPairFailure.serializer(), MirrorPairFailure(remainingAttempts = 2)),
                HttpStatusCode.Unauthorized,
            )
        }
        val session = MirrorSession(FakeLocalStore(), FakeSecureStorage()) { endpoint, _ ->
            testClient(engine, endpoint)
        }

        val failure = assertFailsWith<MirrorWrongSecretException> {
            session.pair(MirrorPairingPayload(ip = "1.2.3.4", port = 1, secret = "000000", fingerprint = "ff"))
        }
        assertEquals(2, failure.remainingAttempts)
    }

    @Test
    fun lockedPairThrowsPairingLocked() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.Locked) }
        val session = MirrorSession(FakeLocalStore(), FakeSecureStorage()) { endpoint, _ ->
            testClient(engine, endpoint)
        }

        assertFailsWith<MirrorPairingLockedException> {
            session.pair(MirrorPairingPayload(ip = "1.2.3.4", port = 1, secret = "000000", fingerprint = "ff"))
        }
    }
}

private fun pairedStorage() = FakeSecureStorage().apply {
    values[MIRROR_CLIENT_TOKEN_KEY] = "token-1"
    values[MIRROR_CLIENT_HOST_FINGERPRINT_KEY] = "aabb"
    values[MIRROR_CLIENT_HOST_ENDPOINT_KEY] = "127.0.0.1:56789"
}

private class FakeSecureStorage : SecureStorage {
    val values = mutableMapOf<String, String>()
    override suspend fun get(key: String): String? = values[key]
    override suspend fun put(key: String, value: String) {
        values[key] = value
    }

    override suspend fun remove(key: String) {
        values.remove(key)
    }
}

private class FakeLocalStore(
    var current: LibraryExport = LibraryExport(),
    var storedBaseline: LibraryExport? = null,
    var storedDismissals: List<SyncDismissal> = emptyList(),
) : MirrorLocalStore {
    val applied = mutableListOf<Pair<String, MirrorOutcome>>()

    override suspend fun snapshot(): LibraryExport = current
    override suspend fun dismissals(): List<SyncDismissal> = storedDismissals
    override suspend fun baseline(pairingId: String): LibraryExport? = storedBaseline
    override suspend fun apply(pairingId: String, outcome: MirrorOutcome) {
        applied += pairingId to outcome
    }
}
