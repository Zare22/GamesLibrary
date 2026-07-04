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
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.action_back
import hr.kotwave.gameslibrary.resources.cd_back
import hr.kotwave.gameslibrary.resources.common_done
import hr.kotwave.gameslibrary.resources.import_add_button
import hr.kotwave.gameslibrary.resources.import_adding
import hr.kotwave.gameslibrary.resources.import_done_title
import hr.kotwave.gameslibrary.resources.transfer_add_new
import hr.kotwave.gameslibrary.resources.transfer_choose_another
import hr.kotwave.gameslibrary.resources.transfer_collision_note
import hr.kotwave.gameslibrary.resources.transfer_done_subtitle
import hr.kotwave.gameslibrary.resources.transfer_fail_empty
import hr.kotwave.gameslibrary.resources.transfer_fail_import
import hr.kotwave.gameslibrary.resources.transfer_fail_invalid
import hr.kotwave.gameslibrary.resources.transfer_failed_title
import hr.kotwave.gameslibrary.resources.transfer_importing
import hr.kotwave.gameslibrary.resources.transfer_kind_already
import hr.kotwave.gameslibrary.resources.transfer_kind_collision
import hr.kotwave.gameslibrary.resources.transfer_kind_manual
import hr.kotwave.gameslibrary.resources.transfer_kind_new
import hr.kotwave.gameslibrary.resources.transfer_merge
import hr.kotwave.gameslibrary.resources.transfer_picking
import hr.kotwave.gameslibrary.resources.transfer_reading
import hr.kotwave.gameslibrary.resources.transfer_review_count
import hr.kotwave.gameslibrary.resources.transfer_title
import hr.kotwave.gameslibrary.ui.components.CoverArt
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.gameMeta
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
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
        TransferPhase.Picking -> LoadingState(stringResource(Res.string.transfer_picking))
        TransferPhase.Working -> LoadingState(if (viewModel.importing) stringResource(Res.string.transfer_importing) else stringResource(Res.string.transfer_reading))
        TransferPhase.Review -> ReviewState(viewModel, onBack)
        is TransferPhase.Done -> DoneState(phase.summary, onBack)
        is TransferPhase.Failed -> FailedState(phase.reason.message(), onRetry = pickFile, onBack = onBack)
    }
}

@Composable
private fun TransferFailure.message(): String = when (this) {
    TransferFailure.InvalidFile -> stringResource(Res.string.transfer_fail_invalid)
    TransferFailure.EmptyFile -> stringResource(Res.string.transfer_fail_empty)
    TransferFailure.ImportFailed -> stringResource(Res.string.transfer_fail_import)
}

@Composable
private fun LoadingState(label: String) {
    val tokens = AppTheme.tokens
    Column(
        Modifier.fillMaxSize().padding(horizontal = tokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = tokens.colors.accent, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(tokens.spacing.lg))
        Text(label, style = AppTheme.type.bodyStrong, color = tokens.colors.text)
    }
}

@Composable
private fun ReviewState(viewModel: LibraryTransferViewModel, onBack: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(Modifier.fillMaxSize().padding(horizontal = tokens.spacing.lg)) {
        Spacer(Modifier.height(tokens.spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            BackButton(onBack)
            Column {
                Text(stringResource(Res.string.transfer_title), style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
                Text(
                    stringResource(Res.string.transfer_review_count, viewModel.candidates.size, viewModel.checkedCount),
                    style = AppTheme.type.caption,
                    color = tokens.colors.faint,
                )
            }
        }
        Spacer(Modifier.height(tokens.spacing.xs))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            items(viewModel.candidates) { candidate -> CandidateRow(candidate) }
            item { Spacer(Modifier.height(tokens.spacing.micro)) }
        }

        Column(Modifier.fillMaxWidth().padding(top = tokens.spacing.sm, bottom = tokens.spacing.lg)) {
            PrimaryButton(
                text = if (viewModel.importing) stringResource(Res.string.import_adding) else stringResource(Res.string.import_add_button, viewModel.checkedCount),
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
    val shape = RoundedCornerShape(tokens.radii.lg)
    val coverShape = RoundedCornerShape(tokens.radii.sm)
    val game = candidate.row.game
    Column(
        Modifier.fillMaxWidth().clip(shape).background(tokens.colors.surface)
            .border(1.dp, if (candidate.checked) tokens.colors.accent.copy(alpha = 0.40f) else tokens.colors.border, shape)
            .padding(tokens.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            if (game.igdbId != null) {
                CoverArt(game.name, game.coverImageId, Modifier.size(width = 44.dp, height = 58.dp), coverShape)
            } else {
                Box(
                    Modifier.size(width = 44.dp, height = 58.dp).clip(coverShape).background(tokens.colors.bg2),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Plus, null, Modifier.size(18.dp), tint = tokens.colors.muted)
                }
            }
            Column(Modifier.weight(1f)) {
                KindTag(candidate.row.kind)
                Spacer(Modifier.height(tokens.spacing.micro))
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
            Spacer(Modifier.height(tokens.spacing.sm))
            CollisionChoice(candidate)
        }
    }
}

@Composable
private fun CollisionChoice(candidate: LibraryImportCandidate) {
    val tokens = AppTheme.tokens
    Text(
        stringResource(Res.string.transfer_collision_note),
        style = AppTheme.type.caption,
        color = tokens.colors.muted,
    )
    Spacer(Modifier.height(tokens.spacing.xs))
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
        ChoiceChip(stringResource(Res.string.transfer_merge), selected = candidate.mergeByTitle) { candidate.mergeByTitle = true }
        ChoiceChip(stringResource(Res.string.transfer_add_new), selected = !candidate.mergeByTitle) { candidate.mergeByTitle = false }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Text(
        label,
        style = AppTheme.type.caption.copy(fontSize = 12.sp),
        color = if (selected) tokens.colors.text else tokens.colors.muted,
        modifier = Modifier.clip(shape)
            .background(if (selected) tokens.colors.accent.copy(alpha = 0.14f) else tokens.colors.surface)
            .border(1.dp, if (selected) tokens.colors.accent.copy(alpha = 0.55f) else tokens.colors.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
    )
}

@Composable
private fun KindTag(kind: ImportRowKind) {
    val (text, color) = when (kind) {
        ImportRowKind.AlreadyById -> stringResource(Res.string.transfer_kind_already) to AppTheme.tokens.colors.muted
        ImportRowKind.NewMatched -> stringResource(Res.string.transfer_kind_new) to AppTheme.tokens.colors.accent
        ImportRowKind.TitleCollision -> stringResource(Res.string.transfer_kind_collision) to Amber
        ImportRowKind.NewManual -> stringResource(Res.string.transfer_kind_manual) to AppTheme.tokens.colors.accent
    }
    Text(text.uppercase(), style = AppTheme.type.section.copy(fontSize = 10.sp), color = color)
}

@Composable
private fun DoneState(summary: ImportSummary, onDone: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(
        Modifier.fillMaxSize().padding(horizontal = tokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(Ok.copy(alpha = 0.15f)).border(1.dp, Ok.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Check, null, Modifier.size(30.dp), tint = Ok)
        }
        Spacer(Modifier.height(tokens.spacing.lg))
        Text(pluralStringResource(Res.plurals.import_done_title, summary.total, summary.total), style = AppTheme.type.display.copy(fontSize = 22.sp), color = tokens.colors.text)
        Spacer(Modifier.height(tokens.spacing.xs))
        Text(
            stringResource(Res.string.transfer_done_subtitle, summary.added, summary.attached),
            style = AppTheme.type.body,
            color = tokens.colors.faint,
        )
        Spacer(Modifier.height(tokens.spacing.xl))
        PrimaryButton(stringResource(Res.string.common_done), onDone, leadingIcon = AppIcons.Check)
    }
}

@Composable
private fun FailedState(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(
        Modifier.fillMaxSize().padding(horizontal = tokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(ErrorRed.copy(alpha = 0.12f)).border(1.dp, ErrorRed.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Close, null, Modifier.size(28.dp), tint = ErrorRed)
        }
        Spacer(Modifier.height(tokens.spacing.lg))
        Text(stringResource(Res.string.transfer_failed_title), style = AppTheme.type.display.copy(fontSize = 20.sp), color = tokens.colors.text)
        Spacer(Modifier.height(tokens.spacing.xs))
        Text(message, style = AppTheme.type.body, color = tokens.colors.faint)
        Spacer(Modifier.height(tokens.spacing.xl))
        PrimaryButton(stringResource(Res.string.transfer_choose_another), onRetry, leadingIcon = AppIcons.ImportFile)
        Spacer(Modifier.height(tokens.spacing.sm))
        Text(
            stringResource(Res.string.action_back),
            style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp),
            color = tokens.colors.muted,
            modifier = Modifier.clip(RoundedCornerShape(tokens.radii.md)).clickable(onClick = onBack).padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.xs),
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Box(
        Modifier.size(36.dp).clip(shape).background(tokens.colors.surface)
            .border(1.dp, tokens.colors.border, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.ChevronLeft, stringResource(Res.string.cd_back), Modifier.size(18.dp), tint = tokens.colors.muted)
    }
}

@Composable
private fun CheckBox(checked: Boolean, onToggle: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.sm)
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
