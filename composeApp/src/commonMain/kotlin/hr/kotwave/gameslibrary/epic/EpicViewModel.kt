package hr.kotwave.gameslibrary.epic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.data.sync.SyncSummary
import hr.kotwave.gameslibrary.igdb.IgdbClient
import hr.kotwave.gameslibrary.secure.EPIC_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import hr.kotwave.gameslibrary.store.StoreSyncResult
import hr.kotwave.gameslibrary.store.StoreSyncViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Where the connect flow stands: idle, showing the sign-in + code paste capture, exchanging, or failed. */
sealed interface EpicConnectState {
    data object Idle : EpicConnectState
    data object AwaitingCode : EpicConnectState
    data object Exchanging : EpicConnectState
    data class Failed(val reason: EpicConnectFailure) : EpicConnectState
}

enum class EpicConnectFailure { Paste, Rejected, Network }

/** What a failed Epic sync failed on: one of the five stages. */
enum class EpicSyncFailure { TokenRefresh, EpicFetch, CatalogResolve, IgdbMatch, Merge }

/**
 * Epic's Sync slice: "Connect Epic Games" opens the sign-in + authorizationCode paste capture
 * ([EpicConnectCapture] + [EpicAuth]) and persists the resulting token through [SecureStorage]; the
 * shared [StoreSyncViewModel] then drives the additive sync, calling [resolve] to do the Epic + IGDB
 * networking and hand resolved entries to [GameRepository.syncStore] — the merge lives in `:shared`.
 * Epic rotates the refresh token on every refresh, so every [EpicAuth.refresh] result is persisted
 * before it is used.
 */
class EpicViewModel(
    private val repository: GameRepository,
    private val epicClient: EpicClient,
    private val epicAuth: EpicAuth,
    private val igdbClient: IgdbClient,
    private val secureStorage: SecureStorage,
) : StoreSyncViewModel<EpicSyncFailure>() {

    private var token by mutableStateOf<EpicToken?>(null)

    override val store = Store.EPIC

    override val connected: Boolean get() = token != null

    val signInUrl: String get() = epicAuth.signInUrl()

    var connectState by mutableStateOf<EpicConnectState>(EpicConnectState.Idle)
        private set

    override val initialStage = EpicSyncFailure.TokenRefresh

    init {
        viewModelScope.launch {
            token = secureStorage.get(EPIC_TOKEN_KEY)?.let { EpicToken.decode(it) }
        }
    }

    /** Opens Epic sign-in: the capture leg shows the browser/paste UI, then calls [onCodeCaptured]. */
    fun connect() {
        if (connectState is EpicConnectState.AwaitingCode || connectState is EpicConnectState.Exchanging) return
        connectState = EpicConnectState.AwaitingCode
    }

    /** Takes the pasted code (bare value or the page's whole JSON), exchanges it, and persists the token. */
    fun onCodeCaptured(raw: String) {
        val code = epicAuth.extractAuthorizationCode(raw)
        if (code == null) {
            connectState = EpicConnectState.Failed(EpicConnectFailure.Paste)
            return
        }
        connectState = EpicConnectState.Exchanging
        viewModelScope.launch {
            try {
                persist(epicAuth.exchangeCode(code))
                connectState = EpicConnectState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (_: EpicCodeRejectedException) {
                connectState = EpicConnectState.Failed(EpicConnectFailure.Rejected)
            } catch (_: Exception) {
                connectState = EpicConnectState.Failed(EpicConnectFailure.Network)
            }
        }
    }

    fun cancelConnect() {
        connectState = EpicConnectState.Idle
    }

    override suspend fun clearConnection() {
        secureStorage.remove(EPIC_TOKEN_KEY)
        token = null
        connectState = EpicConnectState.Idle
    }

    /**
     * Pulls the owned entitlements, resolves them per namespace (titles + store offers), matches to
     * IGDB by offerId, and additively syncs. Refreshes an expired token first; a failed refresh
     * surfaces as a sync failure (the refresh chain lives ~a year — reconnecting mints a fresh one).
     */
    override suspend fun resolve(setStage: (EpicSyncFailure) -> Unit): StoreSyncResult {
        val current = token ?: return StoreSyncResult(SyncSummary(0, 0), emptyList())
        val fresh = ensureFreshToken(current)
        setStage(EpicSyncFailure.EpicFetch)
        val items = epicClient.getLibraryItems(fresh.accessToken)
        if (items.isEmpty()) {
            ownedCount = 0
            return StoreSyncResult(SyncSummary(0, 0), emptyList())
        }
        setStage(EpicSyncFailure.CatalogResolve)
        // Namespaces whose items are all IGDB-matched from an earlier sync skip the offers leg.
        val alreadyMatched = repository.uidsAlreadyMatched(Store.EPIC, items.map { it.catalogItemId })
        val owned = epicClient.resolveOwnedGames(fresh.accessToken, items, alreadyMatched)
        ownedCount = owned.size
        setStage(EpicSyncFailure.IgdbMatch)
        val matched = igdbClient.matchByEpicOfferIds(owned.mapNotNull { it.offerId })
        setStage(EpicSyncFailure.Merge)
        val split = repository.splitSyncTail(Store.EPIC, epicTailRows(owned, matched))
        return StoreSyncResult(
            repository.syncStore(Store.EPIC, epicEntries(matched, owned, split.known)),
            split.needsReview,
        )
    }

    private suspend fun ensureFreshToken(current: EpicToken): EpicToken {
        if (!current.isExpired(Clock.System.now().epochSeconds)) return current
        // The refresh response rotates the refresh token — persist before use, always.
        return epicAuth.refresh(current.refreshToken).also { persist(it) }
    }

    private suspend fun persist(newToken: EpicToken) {
        secureStorage.put(EPIC_TOKEN_KEY, newToken.encode())
        token = newToken
    }
}
