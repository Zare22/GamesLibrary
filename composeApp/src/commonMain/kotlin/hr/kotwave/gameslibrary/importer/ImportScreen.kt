package hr.kotwave.gameslibrary.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.IgdbSearchResult
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.data.sync.ImportSummary
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.cd_back
import hr.kotwave.gameslibrary.resources.cd_selected
import hr.kotwave.gameslibrary.resources.common_cancel
import hr.kotwave.gameslibrary.resources.error_igdb_unreachable
import hr.kotwave.gameslibrary.resources.import_add_button
import hr.kotwave.gameslibrary.resources.import_adding
import hr.kotwave.gameslibrary.resources.import_ambiguous_count
import hr.kotwave.gameslibrary.resources.import_done_another
import hr.kotwave.gameslibrary.resources.import_done_subtitle
import hr.kotwave.gameslibrary.resources.import_done_title
import hr.kotwave.gameslibrary.resources.import_footnote_empty
import hr.kotwave.gameslibrary.resources.import_footnote_pick_store
import hr.kotwave.gameslibrary.resources.import_footnote_ready
import hr.kotwave.gameslibrary.resources.import_matching_title
import hr.kotwave.gameslibrary.resources.import_parse_button
import hr.kotwave.gameslibrary.resources.import_paste_label
import hr.kotwave.gameslibrary.resources.import_paste_placeholder
import hr.kotwave.gameslibrary.resources.import_progress
import hr.kotwave.gameslibrary.resources.import_review_count
import hr.kotwave.gameslibrary.resources.import_review_error
import hr.kotwave.gameslibrary.resources.import_review_title
import hr.kotwave.gameslibrary.resources.import_share_hint_desktop
import hr.kotwave.gameslibrary.resources.import_share_hint_phone
import hr.kotwave.gameslibrary.resources.import_show_all
import hr.kotwave.gameslibrary.resources.import_step_paste
import hr.kotwave.gameslibrary.resources.import_step_store
import hr.kotwave.gameslibrary.resources.import_subtitle
import hr.kotwave.gameslibrary.resources.import_tag_matched
import hr.kotwave.gameslibrary.resources.import_tag_pick
import hr.kotwave.gameslibrary.resources.import_tag_unmatched
import hr.kotwave.gameslibrary.resources.import_title
import hr.kotwave.gameslibrary.resources.import_unmatched_manual
import hr.kotwave.gameslibrary.resources.line_count
import hr.kotwave.gameslibrary.resources.similar_title_warning
import hr.kotwave.gameslibrary.resources.sync_review_confirm
import hr.kotwave.gameslibrary.resources.sync_review_dismiss
import hr.kotwave.gameslibrary.resources.sync_review_dismissed
import hr.kotwave.gameslibrary.search.IgdbResultRow
import hr.kotwave.gameslibrary.ui.components.CoverArt
import hr.kotwave.gameslibrary.ui.components.GlowBox
import hr.kotwave.gameslibrary.ui.components.actionWidth
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.gameMeta
import hr.kotwave.gameslibrary.ui.shell.LocalIsCompact
import hr.kotwave.gameslibrary.ui.model.glyph
import hr.kotwave.gameslibrary.ui.model.label
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Ambiguous candidates show this many matches inline before a "Show all" expander. */
private const val AMBIGUOUS_INLINE_CAP = 3

/**
 * The paste Import tab: Intake (paste + Store) → Matching → Review → Done. Each Candidate passes
 * through Review before anything enters the library; Confirm is additive.
 */
@Composable
fun ImportScreen(modifier: Modifier = Modifier, viewModel: ImportViewModel = koinViewModel()) {
    val inbox = koinInject<SharedTextInbox>()
    val shared by inbox.pending.collectAsState()
    LaunchedEffect(shared) {
        shared?.let {
            viewModel.receiveSharedText(it)
            inbox.clear()
        }
    }
    when (val phase = viewModel.phase) {
        ImportPhase.Intake -> IntakePhase(viewModel, modifier)
        ImportPhase.Matching -> MatchingPhase(viewModel, modifier)
        ImportPhase.Review -> ReviewPhase(viewModel, modifier)
        is ImportPhase.Done -> DonePhase(phase.summary, viewModel::reset, modifier)
    }
}

// ---- Intake -------------------------------------------------------------------------------------

@Composable
private fun IntakePhase(viewModel: ImportViewModel, modifier: Modifier) {
    val tokens = AppTheme.tokens
    val compact = LocalIsCompact.current
    Column(modifier.fillMaxSize().padding(horizontal = tokens.spacing.lg)) {
        Spacer(Modifier.height(tokens.spacing.lg))
        Text(stringResource(Res.string.import_title), style = AppTheme.type.display, color = tokens.colors.text)
        Spacer(Modifier.height(tokens.spacing.micro))
        Text(stringResource(Res.string.import_subtitle), style = AppTheme.type.body, color = tokens.colors.faint)

        Column((if (compact) Modifier.weight(1f) else Modifier.fillMaxWidth()).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(tokens.spacing.md))
            if (viewModel.failed) {
                Notice(stringResource(Res.string.error_igdb_unreachable), tokens.colors.error)
                Spacer(Modifier.height(tokens.spacing.md))
            }
            StepLabel(1, stringResource(Res.string.import_step_paste))
            Spacer(Modifier.height(tokens.spacing.xs))
            PasteCard(
                text = viewModel.pasteText,
                lineCount = viewModel.lineCount,
                onTextChange = viewModel::updateText,
                modifier = Modifier.importFileDrop(viewModel::updateText),
            )
            Spacer(Modifier.height(tokens.spacing.sm))
            ShareHint()
            Spacer(Modifier.height(tokens.spacing.lg))
            StepLabel(2, stringResource(Res.string.import_step_store))
            Spacer(Modifier.height(tokens.spacing.sm))
            StorePicker(selected = viewModel.store, onSelect = viewModel::selectStore)
            Spacer(Modifier.height(tokens.spacing.md))
        }

        IntakeFooter(viewModel)
    }
}

@Composable
private fun PasteCard(text: String, lineCount: Int, onTextChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.xl)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0x59000000))
            .border(1.dp, tokens.colors.borderStrong, shape),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            Icon(AppIcons.Import, null, Modifier.size(14.dp), tint = tokens.colors.accent)
            Text(stringResource(Res.string.import_paste_label), style = AppTheme.type.section.copy(fontSize = 11.sp), color = tokens.colors.faint)
            Spacer(Modifier.weight(1f))
            Text(pluralStringResource(Res.plurals.line_count, lineCount, lineCount), style = AppTheme.type.caption, color = tokens.colors.muted)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 200.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.sm),
        ) {
            if (text.isEmpty()) {
                Text(
                    stringResource(Res.string.import_paste_placeholder),
                    style = AppTheme.type.body,
                    color = tokens.colors.faint,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = AppTheme.type.body.copy(color = tokens.colors.text),
                cursorBrush = SolidColor(tokens.colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ShareHint() {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.tile)
    val iconShape = RoundedCornerShape(tokens.radii.sm)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.surface)
            .border(1.dp, tokens.colors.border, shape)
            .padding(tokens.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Box(
            Modifier.size(32.dp).clip(iconShape)
                .background(tokens.colors.accent.copy(alpha = 0.10f))
                .border(1.dp, tokens.colors.accent.copy(alpha = 0.30f), iconShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Import, null, Modifier.size(16.dp), tint = tokens.colors.accent)
        }
        Column {
            Text(stringResource(Res.string.import_share_hint_desktop), style = AppTheme.type.caption, color = tokens.colors.muted)
            Spacer(Modifier.height(tokens.spacing.micro))
            Text(stringResource(Res.string.import_share_hint_phone), style = AppTheme.type.caption, color = tokens.colors.faint)
        }
    }
}

@Composable
private fun IntakeFooter(viewModel: ImportViewModel) {
    val tokens = AppTheme.tokens
    Column(Modifier.fillMaxWidth().padding(top = tokens.spacing.sm, bottom = tokens.spacing.lg)) {
        PrimaryButton(
            text = stringResource(Res.string.import_parse_button),
            onClick = viewModel::parse,
            leadingIcon = AppIcons.Import,
            enabled = viewModel.canParse,
            modifier = Modifier.actionWidth(),
        )
        Spacer(Modifier.height(tokens.spacing.xs))
        val store = viewModel.store
        val footnote = when {
            viewModel.pasteText.isBlank() -> stringResource(Res.string.import_footnote_empty)
            store == null -> pluralStringResource(Res.plurals.import_footnote_pick_store, viewModel.lineCount, viewModel.lineCount)
            else -> pluralStringResource(Res.plurals.import_footnote_ready, viewModel.lineCount, viewModel.lineCount, store.label)
        }
        Text(footnote, style = AppTheme.type.caption, color = tokens.colors.faint, modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.micro))
    }
}

// ---- Matching (the importer-parsing state) ------------------------------------------------------

@Composable
internal fun MatchingPhase(viewModel: ImportViewModel, modifier: Modifier) {
    val tokens = AppTheme.tokens
    val total = viewModel.matchTotal
    val done = viewModel.matchProgress
    Column(
        modifier.fillMaxSize().padding(horizontal = tokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = tokens.colors.accent, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(tokens.spacing.lg))
        Text(stringResource(Res.string.import_matching_title), style = AppTheme.type.bodyStrong, color = tokens.colors.text)
        Spacer(Modifier.height(tokens.spacing.xs))
        Text(stringResource(Res.string.import_progress, done, total), style = AppTheme.type.caption, color = tokens.colors.faint)
        Spacer(Modifier.height(tokens.spacing.lg))
        ProgressBar(fraction = if (total == 0) 0f else done.toFloat() / total)
        Spacer(Modifier.height(tokens.spacing.xl))
        CancelAction(viewModel::backToIntake)
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    val tokens = AppTheme.tokens
    Box(
        Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(tokens.colors.surface),
    ) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).clip(CircleShape).background(tokens.colors.accent),
        )
    }
}

// ---- Review -------------------------------------------------------------------------------------

@Composable
internal fun ReviewPhase(viewModel: ImportViewModel, modifier: Modifier) {
    val tokens = AppTheme.tokens
    Column(modifier.fillMaxSize().padding(horizontal = tokens.spacing.lg)) {
        Spacer(Modifier.height(tokens.spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            BackButton(viewModel::backToIntake)
            Column {
                Text(stringResource(Res.string.import_review_title), style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
                Text(stringResource(Res.string.import_review_count, viewModel.candidates.size, viewModel.checkedCount), style = AppTheme.type.caption, color = tokens.colors.faint)
            }
        }
        Spacer(Modifier.height(tokens.spacing.xs))
        if (viewModel.failed) {
            Notice(stringResource(Res.string.import_review_error), tokens.colors.error)
            Spacer(Modifier.height(tokens.spacing.xs))
        }

        Box(Modifier.weight(1f)) {
            val showDismiss = viewModel.target is ImportTarget.SyncTail
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                items(viewModel.candidates) { candidate -> CandidateRow(candidate, showDismiss) }
                item { Spacer(Modifier.height(tokens.spacing.micro)) }
            }
            Box(
                Modifier.fillMaxWidth().height(12.dp).align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(0f to tokens.colors.bg, 1f to Color.Transparent)),
            )
        }

        Column(Modifier.fillMaxWidth().padding(top = tokens.spacing.sm, bottom = tokens.spacing.lg)) {
            val syncTail = viewModel.target is ImportTarget.SyncTail
            PrimaryButton(
                text = when {
                    viewModel.importing -> stringResource(Res.string.import_adding)
                    syncTail -> stringResource(Res.string.sync_review_confirm)
                    else -> stringResource(Res.string.import_add_button, viewModel.checkedCount)
                },
                onClick = viewModel::confirm,
                leadingIcon = AppIcons.Check,
                enabled = (viewModel.checkedCount > 0 || (syncTail && viewModel.dismissedCount > 0)) && !viewModel.importing,
                modifier = Modifier.actionWidth(),
            )
        }
    }
}

@Composable
private fun CandidateRow(candidate: ImportCandidate, showDismiss: Boolean) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.lg)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.surface)
            .border(1.dp, if (candidate.checked) tokens.colors.accent.copy(alpha = 0.40f) else tokens.colors.border, shape)
            .padding(tokens.spacing.sm),
    ) {
        Column(Modifier.alpha(if (candidate.dismissed) 0.45f else 1f)) {
            when (val classification = candidate.classification) {
                is MatchClassification.Matched -> MatchedHead(candidate, classification.result)
                is MatchClassification.Ambiguous -> AmbiguousHead(candidate, classification.results)
                MatchClassification.Unmatched -> UnmatchedHead(candidate)
            }
        }
        if (showDismiss) {
            Spacer(Modifier.height(tokens.spacing.xs))
            DismissAction(candidate)
        }
    }
}

@Composable
private fun DismissAction(candidate: ImportCandidate) {
    val tokens = AppTheme.tokens
    val color = if (candidate.dismissed) tokens.colors.error else tokens.colors.muted
    Row(
        Modifier.clip(RoundedCornerShape(tokens.radii.md)).clickable(onClick = candidate::toggleDismissed)
            .padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Icon(AppIcons.Close, null, Modifier.size(14.dp), tint = color)
        Text(
            stringResource(if (candidate.dismissed) Res.string.sync_review_dismissed else Res.string.sync_review_dismiss),
            style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp),
            color = color,
        )
    }
}

@Composable
private fun MatchedHead(candidate: ImportCandidate, result: IgdbSearchResult) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        CoverArt(result.name, result.coverImageId, Modifier.size(width = 44.dp, height = 58.dp), RoundedCornerShape(tokens.radii.sm))
        Column(Modifier.weight(1f)) {
            MatchTag(stringResource(Res.string.import_tag_matched), tokens.colors.accent)
            Spacer(Modifier.height(tokens.spacing.micro))
            Text(result.name, style = AppTheme.type.bodyStrong, color = tokens.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            gameMeta(result.firstReleaseDate, result.developer)?.let {
                Text(it, style = AppTheme.type.caption, color = tokens.colors.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        CheckBox(candidate.checked, candidate::toggleChecked)
    }
}

@Composable
private fun AmbiguousHead(candidate: ImportCandidate, options: List<IgdbSearchResult>) {
    val tokens = AppTheme.tokens
    var expanded by remember { mutableStateOf(false) }
    val limit = if (expanded) options.size else maxOf(AMBIGUOUS_INLINE_CAP, candidate.pickedIndex + 1)
    MatchTag(stringResource(Res.string.import_tag_pick), tokens.colors.warning)
    Spacer(Modifier.height(tokens.spacing.xs))
    Text("“${candidate.rawTitle}”", style = AppTheme.type.bodyStrong, color = tokens.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(tokens.spacing.micro))
    Text(pluralStringResource(Res.plurals.import_ambiguous_count, options.size, options.size), style = AppTheme.type.caption, color = tokens.colors.faint)
    Spacer(Modifier.height(tokens.spacing.xs))
    options.take(limit).forEachIndexed { index, option ->
        IgdbResultRow(result = option, selected = candidate.pickedIndex == index, onClick = { candidate.pick(index) })
    }
    if (limit < options.size) {
        ShowAllMatches(total = options.size) { expanded = true }
    }
}

@Composable
private fun ShowAllMatches(total: Int, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    Row(
        Modifier.clip(RoundedCornerShape(tokens.radii.md)).clickable(onClick = onClick).padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Icon(AppIcons.ChevronRight, null, Modifier.size(14.dp), tint = tokens.colors.accent)
        Text(pluralStringResource(Res.plurals.import_show_all, total, total), style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp), color = tokens.colors.accent)
    }
}

@Composable
private fun UnmatchedHead(candidate: ImportCandidate) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        Box(
            Modifier.size(width = 44.dp, height = 58.dp).clip(RoundedCornerShape(tokens.radii.sm)).background(tokens.colors.bg2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Plus, null, Modifier.size(18.dp), tint = tokens.colors.muted)
        }
        Column(Modifier.weight(1f)) {
            MatchTag(stringResource(Res.string.import_tag_unmatched), tokens.colors.muted)
            Spacer(Modifier.height(tokens.spacing.micro))
            Text(candidate.rawTitle, style = AppTheme.type.bodyStrong, color = tokens.colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(Res.string.import_unmatched_manual), style = AppTheme.type.caption, color = tokens.colors.faint)
        }
        CheckBox(candidate.checked, candidate::toggleChecked)
    }
    if (candidate.alreadyInLibrary) {
        Spacer(Modifier.height(tokens.spacing.sm))
        SimilarTitleWarning(candidate.rawTitle)
    }
}

// ---- Done ---------------------------------------------------------------------------------------

@Composable
internal fun DonePhase(summary: ImportSummary, onDone: () -> Unit, modifier: Modifier) {
    val tokens = AppTheme.tokens
    Column(
        modifier.fillMaxSize().padding(horizontal = tokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(tokens.colors.accent.copy(alpha = 0.15f))
                .border(1.dp, tokens.colors.accent.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Check, null, Modifier.size(30.dp), tint = tokens.colors.accent)
        }
        Spacer(Modifier.height(tokens.spacing.lg))
        Text(pluralStringResource(Res.plurals.import_done_title, summary.total, summary.total), style = AppTheme.type.display.copy(fontSize = 22.sp), color = tokens.colors.text)
        Spacer(Modifier.height(tokens.spacing.xs))
        Text(
            stringResource(Res.string.import_done_subtitle, summary.added, summary.attached),
            style = AppTheme.type.body,
            color = tokens.colors.faint,
        )
        Spacer(Modifier.height(tokens.spacing.xl))
        PrimaryButton(stringResource(Res.string.import_done_another), onDone, leadingIcon = AppIcons.Import)
    }
}

// ---- Shared bits --------------------------------------------------------------------------------

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
        if (checked) Icon(AppIcons.Check, stringResource(Res.string.cd_selected), Modifier.size(16.dp), tint = tokens.colors.accent)
    }
}

@Composable
private fun MatchTag(text: String, color: Color) {
    Text(text.uppercase(), style = AppTheme.type.section.copy(fontSize = 10.sp), color = color)
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
private fun CancelAction(onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    Text(
        stringResource(Res.string.common_cancel),
        style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp),
        color = tokens.colors.muted,
        modifier = Modifier.clip(RoundedCornerShape(tokens.radii.md)).clickable(onClick = onClick).padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.xs),
    )
}

@Composable
private fun Notice(text: String, color: Color) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), shape).padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Icon(AppIcons.Close, null, Modifier.size(14.dp), tint = color)
        Text(text, style = AppTheme.type.caption, color = tokens.colors.muted)
    }
}

@Composable
private fun SimilarTitleWarning(existing: String) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(tokens.colors.warning.copy(alpha = 0.08f))
            .border(1.dp, tokens.colors.warning.copy(alpha = 0.30f), shape).padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Icon(AppIcons.Heart, null, Modifier.size(14.dp), tint = tokens.colors.warning)
        Text(
            stringResource(Res.string.similar_title_warning, existing),
            style = AppTheme.type.caption,
            color = tokens.colors.muted,
        )
    }
}

@Composable
private fun StepLabel(number: Int, text: String) {
    val tokens = AppTheme.tokens
    val accent = tokens.colors.accent
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
        Box(
            Modifier.size(18.dp).clip(CircleShape).background(accent.copy(alpha = 0.20f))
                .border(1.dp, accent.copy(alpha = 0.50f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = AppTheme.type.caption.copy(fontSize = 10.sp), color = Color(0xFFBCD4FF))
        }
        Text(text.uppercase(), style = AppTheme.type.section.copy(fontSize = 11.sp), color = accent)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorePicker(selected: Store?, onSelect: (Store) -> Unit) {
    val tokens = AppTheme.tokens
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        // Paste stores only; Battle.net intakes via its own checklist picker.
        Store.entries.filter { it != Store.BATTLE_NET }.forEach { store ->
            StoreChip(store = store, selected = store == selected) { onSelect(store) }
        }
    }
}

@Composable
private fun StoreChip(store: Store, selected: Boolean, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val accent = tokens.store.accent(store)
    val shape = RoundedCornerShape(tokens.radii.md)
    GlowBox(
        glow = if (selected && tokens.store.glows(store)) accent else null,
        shape = shape,
        glowRadius = 12.dp,
        glowAlpha = 0.35f,
    ) {
        Row(
            Modifier
                .clip(shape)
                .background(if (selected) accent.copy(alpha = 0.10f) else tokens.colors.surface)
                .border(1.dp, if (selected) accent.copy(alpha = 0.55f) else tokens.colors.border, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                Text(store.glyph, style = AppTheme.type.brand.copy(fontSize = 10.sp), color = tokens.store.glyph(store))
            }
            Text(
                store.label,
                style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp),
                color = if (selected) tokens.colors.text else tokens.colors.muted,
            )
            if (selected) Icon(AppIcons.Check, null, Modifier.size(14.dp), tint = accent)
        }
    }
}
