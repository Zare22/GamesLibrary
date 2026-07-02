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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.IgdbSearchResult
import hr.kotwave.gameslibrary.data.ImportSummary
import hr.kotwave.gameslibrary.data.Store
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

private val Amber = Color(0xFFFFD24A)
private val ErrorRed = Color(0xFFF4707A)

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
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Text(stringResource(Res.string.import_title), style = AppTheme.type.display, color = tokens.colors.text)
        Spacer(Modifier.height(3.dp))
        Text(stringResource(Res.string.import_subtitle), style = AppTheme.type.body, color = tokens.colors.faint)

        Column((if (compact) Modifier.weight(1f) else Modifier.fillMaxWidth()).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            if (viewModel.failed) {
                Notice(stringResource(Res.string.error_igdb_unreachable), ErrorRed)
                Spacer(Modifier.height(14.dp))
            }
            StepLabel(1, stringResource(Res.string.import_step_paste))
            Spacer(Modifier.height(9.dp))
            PasteCard(
                text = viewModel.pasteText,
                lineCount = viewModel.lineCount,
                onTextChange = viewModel::updateText,
                modifier = Modifier.importFileDrop(viewModel::updateText),
            )
            Spacer(Modifier.height(13.dp))
            ShareHint()
            Spacer(Modifier.height(18.dp))
            StepLabel(2, stringResource(Res.string.import_step_store))
            Spacer(Modifier.height(11.dp))
            StorePicker(selected = viewModel.store, onSelect = viewModel::selectStore)
            Spacer(Modifier.height(16.dp))
        }

        IntakeFooter(viewModel)
    }
}

@Composable
private fun PasteCard(text: String, lineCount: Int, onTextChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0x59000000))
            .border(1.dp, tokens.colors.borderStrong, shape),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                .padding(horizontal = 15.dp, vertical = 13.dp),
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
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.colors.surface)
            .border(1.dp, tokens.colors.border, RoundedCornerShape(14.dp))
            .padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
                .background(tokens.colors.accent.copy(alpha = 0.10f))
                .border(1.dp, tokens.colors.accent.copy(alpha = 0.30f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Import, null, Modifier.size(16.dp), tint = tokens.colors.accent)
        }
        Column {
            Text(stringResource(Res.string.import_share_hint_desktop), style = AppTheme.type.caption, color = tokens.colors.muted)
            Spacer(Modifier.height(3.dp))
            Text(stringResource(Res.string.import_share_hint_phone), style = AppTheme.type.caption, color = tokens.colors.faint)
        }
    }
}

@Composable
private fun IntakeFooter(viewModel: ImportViewModel) {
    val tokens = AppTheme.tokens
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp)) {
        PrimaryButton(
            text = stringResource(Res.string.import_parse_button),
            onClick = viewModel::parse,
            leadingIcon = AppIcons.Import,
            enabled = viewModel.canParse,
            modifier = Modifier.actionWidth(),
        )
        Spacer(Modifier.height(9.dp))
        val store = viewModel.store
        val footnote = when {
            viewModel.pasteText.isBlank() -> stringResource(Res.string.import_footnote_empty)
            store == null -> pluralStringResource(Res.plurals.import_footnote_pick_store, viewModel.lineCount, viewModel.lineCount)
            else -> pluralStringResource(Res.plurals.import_footnote_ready, viewModel.lineCount, viewModel.lineCount, store.label)
        }
        Text(footnote, style = AppTheme.type.caption, color = tokens.colors.faint, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
    }
}

// ---- Matching (the importer-parsing state) ------------------------------------------------------

@Composable
internal fun MatchingPhase(viewModel: ImportViewModel, modifier: Modifier) {
    val tokens = AppTheme.tokens
    val total = viewModel.matchTotal
    val done = viewModel.matchProgress
    Column(
        modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = tokens.colors.accent, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(20.dp))
        Text(stringResource(Res.string.import_matching_title), style = AppTheme.type.bodyStrong, color = tokens.colors.text)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(Res.string.import_progress, done, total), style = AppTheme.type.caption, color = tokens.colors.faint)
        Spacer(Modifier.height(18.dp))
        ProgressBar(fraction = if (total == 0) 0f else done.toFloat() / total)
        Spacer(Modifier.height(22.dp))
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
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(viewModel::backToIntake)
            Column {
                Text(stringResource(Res.string.import_review_title), style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
                Text(stringResource(Res.string.import_review_count, viewModel.candidates.size, viewModel.checkedCount), style = AppTheme.type.caption, color = tokens.colors.faint)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (viewModel.failed) {
            Notice(stringResource(Res.string.import_review_error), ErrorRed)
            Spacer(Modifier.height(6.dp))
        }

        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(viewModel.candidates) { candidate -> CandidateRow(candidate) }
                item { Spacer(Modifier.height(4.dp)) }
            }
            Box(
                Modifier.fillMaxWidth().height(12.dp).align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(0f to tokens.colors.bg, 1f to Color.Transparent)),
            )
        }

        Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp)) {
            PrimaryButton(
                text = if (viewModel.importing) stringResource(Res.string.import_adding) else stringResource(Res.string.import_add_button, viewModel.checkedCount),
                onClick = viewModel::confirm,
                leadingIcon = AppIcons.Check,
                enabled = viewModel.checkedCount > 0 && !viewModel.importing,
                modifier = Modifier.actionWidth(),
            )
        }
    }
}

@Composable
private fun CandidateRow(candidate: ImportCandidate) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.surface)
            .border(1.dp, if (candidate.checked) tokens.colors.accent.copy(alpha = 0.40f) else tokens.colors.border, shape)
            .padding(12.dp),
    ) {
        when (val classification = candidate.classification) {
            is MatchClassification.Matched -> MatchedHead(candidate, classification.result)
            is MatchClassification.Ambiguous -> AmbiguousHead(candidate, classification.results)
            MatchClassification.Unmatched -> UnmatchedHead(candidate)
        }
    }
}

@Composable
private fun MatchedHead(candidate: ImportCandidate, result: IgdbSearchResult) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CoverArt(result.name, result.coverImageId, Modifier.size(width = 44.dp, height = 58.dp), RoundedCornerShape(9.dp))
        Column(Modifier.weight(1f)) {
            MatchTag(stringResource(Res.string.import_tag_matched), tokens.colors.accent)
            Spacer(Modifier.height(4.dp))
            Text(result.name, style = AppTheme.type.bodyStrong, color = tokens.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            gameMeta(result.firstReleaseDate, result.developer)?.let {
                Text(it, style = AppTheme.type.caption, color = tokens.colors.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        CheckBox(candidate.checked) { candidate.checked = !candidate.checked }
    }
}

@Composable
private fun AmbiguousHead(candidate: ImportCandidate, options: List<IgdbSearchResult>) {
    val tokens = AppTheme.tokens
    var expanded by remember { mutableStateOf(false) }
    val limit = if (expanded) options.size else maxOf(AMBIGUOUS_INLINE_CAP, candidate.pickedIndex + 1)
    MatchTag(stringResource(Res.string.import_tag_pick), Amber)
    Spacer(Modifier.height(7.dp))
    Text("“${candidate.rawTitle}”", style = AppTheme.type.bodyStrong, color = tokens.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(2.dp))
    Text(pluralStringResource(Res.plurals.import_ambiguous_count, options.size, options.size), style = AppTheme.type.caption, color = tokens.colors.faint)
    Spacer(Modifier.height(6.dp))
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
        Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(AppIcons.ChevronRight, null, Modifier.size(14.dp), tint = tokens.colors.accent)
        Text(pluralStringResource(Res.plurals.import_show_all, total, total), style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp), color = tokens.colors.accent)
    }
}

@Composable
private fun UnmatchedHead(candidate: ImportCandidate) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(width = 44.dp, height = 58.dp).clip(RoundedCornerShape(9.dp)).background(tokens.colors.bg2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Plus, null, Modifier.size(18.dp), tint = tokens.colors.muted)
        }
        Column(Modifier.weight(1f)) {
            MatchTag(stringResource(Res.string.import_tag_unmatched), tokens.colors.muted)
            Spacer(Modifier.height(4.dp))
            Text(candidate.rawTitle, style = AppTheme.type.bodyStrong, color = tokens.colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(Res.string.import_unmatched_manual), style = AppTheme.type.caption, color = tokens.colors.faint)
        }
        CheckBox(candidate.checked) { candidate.checked = !candidate.checked }
    }
    if (candidate.alreadyInLibrary) {
        Spacer(Modifier.height(10.dp))
        SimilarTitleWarning(candidate.rawTitle)
    }
}

// ---- Done ---------------------------------------------------------------------------------------

@Composable
internal fun DonePhase(summary: ImportSummary, onDone: () -> Unit, modifier: Modifier) {
    val tokens = AppTheme.tokens
    Column(
        modifier.fillMaxSize().padding(horizontal = 28.dp),
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
        Spacer(Modifier.height(18.dp))
        Text(pluralStringResource(Res.plurals.import_done_title, summary.total, summary.total), style = AppTheme.type.display.copy(fontSize = 22.sp), color = tokens.colors.text)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(Res.string.import_done_subtitle, summary.added, summary.attached),
            style = AppTheme.type.body,
            color = tokens.colors.faint,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton(stringResource(Res.string.import_done_another), onDone, leadingIcon = AppIcons.Import)
    }
}

// ---- Shared bits --------------------------------------------------------------------------------

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
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(tokens.colors.surface)
            .border(1.dp, tokens.colors.border, RoundedCornerShape(11.dp)).clickable(onClick = onClick),
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
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun Notice(text: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp)).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(AppIcons.Close, null, Modifier.size(14.dp), tint = color)
        Text(text, style = AppTheme.type.caption, color = AppTheme.tokens.colors.muted)
    }
}

@Composable
private fun SimilarTitleWarning(existing: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Amber.copy(alpha = 0.08f))
            .border(1.dp, Amber.copy(alpha = 0.30f), RoundedCornerShape(12.dp)).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(AppIcons.Heart, null, Modifier.size(14.dp), tint = Amber)
        Text(
            stringResource(Res.string.similar_title_warning, existing),
            style = AppTheme.type.caption,
            color = AppTheme.tokens.colors.muted,
        )
    }
}

@Composable
private fun StepLabel(number: Int, text: String) {
    val tokens = AppTheme.tokens
    val accent = tokens.colors.accent
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
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
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
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
    val shape = RoundedCornerShape(12.dp)
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
                .padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
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
