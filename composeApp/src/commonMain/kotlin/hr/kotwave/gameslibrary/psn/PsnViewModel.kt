package hr.kotwave.gameslibrary.psn

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.data.sync.SyncSummary
import hr.kotwave.gameslibrary.igdb.IgdbClient
import hr.kotwave.gameslibrary.secure.PSN_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import hr.kotwave.gameslibrary.store.StoreSyncResult
import hr.kotwave.gameslibrary.store.StoreSyncViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Where the connect flow stands: idle, showing the sign-in + npsso paste capture, exchanging, or failed. */
sealed interface PsnConnectState {
    data object Idle : PsnConnectState
    data object AwaitingNpsso : PsnConnectState
    data object Exchanging : PsnConnectState
    data class Failed(val reason: PsnConnectFailure) : PsnConnectState
}

enum class PsnConnectFailure { Paste, Rejected, Network }

/** What a failed PSN sync failed on: one of the four stages, or the persisted query hash going stale. */
enum class PsnSyncFailure { TokenRefresh, PsnFetch, QueryOutdated, IgdbMatch, Merge }

/**
 * PSN's Sync slice: "Connect PlayStation" opens the sign-in + npsso paste capture ([PsnConnectCapture] +
 * [PsnAuth]) and persists the resulting token through [SecureStorage]; the shared [StoreSyncViewModel]
 * then drives the additive sync, calling [resolve] to do the PSN + IGDB networking and hand resolved
 * entries to [GameRepository.syncStore] — the merge lives in `:shared`.
 */
class PsnViewModel(
    private val repository: GameRepository,
    private val psnClient: PsnClient,
    private val psnAuth: PsnAuth,
    private val igdbClient: IgdbClient,
    private val secureStorage: SecureStorage,
) : StoreSyncViewModel<PsnSyncFailure>() {

    private var token by mutableStateOf<PsnToken?>(null)

    override val connected: Boolean get() = token != null

    val signInUrl: String get() = psnAuth.signInUrl()
    val npssoUrl: String get() = psnAuth.npssoUrl()

    var connectState by mutableStateOf<PsnConnectState>(PsnConnectState.Idle)
        private set

    override val initialStage = PsnSyncFailure.TokenRefresh

    init {
        viewModelScope.launch {
            token = secureStorage.get(PSN_TOKEN_KEY)?.let { PsnToken.decode(it) }
        }
    }

    /** Opens PSN sign-in: the capture leg shows the browser/paste UI, then calls [onNpssoCaptured]. */
    fun connect() {
        if (connectState is PsnConnectState.AwaitingNpsso || connectState is PsnConnectState.Exchanging) return
        connectState = PsnConnectState.AwaitingNpsso
    }

    /** Takes the pasted npsso (bare value or the page's whole JSON), exchanges it, and persists the token. */
    fun onNpssoCaptured(raw: String) {
        val npsso = psnAuth.extractNpsso(raw)
        if (npsso == null) {
            connectState = PsnConnectState.Failed(PsnConnectFailure.Paste)
            return
        }
        connectState = PsnConnectState.Exchanging
        viewModelScope.launch {
            try {
                persist(psnAuth.exchangeNpsso(npsso))
                connectState = PsnConnectState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (_: PsnNpssoRejectedException) {
                connectState = PsnConnectState.Failed(PsnConnectFailure.Rejected)
            } catch (_: Exception) {
                connectState = PsnConnectState.Failed(PsnConnectFailure.Network)
            }
        }
    }

    fun cancelConnect() {
        connectState = PsnConnectState.Idle
    }

    override suspend fun clearConnection() {
        secureStorage.remove(PSN_TOKEN_KEY)
        token = null
        connectState = PsnConnectState.Idle
    }

    override fun classifyFailure(error: Exception, stage: PsnSyncFailure): PsnSyncFailure =
        if (error is PsnQueryOutdatedException) PsnSyncFailure.QueryOutdated else stage

    /**
     * Pulls the purchased ∪ played games, matches them to IGDB by conceptId, and additively syncs.
     * Refreshes an expired token first; a failed refresh surfaces as a sync failure (the npsso-backed
     * refresh token dies ~every 2 months — reconnecting mints a fresh one).
     */
    override suspend fun resolve(setStage: (PsnSyncFailure) -> Unit): StoreSyncResult {
        val current = token ?: return StoreSyncResult(SyncSummary(0, 0), emptyList())
        val fresh = ensureFreshToken(current)
        setStage(PsnSyncFailure.PsnFetch)
        val owned = psnClient.getOwnedGames(fresh.accessToken)
        ownedCount = owned.size
        if (owned.isEmpty()) return StoreSyncResult(SyncSummary(0, 0), emptyList())
        // Sony's purchased list rarely carries a conceptId — resolve the gap per titleId,
        // skipping titles whose Game is already IGDB-matched from an earlier sync.
        val conceptless = conceptlessTitleIds(owned)
        val alreadyMatched = repository.uidsAlreadyMatched(Store.PSN, conceptless)
        val resolved = psnClient.resolveConceptIds(fresh.accessToken, conceptless - alreadyMatched)
        val library = withResolvedConceptIds(owned, resolved)
        setStage(PsnSyncFailure.IgdbMatch)
        val matched = igdbClient.matchByPsnConceptIds(library.mapNotNull { it.conceptId })
        setStage(PsnSyncFailure.Merge)
        val split = repository.splitSyncTail(Store.PSN, psnTailRows(library, matched))
        return StoreSyncResult(
            repository.syncStore(Store.PSN, psnEntries(matched, library, split.known)),
            split.needsReview,
        )
    }

    private suspend fun ensureFreshToken(current: PsnToken): PsnToken {
        if (!current.isExpired(Clock.System.now().epochSeconds)) return current
        return psnAuth.refresh(current.refreshToken).also { persist(it) }
    }

    private suspend fun persist(newToken: PsnToken) {
        secureStorage.put(PSN_TOKEN_KEY, newToken.encode())
        token = newToken
    }
}
