package hr.kotwave.gameslibrary.transfer

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.ImportSummary
import hr.kotwave.gameslibrary.ui.components.CoverArt
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.gameMeta
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

private val Amber = Color(0xFFFFD24A)
private val ErrorRed = Color(0xFFF4707A)
private val Ok = Color(0xFF46D39A)

/**
 * Library import: picks a GamesLibrary export file, classifies each game against the current library,
 * and reviews per row before applying it additively. `igdbId` rows dedup by id; `igdb`-null
 * rows that collide with an existing title offer merge-or-add.
 */
@Composable
fun LibraryImportScreen(onBack: () -> Unit, viewModel: LibraryTransferViewModel = koinViewModel()) {
    val io = rememberLibraryFileIo()
    val pickFile = { io.import { text -> if (text == null) onBack() else viewModel.onFilePicked(text) } }
    LaunchedEffect(Unit) { pickFile() }

    when (val phase = viewModel.phase) {
        TransferPhase.Picking -> LoadingState("Choose a GamesLibrary file…")
        TransferPhase.Working -> LoadingState(if (viewModel.importing) "Adding to your library…" else "Reading file…")
        TransferPhase.Review -> ReviewState(viewModel, onBack)
        is TransferPhase.Done -> DoneState(phase.summary, onBack)
        is TransferPhase.Failed -> FailedState(phase.message, onRetry = pickFile, onBack = onBack)
    }
}

@Composable
private fun LoadingState(label: String) {
    val tokens = AppTheme.tokens
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = tokens.colors.accent, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(20.dp))
        Text(label, style = AppTheme.type.bodyStrong, color = tokens.colors.text)
    }
}

@Composable
private fun ReviewState(viewModel: LibraryTransferViewModel, onBack: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(onBack)
            Column {
                Text("Import library", style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
                Text(
                    "${viewModel.candidates.size} in file · ${viewModel.checkedCount} selected",
                    style = AppTheme.type.caption,
                    color = tokens.colors.faint,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(viewModel.candidates) { candidate -> CandidateRow(candidate) }
            item { Spacer(Modifier.height(4.dp)) }
        }

        Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp)) {
            PrimaryButton(
                text = if (viewModel.importing) "Adding…" else "Add ${viewModel.checkedCount} to library",
                onClick = viewModel::confirm,
                leadingIcon = AppIcons.Check,
                enabled = viewModel.checkedCount > 0 && !viewModel.importing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CandidateRow(candidate: LibraryImportCandidate) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(16.dp)
    val game = candidate.row.game
    Column(
        Modifier.fillMaxWidth().clip(shape).background(tokens.colors.surface)
            .border(1.dp, if (candidate.checked) tokens.colors.accent.copy(alpha = 0.40f) else tokens.colors.border, shape)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (game.igdbId != null) {
                CoverArt(game.name, game.coverImageId, Modifier.size(width = 44.dp, height = 58.dp), RoundedCornerShape(9.dp))
            } else {
                Box(
                    Modifier.size(width = 44.dp, height = 58.dp).clip(RoundedCornerShape(9.dp)).background(tokens.colors.bg2),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Plus, null, Modifier.size(18.dp), tint = tokens.colors.muted)
                }
            }
            Column(Modifier.weight(1f)) {
                KindTag(candidate.row.kind)
                Spacer(Modifier.height(4.dp))
                Text(game.name, style = AppTheme.type.bodyStrong, color = tokens.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val meta = if (game.igdbId != null) gameMeta(game.firstReleaseDate, game.developer) else null
                val stores = game.ownerships.joinToString(" · ") { it.store }.ifBlank { null }
                (meta ?: stores)?.let {
                    Text(it, style = AppTheme.type.caption, color = tokens.colors.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            CheckBox(candidate.checked) { candidate.checked = !candidate.checked }
        }
        if (candidate.row.kind == ImportRowKind.TitleCollision) {
            Spacer(Modifier.height(10.dp))
            CollisionChoice(candidate)
        }
    }
}

@Composable
private fun CollisionChoice(candidate: LibraryImportCandidate) {
    val tokens = AppTheme.tokens
    Text(
        "A game with this title is already in your library.",
        style = AppTheme.type.caption,
        color = tokens.colors.muted,
    )
    Spacer(Modifier.height(7.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceChip("Merge onto it", selected = candidate.mergeByTitle) { candidate.mergeByTitle = true }
        ChoiceChip("Add as new", selected = !candidate.mergeByTitle) { candidate.mergeByTitle = false }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(10.dp)
    Text(
        label,
        style = AppTheme.type.caption.copy(fontSize = 12.sp),
        color = if (selected) tokens.colors.text else tokens.colors.muted,
        modifier = Modifier.clip(shape)
            .background(if (selected) tokens.colors.accent.copy(alpha = 0.14f) else tokens.colors.surface)
            .border(1.dp, if (selected) tokens.colors.accent.copy(alpha = 0.55f) else tokens.colors.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun KindTag(kind: ImportRowKind) {
    val (text, color) = when (kind) {
        ImportRowKind.AlreadyById -> "Already in library" to AppTheme.tokens.colors.muted
        ImportRowKind.NewMatched -> "New" to AppTheme.tokens.colors.accent
        ImportRowKind.TitleCollision -> "Title exists" to Amber
        ImportRowKind.NewManual -> "New · manual" to AppTheme.tokens.colors.accent
    }
    Text(text.uppercase(), style = AppTheme.type.section.copy(fontSize = 10.sp), color = color)
}

@Composable
private fun DoneState(summary: ImportSummary, onDone: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(Ok.copy(alpha = 0.15f)).border(1.dp, Ok.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Check, null, Modifier.size(30.dp), tint = Ok)
        }
        Spacer(Modifier.height(18.dp))
        Text("Imported ${summary.total} games", style = AppTheme.type.display.copy(fontSize = 22.sp), color = tokens.colors.text)
        Spacer(Modifier.height(6.dp))
        Text(
            "${summary.added} added · ${summary.attached} merged into existing.",
            style = AppTheme.type.body,
            color = tokens.colors.faint,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Done", onDone, leadingIcon = AppIcons.Check)
    }
}

@Composable
private fun FailedState(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(ErrorRed.copy(alpha = 0.12f)).border(1.dp, ErrorRed.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Close, null, Modifier.size(28.dp), tint = ErrorRed)
        }
        Spacer(Modifier.height(18.dp))
        Text("Couldn't import", style = AppTheme.type.display.copy(fontSize = 20.sp), color = tokens.colors.text)
        Spacer(Modifier.height(6.dp))
        Text(message, style = AppTheme.type.body, color = tokens.colors.faint)
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Choose another file", onRetry, leadingIcon = AppIcons.ImportFile)
        Spacer(Modifier.height(10.dp))
        Text(
            "Back",
            style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp),
            color = tokens.colors.muted,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onBack).padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(tokens.colors.surface)
            .border(1.dp, tokens.colors.border, RoundedCornerShape(11.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.ChevronLeft, "Back", Modifier.size(18.dp), tint = tokens.colors.muted)
    }
}

@Composable
private fun CheckBox(checked: Boolean, onToggle: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.size(26.dp).clip(shape)
            .background(if (checked) tokens.colors.accent.copy(alpha = 0.18f) else tokens.colors.surface)
            .border(1.dp, if (checked) tokens.colors.accent.copy(alpha = 0.60f) else tokens.colors.borderStrong, shape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(AppIcons.Check, "Selected", Modifier.size(16.dp), tint = tokens.colors.accent)
    }
}
