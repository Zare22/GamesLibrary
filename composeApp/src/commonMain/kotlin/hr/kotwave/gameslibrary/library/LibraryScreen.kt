package hr.kotwave.gameslibrary.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.ui.components.AppIconButton
import hr.kotwave.gameslibrary.ui.components.BrandWordmark
import hr.kotwave.gameslibrary.ui.components.GameTile
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.shell.LocalIsCompact
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

/** Library home: header chrome over the owned-games grid. */
@Composable
fun LibraryScreen(
    onAdd: () -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val games by viewModel.ownedGames.collectAsState()
    val compact = LocalIsCompact.current
    val tokens = AppTheme.tokens

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandWordmark()
                Spacer(Modifier.weight(1f))
                AppIconButton(AppIcons.Sliders, onClick = {}, contentDescription = "Sort & filter")
                Spacer(Modifier.width(10.dp))
                AppIconButton(AppIcons.Plus, onClick = onAdd, contentDescription = "Add game", accent = true)
            }
            Spacer(Modifier.height(12.dp))
        } else {
            Text("Library", style = AppTheme.type.display, color = tokens.colors.text)
            Spacer(Modifier.height(14.dp))
        }
        SearchField()
        Spacer(Modifier.height(16.dp))

        if (games.isEmpty()) {
            EmptyLibrary()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (compact) 3 else 6),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(games, key = { it.game.id }) { owned ->
                    GameTile(
                        title = owned.game.name,
                        stores = owned.ownerships.map { it.store },
                        status = owned.game.status,
                        coverImageId = owned.game.coverImageId,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary() {
    val tokens = AppTheme.tokens
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No games yet", style = AppTheme.type.display, color = tokens.colors.text)
            Spacer(Modifier.height(6.dp))
            Text(
                "Add your first game to start your library.",
                style = AppTheme.type.body,
                color = tokens.colors.faint,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Non-interactive search field. */
@Composable
private fun SearchField() {
    val tokens = AppTheme.tokens
    val compact = LocalIsCompact.current
    GlassSurface(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(AppIcons.Search, null, Modifier.size(17.dp), tint = tokens.colors.faint)
            Text("Search games", style = AppTheme.type.body, color = tokens.colors.faint, modifier = Modifier.weight(1f))
            if (!compact) {
                GlassSurface(shape = RoundedCornerShape(6.dp)) {
                    Text(
                        "/",
                        style = AppTheme.type.caption,
                        color = tokens.colors.faint,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
