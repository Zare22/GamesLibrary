package hr.kotwave.gameslibrary.psn

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.data.SyncEntry
import hr.kotwave.gameslibrary.data.SyncSummary
import hr.kotwave.gameslibrary.data.SyncTailRow
import hr.kotwave.gameslibrary.igdb.IgdbClient
import hr.kotwave.gameslibrary.importer.SyncReviewResult
import hr.kotwave.gameslibrary.secure.PSN_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** IGDB's external-game category for the Playstation Store; the PSN conceptId is its `uid`. */
private const val PSN_EXTERNAL_CATEGORY = 36

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
 * PSN screen state: "Connect PlayStation" opens the sign-in + npsso paste capture ([PsnConnectCapture] +
 * [PsnAuth]), persists the resulting token through [SecureStorage], then the additive sync does the
 * PSN + IGDB networking and hands resolved entries to [GameRepository.syncStore] — the merge lives
 * in `:shared`.
 */
class PsnViewModel(
    private val repository: GameRepository,
    private val psnClient: PsnClient,
    private val psnAuth: PsnAuth,
    private val igdbClient: IgdbClient,
    private val secureStorage: SecureStorage,
) : ViewModel() {

    private var token by mutableStateOf<PsnToken?>(null)

    val connected: Boolean get() = token != null

    val signInUrl: String get() = psnAuth.signInUrl()
    val npssoUrl: String get() = psnAuth.npssoUrl()

    var connectState by mutableStateOf<PsnConnectState>(PsnConnectState.Idle)
        private set

    var syncing by mutableStateOf(false)
        private set

    /** Games on PSN (purchased ∪ played) from the last sync; null until a sync has run. */
    var ownedCount by mutableStateOf<Int?>(null)
        private set

    var lastSummary by mutableStateOf<SyncSummary?>(null)
        private set

    /** The last sync's needs-review tail: id-unmatched rows the Review picker can settle. */
    var reviewTail by mutableStateOf<List<SyncTailRow>>(emptyList())
        private set

    /** What the last sync failed on, or null if it succeeded or hasn't run. */
    var syncFailure by mutableStateOf<PsnSyncFailure?>(null)
        private set

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
            } catch (e: PsnNpssoRejectedException) {
                connectState = PsnConnectState.Failed(PsnConnectFailure.Rejected)
            } catch (e: Exception) {
                connectState = PsnConnectState.Failed(PsnConnectFailure.Network)
            }
        }
    }

    fun cancelConnect() {
        connectState = PsnConnectState.Idle
    }

    fun disconnect() {
        viewModelScope.launch {
            secureStorage.remove(PSN_TOKEN_KEY)
            token = null
            ownedCount = null
            lastSummary = null
            reviewTail = emptyList()
            syncFailure = null
            connectState = PsnConnectState.Idle
        }
    }

    /** Folds a confirmed Review's merge counts into the last summary and drops its settled rows from the tail. */
    fun absorbReview(result: SyncReviewResult) {
        lastSummary = SyncSummary(
            added = (lastSummary?.added ?: 0) + result.added,
            updated = (lastSummary?.updated ?: 0) + result.updated,
        )
        reviewTail = reviewTail.filterNot { row -> row.uids.any { it in result.handledUids } }
    }

    /**
     * Pulls the purchased ∪ played games, matches them to IGDB by conceptId, and additively syncs.
     * Refreshes an expired token first; a failed refresh surfaces as a sync failure (the npsso-backed
     * refresh token dies ~every 2 months — reconnecting mints a fresh one).
     */
    fun sync() {
        if (syncing) return
        val current = token ?: return
        syncing = true
        syncFailure = null
        reviewTail = emptyList()
        viewModelScope.launch {
            var stage = PsnSyncFailure.TokenRefresh
            try {
                val fresh = ensureFreshToken(current)
                stage = PsnSyncFailure.PsnFetch
                val owned = psnClient.getOwnedGames(fresh.accessToken)
                ownedCount = owned.size
                if (owned.isEmpty()) {
                    lastSummary = SyncSummary(added = 0, updated = 0)
                    return@launch
                }
                // Sony's purchased list rarely carries a conceptId — resolve the gap per titleId,
                // skipping titles whose Game is already IGDB-matched from an earlier sync.
                val conceptless = owned.filter { it.conceptId == null }.map { it.titleId }
                val alreadyMatched = repository.psnUidsAlreadyMatched(conceptless)
                val resolved = psnClient.resolveConceptIds(fresh.accessToken, conceptless.filterNot { it in alreadyMatched })
                val library = owned.map { row ->
                    if (row.conceptId == null) row.copy(conceptId = resolved[row.titleId]) else row
                }
                stage = PsnSyncFailure.IgdbMatch
                val matched = igdbClient.matchByPsnConceptIds(library.mapNotNull { it.conceptId })
                val matchedIds = matched
                    .flatMap { game -> game.externalGames.filter { it.category == PSN_EXTERNAL_CATEGORY }.map { it.uid } }
                    .toSet()
                val rowsByGame = matched.associateWith { game ->
                    val uids = game.externalGames
                        .filter { it.category == PSN_EXTERNAL_CATEGORY }.map { it.uid }.toSet()
                    library.filter { it.conceptId in uids }
                }
                stage = PsnSyncFailure.Merge
                val split = repository.splitSyncTail(
                    Store.PSN,
                    library.filter { it.conceptId !in matchedIds }
                        .map { SyncTailRow(name = it.name, uids = listOfNotNull(it.titleId, it.conceptId)) },
                )
                val entries = buildList {
                    rowsByGame.forEach { (game, rows) ->
                        add(SyncEntry.Matched(game, rows.flatMap { listOfNotNull(it.titleId, it.conceptId) }.distinct()))
                    }
                    split.known.forEach { add(SyncEntry.Unmatched(uids = it.uids, name = it.name)) }
                }
                lastSummary = repository.syncStore(Store.PSN, entries)
                reviewTail = split.needsReview
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                syncFailure = if (e is PsnQueryOutdatedException) PsnSyncFailure.QueryOutdated else stage
            } finally {
                syncing = false
            }
        }
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
