package hr.kotwave.gameslibrary.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.Game
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.igdb.IgdbClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Settings-screen state: the Orphaned games and the bulk re-match (retry-fetch) over them. */
class SettingsViewModel(
    private val repository: GameRepository,
    private val igdbClient: IgdbClient,
) : ViewModel() {

    val orphanedGames: StateFlow<List<Game>> = repository.orphanedGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var retrying by mutableStateOf(false)
        private set

    /**
     * Re-attempts the IGDB fetch for every Orphaned Game by its existing `igdb_id`. Ones that resolve
     * again are refreshed and un-flagged; the rest stay Orphaned for individual Re-match.
     */
    fun retryAllOrphaned() {
        if (retrying) return
        val games = orphanedGames.value.filter { it.igdbId != null }
        if (games.isEmpty()) return
        retrying = true
        viewModelScope.launch {
            try {
                games.forEach { game ->
                    val igdbId = game.igdbId ?: return@forEach
                    try {
                        repository.applyRefresh(game.id, igdbClient.fetchGame(igdbId))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Leave this one Orphaned and carry on with the rest.
                    }
                }
            } finally {
                retrying = false
            }
        }
    }
}
