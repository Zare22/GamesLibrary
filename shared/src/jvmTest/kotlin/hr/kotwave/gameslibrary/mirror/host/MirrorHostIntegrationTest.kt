package hr.kotwave.gameslibrary.mirror.host

import androidx.room.Room
import hr.kotwave.gameslibrary.data.GameDao
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.GamesLibraryDatabase
import hr.kotwave.gameslibrary.data.SyncDismissal
import hr.kotwave.gameslibrary.data.buildGamesLibraryDatabase
import hr.kotwave.gameslibrary.mirror.MirrorNotPairedException
import hr.kotwave.gameslibrary.mirror.MirrorOutcome
import hr.kotwave.gameslibrary.mirror.MirrorPairingLockedException
import hr.kotwave.gameslibrary.mirror.MirrorPushOutcome
import hr.kotwave.gameslibrary.mirror.MirrorReviewDecisions
import hr.kotwave.gameslibrary.mirror.MirrorSession
import hr.kotwave.gameslibrary.mirror.MirrorSessionResult
import hr.kotwave.gameslibrary.mirror.MirrorWrongSecretException
import hr.kotwave.gameslibrary.mirror.RepositoryMirrorStore
import hr.kotwave.gameslibrary.mirror.fetchMirrorCertFingerprint
import hr.kotwave.gameslibrary.mirror.mirrorClient
import hr.kotwave.gameslibrary.mirror.mirrorMerge
import hr.kotwave.gameslibrary.mirror.wire.MIRROR_PORT_ATTEMPTS
import hr.kotwave.gameslibrary.mirror.wire.MirrorPullResponse
import hr.kotwave.gameslibrary.mirror.wire.MirrorPushRequest
import hr.kotwave.gameslibrary.mirror.wire.toWire
import hr.kotwave.gameslibrary.transfer.LibraryExport
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.nio.file.Files
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class MirrorHostIntegrationTest {

    private lateinit var workDir: File
    private lateinit var hostDb: GamesLibraryDatabase
    private lateinit var hostDao: GameDao
    private lateinit var hostRepo: GameRepository
    private lateinit var phoneDb: GamesLibraryDatabase
    private lateinit var phoneDao: GameDao
    private lateinit var phoneRepo: GameRepository
    private lateinit var hostStorage: MapSecureStorage
    private lateinit var phoneStorage: MapSecureStorage
    private lateinit var host: MirrorHost
    private var basePort: Int = 0

    @BeforeTest
    fun setUp() {
        workDir = Files.createTempDirectory("mirror_host_test").toFile()
        hostDb = Room.databaseBuilder<GamesLibraryDatabase>(name = File(workDir, "host.db").absolutePath)
            .buildGamesLibraryDatabase()
        hostDao = hostDb.gameDao()
        hostRepo = GameRepository(hostDao)
        phoneDb = Room.databaseBuilder<GamesLibraryDatabase>(name = File(workDir, "phone.db").absolutePath)
            .buildGamesLibraryDatabase()
        phoneDao = phoneDb.gameDao()
        phoneRepo = GameRepository(phoneDao)
        hostStorage = MapSecureStorage()
        phoneStorage = MapSecureStorage()
        basePort = ServerSocket(0).use { it.localPort }
        host = MirrorHost(
            store = RepositoryMirrorStore(hostRepo),
            secureStorage = hostStorage,
            certificates = MirrorHostCertificates(hostStorage, File(workDir, MIRROR_KEYSTORE_FILE_NAME)),
            basePort = basePort,
        )
    }

    @AfterTest
    fun tearDown() {
        host.stop()
        hostDb.close()
        phoneDb.close()
        workDir.deleteRecursively()
    }

    @Test
    fun pairedSessionCompletesPullMergePushAndBothSidesConverge() = runBlocking {
        hostRepo.addOwnedGame("Host Game", igdbId = 1L)
        hostDao.insertSyncDismissals(listOf(SyncDismissal(1, "555")))
        phoneRepo.addOwnedGame("Phone Game", igdbId = 2L)

        val hosting = host.start()
        val session = MirrorSession(RepositoryMirrorStore(phoneRepo), phoneStorage)
        session.pair(hosting.payload("127.0.0.1"))
        val result = session.mirror { fail("No Review rows expected on an additive first Mirror") }

        assertIs<MirrorSessionResult.Completed>(result)
        val hostGames = hostRepo.exportSnapshot().games
        val phoneGames = phoneRepo.exportSnapshot().games
        assertEquals(listOf("Host Game", "Phone Game"), hostGames.map { it.name }.sorted())
        assertEquals(hostGames.map { it.name }.sorted(), phoneGames.map { it.name }.sorted())
        assertNotNull(hostRepo.mirrorBaseline(hosting.fingerprint))
        assertNotNull(phoneRepo.mirrorBaseline(hosting.fingerprint))
        assertEquals(listOf("555"), phoneDao.dismissedSyncUids(1, listOf("555")))
    }

    @Test
    fun secondMirrorPropagatesEditsAndDeletesOverTheBaseline() = runBlocking {
        hostRepo.addOwnedGame("Shared", igdbId = 1L)
        hostRepo.addOwnedGame("Doomed", igdbId = 2L)
        phoneRepo.addOwnedGame("Shared", igdbId = 1L)
        phoneRepo.addOwnedGame("Doomed", igdbId = 2L)

        val hosting = host.start()
        val session = MirrorSession(RepositoryMirrorStore(phoneRepo), phoneStorage)
        session.pair(hosting.payload("127.0.0.1"))
        assertIs<MirrorSessionResult.Completed>(session.mirror { MirrorReviewDecisions() })

        // Since the Baseline: the phone deletes one game untouched on the host.
        phoneDao.deleteGame(phoneDao.allGamesWithOwnerships().first { it.game.igdbId == 2L }.game.id)

        val second = session.mirror { fail("A clean delete propagates without Review") }

        assertIs<MirrorSessionResult.Completed>(second)
        assertEquals(listOf("Shared"), hostRepo.exportSnapshot().games.map { it.name })
        assertEquals(listOf("Shared"), phoneRepo.exportSnapshot().games.map { it.name })
    }

    @Test
    fun wrongSecretIsRejectedWithRemainingAttempts(): Unit = runBlocking {
        val hosting = host.start()
        val client = mirrorClient("127.0.0.1:${hosting.port}", hosting.fingerprint)

        val failure = assertFailsWith<MirrorWrongSecretException> { client.pair("000000") }
        assertEquals(4, failure.remainingAttempts)
    }

    @Test
    fun pairingLocksAfterRepeatedFailures(): Unit = runBlocking {
        val hosting = host.start()
        val client = mirrorClient("127.0.0.1:${hosting.port}", hosting.fingerprint)

        repeat(4) { attempt ->
            val failure = assertFailsWith<MirrorWrongSecretException> { client.pair("000000") }
            assertEquals(4 - attempt, failure.remainingAttempts)
        }
        assertFailsWith<MirrorPairingLockedException> { client.pair("000000") }
        assertFailsWith<MirrorPairingLockedException> { client.pair(hosting.secret) }
    }

    @Test
    fun hostEmitsPairedPulledAppliedAndLockedEvents(): Unit = runBlocking {
        hostRepo.addOwnedGame("Host Game", igdbId = 1L)
        phoneRepo.addOwnedGame("Phone Game", igdbId = 2L)

        val events = mutableListOf<MirrorHostEvent>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) { host.events.collect { events.add(it) } }
        val hosting = host.start()
        val session = MirrorSession(RepositoryMirrorStore(phoneRepo), phoneStorage)
        session.pair(hosting.payload("127.0.0.1"))
        assertIs<MirrorSessionResult.Completed>(session.mirror { MirrorReviewDecisions() })

        withTimeout(5_000) { while (events.size < 3) yield() }
        assertEquals(
            listOf(
                MirrorHostEvent.Paired,
                MirrorHostEvent.Pulled,
                MirrorHostEvent.Applied(added = 1, updated = 0, removed = 0),
            ),
            events,
        )

        val client = mirrorClient("127.0.0.1:${hosting.port}", hosting.fingerprint)
        repeat(5) { runCatching { client.pair("000000") } }
        withTimeout(5_000) { while (events.lastOrNull() != MirrorHostEvent.PairingLocked) yield() }
        assertEquals(1, events.count { it == MirrorHostEvent.PairingLocked })
        collector.cancel()
    }

    @Test
    fun fetchCertReturnsTheHostingFingerprintWithoutPairing(): Unit = runBlocking {
        val hosting = host.start()

        assertEquals(hosting.fingerprint, fetchMirrorCertFingerprint("127.0.0.1", hosting.port))
    }

    @Test
    fun fetchCertThrowsWhenNothingListens(): Unit = runBlocking {
        assertFailsWith<IOException> { fetchMirrorCertFingerprint("127.0.0.1", basePort, timeoutMillis = 3_000) }
    }

    @Test
    fun unpairedTokenIsRejected(): Unit = runBlocking {
        val hosting = host.start()
        val client = mirrorClient("127.0.0.1:${hosting.port}", hosting.fingerprint)

        assertFailsWith<MirrorNotPairedException> { client.pull("bogus-token") }
    }

    @Test
    fun wrongPinnedFingerprintFailsTheHandshake() = runBlocking {
        val hosting = host.start()
        val foreignDir = File(workDir, "foreign").apply { mkdirs() }
        val foreign = MirrorHostCertificates(MapSecureStorage(), File(foreignDir, MIRROR_KEYSTORE_FILE_NAME))
            .loadOrGenerate()
        val client = mirrorClient("127.0.0.1:${hosting.port}", foreign.fingerprint)

        val failure = runCatching { client.pair(hosting.secret) }.exceptionOrNull()

        assertNotNull(failure, "Expected the pinned handshake to fail")
        val chain = generateSequence(failure) { it.cause }.toList()
        assertTrue(
            chain.any { it is SSLException || it is CertificateException },
            "Expected a TLS pinning failure, got $failure",
        )
    }

    @Test
    fun hashGuardAbortsAStalePushAndARetryConverges() = runBlocking {
        hostRepo.addOwnedGame("Host Game", igdbId = 1L)
        phoneRepo.addOwnedGame("Phone Game", igdbId = 2L)

        val hosting = host.start()
        val client = mirrorClient("127.0.0.1:${hosting.port}", hosting.fingerprint)
        val token = client.pair(hosting.secret).token

        val stalePull = client.pull(token)
        // The host state moves between the pull and the push.
        hostRepo.addOwnedGame("Sneaky", igdbId = 99L)

        val (stalePush, _) = buildPush(stalePull)
        assertEquals(MirrorPushOutcome.HostChanged, client.push(token, stalePush))
        assertEquals(
            listOf("Host Game", "Sneaky"),
            hostRepo.exportSnapshot().games.map { it.name }.sorted(),
        )

        val freshPull = client.pull(token)
        val (freshPush, outcome) = buildPush(freshPull)
        assertEquals(MirrorPushOutcome.Applied, client.push(token, freshPush))
        phoneRepo.applyMirrorMerge(hosting.fingerprint, outcome)

        val expected = listOf("Host Game", "Phone Game", "Sneaky")
        assertEquals(expected, hostRepo.exportSnapshot().games.map { it.name }.sorted())
        assertEquals(expected, phoneRepo.exportSnapshot().games.map { it.name }.sorted())
    }

    @Test
    fun occupiedBasePortWalksToALaterOne(): Unit = runBlocking {
        ServerSocket(basePort).use {
            val hosting = host.start()
            // Neighbouring ephemeral ports may also be taken; the walk lands anywhere past the base.
            assertTrue(hosting.port in (basePort + 1) until basePort + MIRROR_PORT_ATTEMPTS)
        }
    }

    private suspend fun buildPush(pulled: MirrorPullResponse): Pair<MirrorPushRequest, MirrorOutcome> {
        val outcome = mirrorMerge(
            baseline = null,
            mine = phoneRepo.exportSnapshot(),
            theirs = pulled.snapshot,
        ).resolve()
        return MirrorPushRequest(
            pulledHash = pulled.snapshotHash,
            converged = LibraryExport(games = outcome.converged),
            hostChanges = outcome.theirsChanges.toWire(),
            dismissals = outcome.dismissals.map { it.toWire() },
        ) to outcome
    }
}
