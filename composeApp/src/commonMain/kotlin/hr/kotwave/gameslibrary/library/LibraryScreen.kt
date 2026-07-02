package hr.kotwave.gameslibrary.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.action_add_game
import hr.kotwave.gameslibrary.resources.cd_clear_search
import hr.kotwave.gameslibrary.resources.library_clear
import hr.kotwave.gameslibrary.resources.library_empty_body
import hr.kotwave.gameslibrary.resources.library_empty_title
import hr.kotwave.gameslibrary.resources.library_no_matches_body
import hr.kotwave.gameslibrary.resources.library_search_hint
import hr.kotwave.gameslibrary.resources.library_title
import hr.kotwave.gameslibrary.resources.no_matches
import hr.kotwave.gameslibrary.ui.components.AppIconButton
import hr.kotwave.gameslibrary.ui.components.BrandWordmark
import hr.kotwave.gameslibrary.ui.components.GameTile
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.components.SecondaryButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.shell.LocalIsCompact
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Library home: header chrome + search/filter controls over the owned-games grid. */
@Composable
fun LibraryScreen(
    onAdd: () -> Unit,
    onOpenGame: (Long) -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val games by viewModel.games.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val libraryEmpty by viewModel.libraryEmpty.collectAsState()
    val compact = LocalIsCompact.current
    val tokens = AppTheme.tokens

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandWordmark()
                Spacer(Modifier.weight(1f))
                LibraryFilterButton(
                    filter,
                    onToggleStore = viewModel::toggleStore,
                    onToggleStatus = viewModel::toggleStatus,
                    onSetSort = viewModel::setSort,
                    onReset = viewModel::resetFilters,
                )
                Spacer(Modifier.size(10.dp))
                AppIconButton(AppIcons.Plus, onClick = onAdd, contentDescription = stringResource(Res.string.action_add_game), accent = true)
            }
            Spacer(Modifier.height(12.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.library_title), style = AppTheme.type.display, color = tokens.colors.text)
                Spacer(Modifier.weight(1f))
                LibraryFilterButton(
                    filter,
                    onToggleStore = viewModel::toggleStore,
                    onToggleStatus = viewModel::toggleStatus,
                    onSetSort = viewModel::setSort,
                    onReset = viewModel::resetFilters,
                )
            }
            Spacer(Modifier.height(14.dp))
        }
        SearchField(query = filter.query, onQueryChange = viewModel::setQuery)
        Spacer(Modifier.height(16.dp))

        when {
            libraryEmpty -> EmptyLibrary()
            games.isEmpty() -> NoMatches(onClear = viewModel::clearAll)
            else -> LazyVerticalGrid(
                columns = if (compact) GridCells.Fixed(3) else GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(games, key = { it.game.id }) { owned ->
                    GameTile(
                        title = owned.game.name,
                        stores = owned.ownerships.map { it.store },
                        status = owned.game.status,
                        coverImageId = owned.game.coverImageId,
                        onClick = { onOpenGame(owned.game.id) },
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
            Text(stringResource(Res.string.library_empty_title), style = AppTheme.type.display, color = tokens.colors.text)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(Res.string.library_empty_body),
                style = AppTheme.type.body,
                color = tokens.colors.faint,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NoMatches(onClear: () -> Unit) {
    val tokens = AppTheme.tokens
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(Res.string.no_matches), style = AppTheme.type.display, color = tokens.colors.text)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(Res.string.library_no_matches_body),
                style = AppTheme.type.body,
                color = tokens.colors.faint,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            SecondaryButton(stringResource(Res.string.library_clear), onClick = onClear)
        }
    }
}

/** Live title search: substring filter as you type, with a clear affordance. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
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
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(stringResource(Res.string.library_search_hint), style = AppTheme.type.body, color = tokens.colors.faint)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = AppTheme.type.body.copy(color = tokens.colors.text),
                    cursorBrush = SolidColor(tokens.colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    AppIcons.Close,
                    stringResource(Res.string.cd_clear_search),
                    Modifier.size(17.dp).clip(CircleShape).clickable { onQueryChange("") },
                    tint = tokens.colors.faint,
                )
            } else if (!compact) {
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
