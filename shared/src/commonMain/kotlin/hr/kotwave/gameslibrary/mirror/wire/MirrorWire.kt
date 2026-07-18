package hr.kotwave.gameslibrary.mirror.wire

import hr.kotwave.gameslibrary.data.SyncDismissal
import hr.kotwave.gameslibrary.mirror.MirrorSideChanges
import hr.kotwave.gameslibrary.transfer.ExportedGame
import hr.kotwave.gameslibrary.transfer.LibraryExport
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The Mirror wire protocol version, carried on every envelope; a mismatch fails the session. */
const val MIRROR_PROTOCOL_VERSION = 1

/** The default host port; binding walks up through [MIRROR_PORT_ATTEMPTS] candidates when taken. */
const val MIRROR_DEFAULT_PORT = 56789

/** How many consecutive ports (starting at the base port) the host tries to bind. */
const val MIRROR_PORT_ATTEMPTS = 10

internal val MirrorWireJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** `POST /pair` body: the QR/PIN pairing secret, exchanged for a long-lived token. */
@Serializable
data class MirrorPairRequest(
    val protocolVersion: Int = MIRROR_PROTOCOL_VERSION,
    val secret: String,
)

@Serializable
data class MirrorPairResponse(
    val protocolVersion: Int = MIRROR_PROTOCOL_VERSION,
    val token: String,
)

/** `GET /library` response: the host's snapshot, its dismissals, and the hash a push must echo. */
@Serializable
data class MirrorPullResponse(
    val protocolVersion: Int = MIRROR_PROTOCOL_VERSION,
    val snapshot: LibraryExport,
    val dismissals: List<WireDismissal> = emptyList(),
    val snapshotHash: String,
)

/**
 * `POST /library` body: the converged library, the changes the host applies to reach it, and the
 * dismissal union. [pulledHash] gates the apply — the host 409s when its state changed since the pull.
 */
@Serializable
data class MirrorPushRequest(
    val protocolVersion: Int = MIRROR_PROTOCOL_VERSION,
    val pulledHash: String,
    val converged: LibraryExport,
    val hostChanges: WireSideChanges,
    val dismissals: List<WireDismissal> = emptyList(),
)

/** A [SyncDismissal] in wire form — dismissals travel outside [LibraryExport]. */
@Serializable
data class WireDismissal(val category: Int, val uid: String)

/** A [MirrorSideChanges] in wire form. */
@Serializable
data class WireSideChanges(
    val adds: List<ExportedGame> = emptyList(),
    val updates: List<ExportedGame> = emptyList(),
    val deletes: List<ExportedGame> = emptyList(),
)

fun SyncDismissal.toWire(): WireDismissal = WireDismissal(category, uid)

fun WireDismissal.toDismissal(): SyncDismissal = SyncDismissal(category, uid)

fun MirrorSideChanges.toWire(): WireSideChanges = WireSideChanges(adds, updates, deletes)

fun WireSideChanges.toSideChanges(): MirrorSideChanges = MirrorSideChanges(adds, updates, deletes)

/** The canonical fingerprint form shared by the QR payload, the pinning check, and the Baseline key. */
fun normalizeFingerprint(raw: String): String = raw.replace(":", "").trim().lowercase()
