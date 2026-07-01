package hr.kotwave.gameslibrary.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.GameWithOwnerships
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class LibraryViewModel(repository: GameRepository) : ViewModel() {

    private val _filter = MutableStateFlow(LibraryFilter())
    val filter: StateFlow<LibraryFilter> = _filter.asStateFlow()

    /** Whether the library holds no owned Games at all — distinguishes "empty" from "no matches". */
    val libraryEmpty: StateFlow<Boolean> = repository.ownedGames
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Owned Games with the active search/filter/sort applied, for the grid. */
    val games: StateFlow<List<GameWithOwnerships>> =
        combine(repository.ownedGames, _filter) { games, filter -> games.filteredSorted(filter) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(query: String) = _filter.update { it.copy(query = query) }

    fun toggleStore(store: Store) = _filter.update {
        it.copy(stores = if (store in it.stores) it.stores - store else it.stores + store)
    }

    fun toggleStatus(status: Status?) = _filter.update {
        it.copy(statuses = if (status in it.statuses) it.statuses - status else it.statuses + status)
    }

    fun setSort(sort: LibrarySort) = _filter.update { it.copy(sort = sort) }

    /** Clears store/status filters and resets sort; leaves the query (the field has its own clear). */
    fun resetFilters() = _filter.update { LibraryFilter(query = it.query) }

    /** Clears search + filters + sort — the no-match empty state's escape hatch. */
    fun clearAll() = _filter.update { LibraryFilter() }
}
