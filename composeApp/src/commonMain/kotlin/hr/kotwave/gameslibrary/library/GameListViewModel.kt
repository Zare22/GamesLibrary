package hr.kotwave.gameslibrary.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.Game
import hr.kotwave.gameslibrary.data.GameRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameListViewModel(private val repository: GameRepository) : ViewModel() {

    val games: StateFlow<List<Game>> = repository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Inserts a placeholder Game to prove the Room -> UI path end to end. */
    fun addSampleGame() {
        viewModelScope.launch {
            repository.addGame("Game ${games.value.size + 1}")
        }
    }
}
