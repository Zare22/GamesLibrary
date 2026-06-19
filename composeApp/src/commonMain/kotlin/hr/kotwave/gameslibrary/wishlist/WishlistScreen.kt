package hr.kotwave.gameslibrary.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.ui.components.GameTile
import hr.kotwave.gameslibrary.ui.shell.LocalIsCompact
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

/** Wishlist tab: the same cover grid as the Library, filtered to Wishlisted Games (no store/status). */
@Composable
fun WishlistScreen(
    onOpenGame: (Long) -> Unit,
    viewModel: WishlistViewModel = koinViewModel(),
) {
    val games by viewModel.wishlistGames.collectAsState()
    val compact = LocalIsCompact.current
    val tokens = AppTheme.tokens

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(if (compact) 20.dp else 0.dp))
        Text("Wishlist", style = AppTheme.type.display, color = tokens.colors.text)
        Spacer(Modifier.height(14.dp))

        if (games.isEmpty()) {
            EmptyWishlist()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (compact) 3 else 6),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(games, key = { it.id }) { game ->
                    GameTile(
                        title = game.name,
                        stores = emptyList(),
                        status = null,
                        coverImageId = game.coverImageId,
                        onClick = { onOpenGame(game.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWishlist() {
    val tokens = AppTheme.tokens
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nothing wishlisted", style = AppTheme.type.display, color = tokens.colors.text)
            Spacer(Modifier.height(6.dp))
            Text(
                "Add a game and mark it as wanted to see it here.",
                style = AppTheme.type.body,
                color = tokens.colors.faint,
                textAlign = TextAlign.Center,
            )
        }
    }
}
