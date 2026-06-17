package hr.kotwave.gameslibrary.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.GameWithOwnerships
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(repository: GameRepository) : ViewModel() {

    val ownedGames: StateFlow<List<GameWithOwnerships>> = repository.ownedGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
