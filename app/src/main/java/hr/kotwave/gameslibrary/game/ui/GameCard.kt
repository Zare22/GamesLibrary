package hr.kotwave.gameslibrary.game.ui

import android.R.attr.onClick
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.game.Game

@Composable
fun GameCard(
    game: Game,
    modifier: Modifier = Modifier,
    onGameClick: () -> Unit,
    onDismissState: (SwipeToDismissBoxValue) -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDismissState(it)
                    return@rememberSwipeToDismissBoxState false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDismissState(it)
                    return@rememberSwipeToDismissBoxState false
                }
                else -> return@rememberSwipeToDismissBoxState false
            }
        },
        positionalThreshold = { it * .25f }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = { DismissBackground(dismissState) })
    {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clickable { onGameClick() },
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = game.platform.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Color.Green
        SwipeToDismissBoxValue.EndToStart -> Color.Red
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .background(color, MaterialTheme.shapes.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(imageVector = Icons.Default.Check, contentDescription = "Played")
        Spacer(modifier = Modifier)
        Icon(Icons.Default.Delete, contentDescription = "Delete")
    }
}