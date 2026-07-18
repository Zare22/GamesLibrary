package hr.kotwave.gameslibrary.mirror

import hr.kotwave.gameslibrary.mirror.wire.MIRROR_PROTOCOL_VERSION
import hr.kotwave.gameslibrary.mirror.wire.MirrorPairingPayload
import hr.kotwave.gameslibrary.mirror.wire.MirrorPushRequest
import hr.kotwave.gameslibrary.mirror.wire.normalizeFingerprint
import hr.kotwave.gameslibrary.mirror.wire.toDismissal
import hr.kotwave.gameslibrary.mirror.wire.toWire
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_HOST_ENDPOINT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_HOST_FINGERPRINT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_NEEDS_REPAIR_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_PAIRED_AT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import hr.kotwave.gameslibrary.transfer.LibraryExport
import kotlin.time.Clock

/** The Conflict and collision rows a Mirror Review must decide, one decision per row. */
data class MirrorReview(
    val conflicts: List<MirrorConflict>,
    val collisions: List<MirrorCollision>,
)

data class MirrorReviewDecisions(
    val conflictDecisions: List<MirrorConflictDecision> = emptyList(),
    val collisionDecisions: List<MirrorCollisionDecision> = emptyList(),
)

sealed interface MirrorSessionResult {
    /** Both sides converged; [outcome] carries what this device applied and the summary counts. */
    data class Completed(val outcome: MirrorOutcome) : MirrorSessionResult

    /** The host's state changed under every pull→push round; nothing was applied on either side. */
    data object HostKeptChanging : MirrorSessionResult
}

/**
 * The client end of a Mirror (ADR 0037): pull the host's snapshot, three-way merge over the stored
 * Baseline, Review, push the converged library, and apply locally only once the host accepted the
 * push — neither side commits alone. The pairing's cert fingerprint is the pairingId throughout.
 */
class MirrorSession(
    private val store: MirrorLocalStore,
    private val secureStorage: SecureStorage,
    private val clock: Clock = Clock.System,
    private val clientFactory: (endpoint: String, certFingerprint: String) -> MirrorClient = ::mirrorClient,
) {

    /**
     * Pairs against a scanned or typed [payload]: the pinned TLS handshake proves the cert matches
     * the payload's fingerprint, and the secret buys the long-lived token. Stores token, fingerprint,
     * endpoint, and the pairing moment; a re-pair overwrites the previous pairing.
     */
    suspend fun pair(payload: MirrorPairingPayload) {
        if (payload.version != MIRROR_PROTOCOL_VERSION) throw MirrorProtocolException(payload.version)
        val fingerprint = normalizeFingerprint(payload.fingerprint)
        val endpoint = "${payload.ip}:${payload.port}"
        val response = clientFactory(endpoint, fingerprint).pair(payload.secret)
        secureStorage.put(MIRROR_CLIENT_TOKEN_KEY, response.token)
        secureStorage.put(MIRROR_CLIENT_HOST_FINGERPRINT_KEY, fingerprint)
        secureStorage.put(MIRROR_CLIENT_HOST_ENDPOINT_KEY, endpoint)
        secureStorage.put(MIRROR_CLIENT_PAIRED_AT_KEY, clock.now().toEpochMilliseconds().toString())
        secureStorage.remove(MIRROR_CLIENT_NEEDS_REPAIR_KEY)
    }

    /** Forgets the pairing on this device; the games on both devices stay as they are. */
    suspend fun unpair() {
        secureStorage.remove(MIRROR_CLIENT_TOKEN_KEY)
        secureStorage.remove(MIRROR_CLIENT_HOST_FINGERPRINT_KEY)
        secureStorage.remove(MIRROR_CLIENT_HOST_ENDPOINT_KEY)
        secureStorage.remove(MIRROR_CLIENT_PAIRED_AT_KEY)
        secureStorage.remove(MIRROR_CLIENT_NEEDS_REPAIR_KEY)
    }

    /**
     * Runs one Mirror against the paired host. [review] is invoked only when the merge has Conflict
     * or collision rows. On a 409 the host changed mid-session: the round restarts from a fresh pull,
     * up to [MAX_PUSH_ATTEMPTS] times.
     */
    suspend fun mirror(review: suspend (MirrorReview) -> MirrorReviewDecisions): MirrorSessionResult {
        val token = secureStorage.get(MIRROR_CLIENT_TOKEN_KEY) ?: throw MirrorNotPairedException()
        val fingerprint = secureStorage.get(MIRROR_CLIENT_HOST_FINGERPRINT_KEY) ?: throw MirrorNotPairedException()
        val endpoint = secureStorage.get(MIRROR_CLIENT_HOST_ENDPOINT_KEY) ?: throw MirrorNotPairedException()
        val client = clientFactory(endpoint, fingerprint)

        repeat(MAX_PUSH_ATTEMPTS) {
            val pulled = client.pull(token)
            val merge = mirrorMerge(
                baseline = store.baseline(fingerprint),
                mine = store.snapshot(),
                theirs = pulled.snapshot,
                mineDismissals = store.dismissals().toSet(),
                theirsDismissals = pulled.dismissals.map { it.toDismissal() }.toSet(),
            )
            val decisions = if (merge.conflicts.isEmpty() && merge.collisions.isEmpty()) {
                MirrorReviewDecisions()
            } else {
                review(MirrorReview(merge.conflicts, merge.collisions))
            }
            val outcome = merge.resolve(decisions.conflictDecisions, decisions.collisionDecisions)
            val push = MirrorPushRequest(
                pulledHash = pulled.snapshotHash,
                converged = LibraryExport(games = outcome.converged),
                hostChanges = outcome.theirsChanges.toWire(),
                dismissals = outcome.dismissals.map { it.toWire() },
            )
            when (client.push(token, push)) {
                MirrorPushOutcome.Applied -> {
                    store.apply(fingerprint, outcome)
                    return MirrorSessionResult.Completed(outcome)
                }

                MirrorPushOutcome.HostChanged -> Unit
            }
        }
        return MirrorSessionResult.HostKeptChanging
    }

    private companion object {
        const val MAX_PUSH_ATTEMPTS = 3
    }
}
