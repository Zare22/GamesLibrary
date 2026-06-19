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
import kotlinx.coroutines.launch

/** IGDB's external-game category for Steam (ADR 0014); the Steam appid is its `uid`. */
private const val STEAM_EXTERNAL_CATEGORY = 1

/**
 * Steam screen state: the connected SteamID (from [SecureStorage]) and the additive sync. Does the
 * Steam + IGDB networking, then hands resolved entries to [GameRepository.syncSteamGames] — the
 * additive merge lives in `:shared` (ADR 0015). The connect step is a temporary manual SteamID entry;
 * real OpenID sign-in replaces it next session (ADR 0016).
 */
class SteamViewModel(
    private val repository: GameRepository,
    private val steamClient: SteamClient,
    private val igdbClient: IgdbClient,
    private val secureStorage: SecureStorage,
) : ViewModel() {

    var steamId by mutableStateOf<String?>(null)
        private set

    val connected: Boolean get() = steamId != null

    var idError by mutableStateOf(false)
        private set

    var syncing by mutableStateOf(false)
        private set

    /** Games owned on Steam from the last sync; null until a sync has run. */
    var ownedCount by mutableStateOf<Int?>(null)
        private set

    var lastSummary by mutableStateOf<SteamSyncSummary?>(null)
        private set

    /** The last sync couldn't reach Steam or IGDB (network) — distinct from a private-profile result. */
    var syncFailed by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch { steamId = secureStorage.get(STEAM_ID_KEY) }
    }

    /**
     * Temporary stub for OpenID sign-in: accepts a pasted public SteamID64 (17 digits) and persists it
     * through [SecureStorage]. Real "Sign in through Steam" replaces this next session.
     */
    fun connect(rawId: String) {
        val id = rawId.trim()
        if (id.length != 17 || !id.all { it.isDigit() }) {
            idError = true
            return
        }
        idError = false
        viewModelScope.launch {
            secureStorage.put(STEAM_ID_KEY, id)
            steamId = id
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            secureStorage.remove(STEAM_ID_KEY)
            steamId = null
            ownedCount = null
            lastSummary = null
            syncFailed = false
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
        syncFailed = false
        viewModelScope.launch {
            try {
                val owned = steamClient.getOwnedGames(id)
                ownedCount = owned.size
                if (owned.isEmpty()) {
                    lastSummary = SteamSyncSummary(added = 0, updated = 0)
                    return@launch
                }
                val matched = igdbClient.matchBySteamAppids(owned.map { it.appid.toString() })
                val matchedAppids = matched
                    .flatMap { game -> game.externalGames.filter { it.category == STEAM_EXTERNAL_CATEGORY }.map { it.uid } }
                    .toSet()
                val entries = buildList {
                    matched.forEach { add(SteamSyncEntry.Matched(it)) }
                    owned.filter { it.appid.toString() !in matchedAppids }
                        .forEach { add(SteamSyncEntry.Unmatched(it.appid.toString(), it.name)) }
                }
                lastSummary = repository.syncSteamGames(entries)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                syncFailed = true
            } finally {
                syncing = false
            }
        }
    }
}
