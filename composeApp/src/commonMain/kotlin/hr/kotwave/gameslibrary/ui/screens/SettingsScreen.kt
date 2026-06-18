package hr.kotwave.gameslibrary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.Game
import hr.kotwave.gameslibrary.settings.SettingsViewModel
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.components.SecondaryButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

private val OrphanRed = Color(0xFFF4707A)

/** Settings: Orphaned-game maintenance, plus the developer entry to the component gallery. */
@Composable
fun SettingsScreen(
    onOpenGallery: () -> Unit,
    onOpenGame: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val tokens = AppTheme.tokens
    val orphaned by viewModel.orphanedGames.collectAsState()
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text("Settings", style = AppTheme.type.display, color = tokens.colors.text)

        if (orphaned.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionLabel("LIBRARY")
            OrphanedSection(
                games = orphaned,
                retrying = viewModel.retrying,
                onRetryAll = viewModel::retryAllOrphaned,
                onOpenGame = onOpenGame,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("DEVELOPER")
        SettingsRow(
            icon = AppIcons.Grid,
            title = "Component gallery",
            subtitle = "Every reusable component",
            onClick = onOpenGallery,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = AppTheme.type.section,
        color = AppTheme.tokens.colors.faint,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
    )
}

@Composable
private fun OrphanedSection(
    games: List<Game>,
    retrying: Boolean,
    onRetryAll: () -> Unit,
    onOpenGame: (Long) -> Unit,
) {
    val tokens = AppTheme.tokens
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(OrphanRed.copy(alpha = 0.06f))
            .border(1.dp, OrphanRed.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                    .background(OrphanRed.copy(alpha = 0.14f)).border(1.dp, OrphanRed.copy(alpha = 0.40f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Sync, null, Modifier.size(18.dp), tint = OrphanRed)
            }
            Column(Modifier.weight(1f)) {
                Text("Orphaned games · ${games.size}", style = AppTheme.type.bodyStrong, color = tokens.colors.text)
                Text(
                    "Their IGDB link broke. Retry resolves any that came back; re-match the rest individually.",
                    style = AppTheme.type.caption.copy(fontSize = 11.5.sp),
                    color = tokens.colors.muted,
                )
            }
        }
        games.forEach { game ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                    .background(tokens.colors.surface).border(1.dp, tokens.colors.border, RoundedCornerShape(11.dp))
                    .clickable { onOpenGame(game.id) }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(game.name, style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp), color = tokens.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(AppIcons.ChevronRight, null, Modifier.size(16.dp), tint = tokens.colors.faint)
            }
        }
        SecondaryButton(
            text = if (retrying) "Re-matching…" else "Re-match all",
            onClick = onRetryAll,
            leadingIcon = AppIcons.Sync,
            enabled = !retrying,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val tokens = AppTheme.tokens
    GlassSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                GlassSurface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(11.dp),
                    fill = tokens.colors.surfaceRaised,
                ) {}
                Icon(icon, null, Modifier.size(18.dp), tint = tokens.colors.muted)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = AppTheme.type.bodyStrong, color = tokens.colors.text)
                Text(subtitle, style = AppTheme.type.caption, color = tokens.colors.faint)
            }
            Icon(AppIcons.ChevronRight, null, Modifier.size(17.dp), tint = tokens.colors.faint)
        }
    }
}
