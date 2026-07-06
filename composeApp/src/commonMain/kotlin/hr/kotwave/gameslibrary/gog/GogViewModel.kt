package hr.kotwave.gameslibrary.gog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.GogSyncEntry
import hr.kotwave.gameslibrary.data.GogSyncSummary
import hr.kotwave.gameslibrary.igdb.IgdbClient
import hr.kotwave.gameslibrary.secure.GOG_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** IGDB's external-game category for GOG; the GOG product id is its `uid`. */
private const val GOG_EXTERNAL_CATEGORY = 5

/** Where the connect flow stands: idle, showing the login capture, exchanging the code, or failed. */
sealed interface GogConnectState {
    data object Idle : GogConnectState
    data class AwaitingLogin(val authUrl: String) : GogConnectState
    data object Exchanging : GogConnectState
    data class Failed(val reason: GogConnectFailure) : GogConnectState
}

enum class GogConnectFailure { Auth, Network }

/** Which stage of a GOG sync threw; drives a stage-accurate error message. */
enum class GogSyncStage { TokenRefresh, GogFetch, IgdbMatch, Merge }

/**
 * GOG screen state: "Connect GOG" opens the OAuth2 login ([GogConnectCapture] + [GogAuth]), persists the
 * resulting token through [SecureStorage], then the additive sync does the GOG + IGDB networking and
 * hands resolved entries to [GameRepository.syncGogGames] — the merge lives in `:shared`.
 */
class GogViewModel(
    private val repository: GameRepository,
    private val gogClient: GogClient,
    private val gogAuth: GogAuth,
    private val igdbClient: IgdbClient,
    private val secureStorage: SecureStorage,
) : ViewModel() {

    private var token by mutableStateOf<GogToken?>(null)

    val connected: Boolean get() = token != null

    var connectState by mutableStateOf<GogConnectState>(GogConnectState.Idle)
        private set

    var syncing by mutableStateOf(false)
        private set

    /** Games owned on GOG from the last sync; null until a sync has run. */
    var ownedCount by mutableStateOf<Int?>(null)
        private set

    var lastSummary by mutableStateOf<GogSyncSummary?>(null)
        private set

    /** Which stage of the last sync failed, or null if it succeeded or hasn't run. */
    var syncFailure by mutableStateOf<GogSyncStage?>(null)
        private set

    init {
        viewModelScope.launch {
            token = secureStorage.get(GOG_TOKEN_KEY)?.let { GogToken.decode(it) }
        }
    }

    /** Opens GOG login: the capture leg shows the browser/paste UI, then calls [onRedirectCaptured]. */
    fun connect() {
        if (connectState is GogConnectState.AwaitingLogin || connectState is GogConnectState.Exchanging) return
        connectState = GogConnectState.AwaitingLogin(gogAuth.authUrl())
    }

    /** Takes the captured post-login redirect (full URL or pasted code), exchanges it, and persists the token. */
    fun onRedirectCaptured(raw: String) {
        val code = gogAuth.extractCode(raw)
        if (code == null) {
            connectState = GogConnectState.Failed(GogConnectFailure.Auth)
            return
        }
        connectState = GogConnectState.Exchanging
        viewModelScope.launch {
            try {
                persist(gogAuth.exchangeCode(code))
                connectState = GogConnectState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                connectState = GogConnectState.Failed(GogConnectFailure.Network)
            }
        }
    }

    fun cancelConnect() {
        connectState = GogConnectState.Idle
    }

    fun disconnect() {
        viewModelScope.launch {
            secureStorage.remove(GOG_TOKEN_KEY)
            token = null
            ownedCount = null
            lastSummary = null
            syncFailure = null
            connectState = GogConnectState.Idle
        }
    }

    /**
     * Pulls the owned games, matches them to IGDB by GOG product id, and additively syncs. Refreshes an
     * expired token first; a failed refresh surfaces as a sync failure (the token may have been revoked).
     */
    fun sync() {
        if (syncing) return
        val current = token ?: return
        syncing = true
        syncFailure = null
        viewModelScope.launch {
            var stage = GogSyncStage.TokenRefresh
            try {
                val fresh = ensureFreshToken(current)
                stage = GogSyncStage.GogFetch
                val owned = gogClient.getOwnedProducts(fresh.accessToken)
                ownedCount = owned.size
                if (owned.isEmpty()) {
                    lastSummary = GogSyncSummary(added = 0, updated = 0)
                    return@launch
                }
                stage = GogSyncStage.IgdbMatch
                val matched = igdbClient.matchByGogIds(owned.map { it.id.toString() })
                val matchedIds = matched
                    .flatMap { game -> game.externalGames.filter { it.category == GOG_EXTERNAL_CATEGORY }.map { it.uid } }
                    .toSet()
                val entries = buildList {
                    matched.forEach { add(GogSyncEntry.Matched(it)) }
                    owned.filter { it.id.toString() !in matchedIds }
                        .forEach { add(GogSyncEntry.Unmatched(it.id.toString(), it.title)) }
                }
                stage = GogSyncStage.Merge
                lastSummary = repository.syncGogGames(entries)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                syncFailure = stage
            } finally {
                syncing = false
            }
        }
    }

    private suspend fun ensureFreshToken(current: GogToken): GogToken {
        if (!current.isExpired(Clock.System.now().epochSeconds)) return current
        return gogAuth.refresh(current.refreshToken).also { persist(it) }
    }

    private suspend fun persist(newToken: GogToken) {
        secureStorage.put(GOG_TOKEN_KEY, newToken.encode())
        token = newToken
    }
}
