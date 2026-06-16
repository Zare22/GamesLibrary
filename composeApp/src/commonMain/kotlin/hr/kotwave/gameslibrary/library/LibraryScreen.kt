package hr.kotwave.gameslibrary.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.ui.components.AppIconButton
import hr.kotwave.gameslibrary.ui.components.BrandWordmark
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.components.SecondaryButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.shell.LocalIsCompact
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

/** Library home: header chrome over the game list. */
@Composable
fun LibraryScreen(
    onAdd: () -> Unit,
    viewModel: GameListViewModel = koinViewModel(),
) {
    val games by viewModel.games.collectAsState()
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
        }
        SearchField()
        Spacer(Modifier.height(16.dp))

        // Temporary dev affordance.
        SecondaryButton("Add sample game", onClick = viewModel::addSampleGame, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            items(games, key = { it.id }) { game ->
                Text(
                    game.name,
                    style = AppTheme.type.bodyStrong,
                    color = tokens.colors.text,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                )
                HorizontalDivider(color = tokens.colors.border)
            }
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
