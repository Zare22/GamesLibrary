package hr.kotwave.gameslibrary.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameListViewModel(private val repository: GameRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allGames: Flow<List<Game>> = repository.allGames

    val filteredGames: Flow<List<Game>> = allGames
        .combine(_searchQuery) { games, query ->
            if (query.isBlank()) games
            else games.filter { it.name.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshGames() {
        viewModelScope.launch {
            repository.refreshSteamGames()
        }
    }
}