package hr.kotwave.gameslibrary.epic

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
import hr.kotwave.gameslibrary.secure.EPIC_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** IGDB's external-game category for the Epic Games Store; the Epic store offerId is its `uid`. */
private const val EPIC_EXTERNAL_CATEGORY = 26

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
 * Epic screen state: "Connect Epic Games" opens the sign-in + authorizationCode paste capture
 * ([EpicConnectCapture] + [EpicAuth]), persists the resulting token through [SecureStorage], then the
 * additive sync does the Epic + IGDB networking and hands resolved entries to
 * [GameRepository.syncStore] — the merge lives in `:shared`. Epic rotates the refresh token on
 * every refresh, so every [EpicAuth.refresh] result is persisted before it is used.
 */
class EpicViewModel(
    private val repository: GameRepository,
    private val epicClient: EpicClient,
    private val epicAuth: EpicAuth,
    private val igdbClient: IgdbClient,
    private val secureStorage: SecureStorage,
) : ViewModel() {

    private var token by mutableStateOf<EpicToken?>(null)

    val connected: Boolean get() = token != null

    val signInUrl: String get() = epicAuth.signInUrl()

    var connectState by mutableStateOf<EpicConnectState>(EpicConnectState.Idle)
        private set

    var syncing by mutableStateOf(false)
        private set

    /** Games on Epic (after the DLC/companion filters) from the last sync; null until a sync has run. */
    var ownedCount by mutableStateOf<Int?>(null)
        private set

    var lastSummary by mutableStateOf<SyncSummary?>(null)
        private set

    /** The last sync's needs-review tail: id-unmatched rows the Review picker can settle. */
    var reviewTail by mutableStateOf<List<SyncTailRow>>(emptyList())
        private set

    /** What the last sync failed on, or null if it succeeded or hasn't run. */
    var syncFailure by mutableStateOf<EpicSyncFailure?>(null)
        private set

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

    fun disconnect() {
        viewModelScope.launch {
            secureStorage.remove(EPIC_TOKEN_KEY)
            token = null
            ownedCount = null
            lastSummary = null
            reviewTail = emptyList()
            syncFailure = null
            connectState = EpicConnectState.Idle
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
     * Pulls the owned entitlements, resolves them per namespace (titles + store offers), matches to
     * IGDB by offerId, and additively syncs. Refreshes an expired token first; a failed refresh
     * surfaces as a sync failure (the refresh chain lives ~a year — reconnecting mints a fresh one).
     */
    fun sync() {
        if (syncing) return
        val current = token ?: return
        syncing = true
        syncFailure = null
        reviewTail = emptyList()
        viewModelScope.launch {
            var stage = EpicSyncFailure.TokenRefresh
            try {
                val fresh = ensureFreshToken(current)
                stage = EpicSyncFailure.EpicFetch
                val items = epicClient.getLibraryItems(fresh.accessToken)
                if (items.isEmpty()) {
                    ownedCount = 0
                    lastSummary = SyncSummary(added = 0, updated = 0)
                    return@launch
                }
                stage = EpicSyncFailure.CatalogResolve
                // Namespaces whose items are all IGDB-matched from an earlier sync skip the offers leg.
                val alreadyMatched = repository.epicUidsAlreadyMatched(items.map { it.catalogItemId })
                val owned = epicClient.resolveOwnedGames(fresh.accessToken, items, alreadyMatched)
                ownedCount = owned.size
                stage = EpicSyncFailure.IgdbMatch
                val matched = igdbClient.matchByEpicOfferIds(owned.mapNotNull { it.offerId })
                val matchedIds = matched
                    .flatMap { game -> game.externalGames.filter { it.category == EPIC_EXTERNAL_CATEGORY }.map { it.uid } }
                    .toSet()
                val rowsByGame = matched.associateWith { game ->
                    val uids = game.externalGames
                        .filter { it.category == EPIC_EXTERNAL_CATEGORY }.map { it.uid }.toSet()
                    owned.filter { it.offerId in uids }
                }
                stage = EpicSyncFailure.Merge
                val split = repository.splitSyncTail(
                    Store.EPIC,
                    owned.filter { it.offerId !in matchedIds }
                        .map { SyncTailRow(name = it.title, uids = it.uids) },
                )
                val entries = buildList {
                    rowsByGame.forEach { (game, rows) ->
                        add(SyncEntry.Matched(game, rows.flatMap { it.uids }.distinct()))
                    }
                    split.known.forEach { add(SyncEntry.Unmatched(uids = it.uids, name = it.name)) }
                }
                lastSummary = repository.syncStore(Store.EPIC, entries)
                reviewTail = split.needsReview
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                syncFailure = stage
            } finally {
                syncing = false
            }
        }
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
