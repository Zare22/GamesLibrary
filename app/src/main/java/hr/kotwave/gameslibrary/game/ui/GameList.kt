package hr.kotwave.gameslibrary.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.game.Game

@Composable
fun GameList(
    games: List<Game>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddGameClick: () -> Unit,
    onGameClick: (Game) -> Unit,
    onDelete: (Game) -> Unit,
    onCompleted: (Game) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddGameClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Game")
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search games") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(games, key = { it.id }) { game ->
                    GameCard(game, onGameClick = { onGameClick(game) }) {
                        when (it) {
                            SwipeToDismissBoxValue.EndToStart -> onDelete(game)
                            SwipeToDismissBoxValue.StartToEnd -> onCompleted(game)
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}