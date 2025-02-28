package hr.kotwave.gameslibrary.game

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SelectedGameViewModel(private val repository: GameRepository) : ViewModel() {
    private val _selectedGame = MutableStateFlow<Game?>(null)
    val selectedGame = _selectedGame.asStateFlow()

    fun onSelectGame(game: Game) {
        _selectedGame.value = game
    }
}
