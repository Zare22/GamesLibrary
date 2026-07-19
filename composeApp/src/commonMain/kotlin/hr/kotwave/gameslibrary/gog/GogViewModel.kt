package hr.kotwave.gameslibrary.gog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.data.sync.SyncEntry
import hr.kotwave.gameslibrary.data.sync.SyncSummary
import hr.kotwave.gameslibrary.data.sync.SyncTailRow
import hr.kotwave.gameslibrary.igdb.IgdbClient
import hr.kotwave.gameslibrary.secure.GOG_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import hr.kotwave.gameslibrary.store.StoreSyncResult
import hr.kotwave.gameslibrary.store.StoreSyncViewModel
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
 * GOG's Sync slice: "Connect GOG" opens the OAuth2 login ([GogConnectCapture] + [GogAuth]) and persists
 * the resulting token through [SecureStorage]; the shared [StoreSyncViewModel] then drives the additive
 * sync, calling [resolve] to do the GOG + IGDB networking and hand resolved entries to
 * [GameRepository.syncStore] — the merge lives in `:shared`.
 */
class GogViewModel(
    private val repository: GameRepository,
    private val gogClient: GogClient,
    private val gogAuth: GogAuth,
    private val igdbClient: IgdbClient,
    private val secureStorage: SecureStorage,
) : StoreSyncViewModel<GogSyncStage>() {

    private var token by mutableStateOf<GogToken?>(null)

    override val connected: Boolean get() = token != null

    var connectState by mutableStateOf<GogConnectState>(GogConnectState.Idle)
        private set

    override val initialStage = GogSyncStage.TokenRefresh

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
            } catch (_: Exception) {
                connectState = GogConnectState.Failed(GogConnectFailure.Network)
            }
        }
    }

    fun cancelConnect() {
        connectState = GogConnectState.Idle
    }

    override suspend fun clearConnection() {
        secureStorage.remove(GOG_TOKEN_KEY)
        token = null
        connectState = GogConnectState.Idle
    }

    /**
     * Pulls the owned games, matches them to IGDB by GOG product id, and additively syncs. Refreshes an
     * expired token first; a failed refresh surfaces as a sync failure (the token may have been revoked).
     */
    override suspend fun resolve(setStage: (GogSyncStage) -> Unit): StoreSyncResult {
        val current = token ?: return StoreSyncResult(SyncSummary(0, 0), emptyList())
        val fresh = ensureFreshToken(current)
        setStage(GogSyncStage.GogFetch)
        val owned = gogClient.getOwnedProducts(fresh.accessToken)
        ownedCount = owned.size
        if (owned.isEmpty()) return StoreSyncResult(SyncSummary(0, 0), emptyList())
        setStage(GogSyncStage.IgdbMatch)
        val matched = igdbClient.matchByGogIds(owned.map { it.id.toString() })
        val matchedIds = matched
            .flatMap { game -> game.externalGames.filter { it.category == GOG_EXTERNAL_CATEGORY }.map { it.uid } }
            .toSet()
        setStage(GogSyncStage.Merge)
        val split = repository.splitSyncTail(
            Store.GOG,
            owned.filter { it.id.toString() !in matchedIds }
                .map { SyncTailRow(name = it.title, uids = listOf(it.id.toString())) },
        )
        val entries = buildList {
            matched.forEach { add(SyncEntry.Matched(it)) }
            split.known.forEach { add(SyncEntry.Unmatched(uids = it.uids, name = it.name)) }
        }
        return StoreSyncResult(repository.syncStore(Store.GOG, entries), split.needsReview)
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
