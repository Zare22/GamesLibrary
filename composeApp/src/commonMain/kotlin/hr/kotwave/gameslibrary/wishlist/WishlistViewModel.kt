package hr.kotwave.gameslibrary.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.Game
import hr.kotwave.gameslibrary.data.GameRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Wishlisted Games, for the Wishlist tab (mirrors [hr.kotwave.gameslibrary.library.LibraryViewModel]). */
class WishlistViewModel(repository: GameRepository) : ViewModel() {

    val wishlistGames: StateFlow<List<Game>> = repository.wishlistGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
