package hr.kotwave.gameslibrary.steam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.SteamSyncEntry
import hr.kotwave.gameslibrary.data.SteamSyncSummary
import hr.kotwave.gameslibrary.igdb.IgdbClient
import hr.kotwave.gameslibrary.secure.STEAM_ID_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** IGDB's external-game category for Steam; the Steam appid is its `uid`. */
private const val STEAM_EXTERNAL_CATEGORY = 1

/** Where the connect flow stands: idle, waiting on the browser, or failed (the browser leg never started over). */
sealed interface SteamConnectState {
    data object Idle : SteamConnectState
    data object Connecting : SteamConnectState
    data class Failed(val reason: SteamConnectFailure) : SteamConnectState
}

enum class SteamConnectFailure { Verification, Network }

/** Which stage of a Steam sync threw; drives a stage-accurate error message. */
enum class SteamSyncStage { SteamFetch, IgdbMatch, Merge }

/**
 * Steam screen state: "Sign in through Steam" (OpenID, [SteamAuthFlow] + [SteamOpenId]) persists a
 * verified SteamID64 through [SecureStorage], then the additive sync does the Steam + IGDB networking
 * and hands resolved entries to [GameRepository.syncSteamGames] — the merge lives in `:shared`.
 */
class SteamViewModel(
    private val repository: GameRepository,
    private val steamClient: SteamClient,
    private val igdbClient: IgdbClient,
    private val secureStorage: SecureStorage,
    private val openId: SteamOpenId,
    private val authFlow: SteamAuthFlow,
) : ViewModel() {

    var steamId by mutableStateOf<String?>(null)
        private set

    val connected: Boolean get() = steamId != null

    /** The signed-in player's display name + avatar; null until fetched (cosmetic, never blocks sign-in). */
    var persona by mutableStateOf<SteamPlayerSummary?>(null)
        private set

    var connectState by mutableStateOf<SteamConnectState>(SteamConnectState.Idle)
        private set

    var syncing by mutableStateOf(false)
        private set

    /** Games owned on Steam from the last sync; null until a sync has run. */
    var ownedCount by mutableStateOf<Int?>(null)
        private set

    var lastSummary by mutableStateOf<SteamSyncSummary?>(null)
        private set

    /** Which stage of the last sync failed, or null if it succeeded or hasn't run. */
    var syncFailure by mutableStateOf<SteamSyncStage?>(null)
        private set

    private var connectJob: Job? = null

    init {
        viewModelScope.launch {
            secureStorage.get(STEAM_ID_KEY)?.let { id ->
                steamId = id
                persona = runCatching { steamClient.getPlayerSummary(id) }.getOrNull()
            }
        }
    }

    /**
     * Runs "Sign in through Steam": opens the browser ([SteamAuthFlow]), verifies the redirect with
     * Steam ([SteamOpenId.verify]), and persists the verified SteamID64. A cancelled/timed-out browser
     * leg returns to [SteamConnectState.Idle]; a rejected assertion or network error surfaces as Failed.
     */
    fun connect() {
        if (connectState is SteamConnectState.Connecting) return
        connectState = SteamConnectState.Connecting
        connectJob = viewModelScope.launch {
            try {
                val params = authFlow.authenticate { returnTo -> openId.authUrl(returnTo) }
                if (params == null) {
                    connectState = SteamConnectState.Idle
                    return@launch
                }
                val id = openId.verify(params)
                if (id == null) {
                    connectState = SteamConnectState.Failed(SteamConnectFailure.Verification)
                    return@launch
                }
                secureStorage.put(STEAM_ID_KEY, id)
                steamId = id
                connectState = SteamConnectState.Idle
                persona = runCatching { steamClient.getPlayerSummary(id) }.getOrNull()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                connectState = SteamConnectState.Failed(SteamConnectFailure.Network)
            }
        }
    }

    fun cancelConnect() {
        connectJob?.cancel()
        connectState = SteamConnectState.Idle
    }

    fun disconnect() {
        viewModelScope.launch {
            secureStorage.remove(STEAM_ID_KEY)
            steamId = null
            persona = null
            ownedCount = null
            lastSummary = null
            syncFailure = null
            connectState = SteamConnectState.Idle
        }
    }

    /**
     * Pulls the owned games, matches them to IGDB by Steam appid, and additively syncs. A private
     * profile yields zero owned games (the privacy-empty state), not an error.
     */
    fun sync() {
        if (syncing) return
        val id = steamId ?: return
        syncing = true
        syncFailure = null
        viewModelScope.launch {
            var stage = SteamSyncStage.SteamFetch
            try {
                val owned = steamClient.getOwnedGames(id)
                ownedCount = owned.size
                if (owned.isEmpty()) {
                    lastSummary = SteamSyncSummary(added = 0, updated = 0)
                    return@launch
                }
                stage = SteamSyncStage.IgdbMatch
                val matched = igdbClient.matchBySteamAppids(owned.map { it.appid.toString() })
                val matchedAppids = matched
                    .flatMap { game -> game.externalGames.filter { it.category == STEAM_EXTERNAL_CATEGORY }.map { it.uid } }
                    .toSet()
                val entries = buildList {
                    matched.forEach { add(SteamSyncEntry.Matched(it)) }
                    owned.filter { it.appid.toString() !in matchedAppids }
                        .forEach { add(SteamSyncEntry.Unmatched(it.appid.toString(), it.name)) }
                }
                stage = SteamSyncStage.Merge
                lastSummary = repository.syncSteamGames(entries)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                syncFailure = stage
            } finally {
                syncing = false
            }
        }
    }
}
