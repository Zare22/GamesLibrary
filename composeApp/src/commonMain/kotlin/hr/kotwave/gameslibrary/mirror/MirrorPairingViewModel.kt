package hr.kotwave.gameslibrary.mirror

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.mirror.wire.MIRROR_PROTOCOL_VERSION
import hr.kotwave.gameslibrary.mirror.wire.MirrorPairingPayload
import hr.kotwave.gameslibrary.mirror.wire.mirrorVerifyCode
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_HOST_ENDPOINT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_NEEDS_REPAIR_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_PAIRED_AT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

sealed interface PairingStep {
    data object Scan : PairingStep
    data object Typed : PairingStep

    /** The typed path's fetched-but-unpinned cert, waiting for the user's verify-code confirm. */
    data class Verify(val ip: String, val port: Int, val pin: String, val fingerprint: String) : PairingStep {
        val verifyCode: String get() = mirrorVerifyCode(fingerprint)
    }

    data class Paired(val endpoint: String, val replacedPrevious: Boolean) : PairingStep
}

sealed interface PairingError {
    data class WrongPin(val remainingAttempts: Int) : PairingError
    data object Locked : PairingError
    data class Unreachable(val endpoint: String) : PairingError
    data object NotPairingCode : PairingError
    data object VersionMismatch : PairingError
}

/** The Settings › Mirror card's state: paired coordinates, or a pair-again ask after a host reset. */
data class PairedState(val endpoint: String, val pairedAtLabel: String?, val needsRepair: Boolean)

class MirrorPairingViewModel(
    private val session: MirrorSession,
    private val secureStorage: SecureStorage,
    private val fetchCert: suspend (ip: String, port: Int) -> String = { ip, port ->
        fetchMirrorCertFingerprint(ip, port)
    },
) : ViewModel() {

    var step by mutableStateOf<PairingStep>(PairingStep.Scan)
        private set
    var error by mutableStateOf<PairingError?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var pairedState by mutableStateOf<PairedState?>(null)
        private set

    fun refreshPairedState() {
        viewModelScope.launch {
            val endpoint = secureStorage.get(MIRROR_CLIENT_HOST_ENDPOINT_KEY)
            pairedState = if (endpoint == null || secureStorage.get(MIRROR_CLIENT_TOKEN_KEY) == null) {
                null
            } else {
                PairedState(
                    endpoint = endpoint,
                    pairedAtLabel = secureStorage.get(MIRROR_CLIENT_PAIRED_AT_KEY)?.toLongOrNull()?.let(::pairedAtLabel),
                    needsRepair = secureStorage.get(MIRROR_CLIENT_NEEDS_REPAIR_KEY) != null,
                )
            }
        }
    }

    fun onQrScanned(text: String) {
        if (busy || step != PairingStep.Scan || error != null) return
        val payload = MirrorPairingPayload.decode(text)
        when {
            payload == null -> error = PairingError.NotPairingCode
            payload.version != MIRROR_PROTOCOL_VERSION -> error = PairingError.VersionMismatch
            else -> pair(payload)
        }
    }

    fun startTyped() {
        step = PairingStep.Typed
        error = null
    }

    fun backToScan() {
        step = PairingStep.Scan
        error = null
    }

    fun dismissError() {
        error = null
    }

    /** The typed path's pre-flight: fetch the host cert unpinned so the verify screen can show its code. */
    fun onTypedConnect(ip: String, port: Int, pin: String) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                step = PairingStep.Verify(ip, port, pin, fetchCert(ip, port))
            } catch (_: Exception) {
                error = PairingError.Unreachable("$ip:$port")
            } finally {
                busy = false
            }
        }
    }

    fun onVerifyConfirmed() {
        val verify = step as? PairingStep.Verify ?: return
        pair(
            MirrorPairingPayload(
                ip = verify.ip,
                port = verify.port,
                secret = verify.pin,
                fingerprint = verify.fingerprint,
            ),
        )
    }

    fun onVerifyRejected() {
        step = PairingStep.Typed
        error = null
    }

    private fun pair(payload: MirrorPairingPayload) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            val endpoint = "${payload.ip}:${payload.port}"
            try {
                val replaced = secureStorage.get(MIRROR_CLIENT_TOKEN_KEY) != null
                session.pair(payload)
                step = PairingStep.Paired(endpoint, replacedPrevious = replaced)
            } catch (failure: Exception) {
                error = failure.asPairingError(endpoint)
            } finally {
                busy = false
            }
        }
    }

    fun unpair() {
        viewModelScope.launch {
            session.unpair()
            refreshPairedState()
        }
    }
}

internal fun Exception.asPairingError(endpoint: String): PairingError = when (this) {
    is MirrorWrongSecretException -> PairingError.WrongPin(remainingAttempts)
    is MirrorPairingLockedException -> PairingError.Locked
    is MirrorProtocolException -> PairingError.VersionMismatch
    else -> PairingError.Unreachable(endpoint)
}

/** "18 Jul" — the paired card's date half. */
internal fun pairedAtLabel(epochMillis: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "${date.day} $month"
}
