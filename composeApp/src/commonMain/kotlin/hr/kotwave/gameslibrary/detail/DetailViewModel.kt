package hr.kotwave.gameslibrary.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.GameWithOwnerships
import hr.kotwave.gameslibrary.data.IgdbSearchResult
import hr.kotwave.gameslibrary.data.RematchResult
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.igdb.IgdbClient
import hr.kotwave.gameslibrary.navigation.Route
import hr.kotwave.gameslibrary.search.IgdbSearchState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Game detail: observes one Game with its Ownerships and applies edits, manual metadata refresh, and
 * Orphaned Re-match. A real androidx ViewModel — detail is a NavHost destination with its own
 * ViewModelStoreOwner.
 */
class DetailViewModel(
    private val repository: GameRepository,
    private val igdbClient: IgdbClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val gameId: Long = savedStateHandle.toRoute<Route.Detail>().gameId

    val game: StateFlow<GameWithOwnerships?> =
        repository.observeGame(gameId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var refreshing by mutableStateOf(false)
        private set

    /** A refresh/re-match couldn't reach IGDB (network) — distinct from an Orphaned result. */
    var igdbUnreachable by mutableStateOf(false)
        private set

    /** Re-match search overlay. */
    val rematchSearch = IgdbSearchState(igdbClient, viewModelScope)
    var rematching by mutableStateOf(false)
        private set
    var rematchConflict by mutableStateOf(false)
        private set
    var rematchPicking by mutableStateOf(false)
        private set

    fun setUserRating(rating: Double?) {
        viewModelScope.launch { repository.setUserRating(gameId, rating) }
    }

    fun setStatus(status: Status) {
        viewModelScope.launch { repository.setStatus(gameId, status) }
    }

    fun addOwnership(store: Store) {
        viewModelScope.launch { repository.addOwnership(gameId, store) }
    }

    fun removeOwnership(store: Store) {
        viewModelScope.launch { repository.removeOwnership(gameId, store) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteGame(gameId)
            onDeleted()
        }
    }

    /** Re-fetches IGDB metadata and overwrites it; a null result flags the Game Orphaned. */
    fun refresh() {
        if (refreshing) return
        val igdbId = game.value?.game?.igdbId ?: return
        refreshing = true
        igdbUnreachable = false
        viewModelScope.launch {
            try {
                repository.applyRefresh(gameId, igdbClient.fetchGame(igdbId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                igdbUnreachable = true
            } finally {
                refreshing = false
            }
        }
    }

    fun startRematch() {
        rematchConflict = false
        igdbUnreachable = false
        rematching = true
    }

    fun cancelRematch() {
        rematching = false
        rematchConflict = false
        rematchSearch.clear()
    }

    /** Repoints this Game to the picked IGDB entry. Blocks if that entry is already in the library. */
    fun pickRematch(result: IgdbSearchResult) {
        if (rematchPicking) return
        rematchSearch.cancelPending()
        rematchConflict = false
        igdbUnreachable = false
        rematchPicking = true
        viewModelScope.launch {
            try {
                val fetched = igdbClient.fetchGame(result.igdbId)
                if (fetched == null) {
                    igdbUnreachable = true
                } else {
                    when (repository.applyRematch(gameId, fetched)) {
                        is RematchResult.Success -> cancelRematch()
                        is RematchResult.AlreadyInLibrary -> rematchConflict = true
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                igdbUnreachable = true
            } finally {
                rematchPicking = false
            }
        }
    }
}
