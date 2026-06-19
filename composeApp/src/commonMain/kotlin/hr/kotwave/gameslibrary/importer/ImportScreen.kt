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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.IgdbSearchResult
import hr.kotwave.gameslibrary.data.ImportSummary
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.search.IgdbResultRow
import hr.kotwave.gameslibrary.ui.components.CoverArt
import hr.kotwave.gameslibrary.ui.components.GlowBox
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.gameMeta
import hr.kotwave.gameslibrary.ui.model.glyph
import hr.kotwave.gameslibrary.ui.model.label
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

private val Amber = Color(0xFFFFD24A)
private val ErrorRed = Color(0xFFF4707A)

/**
 * The paste Import tab: Intake (paste + Store) → Matching → Review → Done. Each Candidate passes
 * through Review before anything enters the library; Confirm is additive (ADR 0006).
 */
@Composable
fun ImportScreen(modifier: Modifier = Modifier, viewModel: ImportViewModel = koinViewModel()) {
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
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("Import games", style = AppTheme.type.display, color = tokens.colors.text)
        Spacer(Modifier.height(3.dp))
        Text("Paste your list — one game per line.", style = AppTheme.type.body, color = tokens.colors.faint)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            if (viewModel.failed) {
                Notice("Couldn't reach IGDB — check your connection and try again.", ErrorRed)
                Spacer(Modifier.height(14.dp))
            }
            StepLabel(1, "Paste your list")
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
            StepLabel(2, "Which store is this for?")
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
            Text("PASTED TEXT", style = AppTheme.type.section.copy(fontSize = 11.sp), color = tokens.colors.faint)
            Spacer(Modifier.weight(1f))
            Text("$lineCount lines", style = AppTheme.type.caption, color = tokens.colors.muted)
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
                    "Persona 5 Royal\nGod of War Ragnarök\nGhost of Tsushima…",
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
            Text("On desktop, drop a .txt or .csv file here.", style = AppTheme.type.caption, color = tokens.colors.muted)
            Spacer(Modifier.height(3.dp))
            Text("On your phone: Share → GamesLibrary (coming soon).", style = AppTheme.type.caption, color = tokens.colors.faint)
        }
    }
}

@Composable
private fun IntakeFooter(viewModel: ImportViewModel) {
    val tokens = AppTheme.tokens
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp)) {
        PrimaryButton(
            text = "Parse list · Review games",
            onClick = viewModel::parse,
            leadingIcon = AppIcons.Import,
            enabled = viewModel.canParse,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(9.dp))
        val store = viewModel.store
        val footnote = when {
            viewModel.pasteText.isBlank() -> "Paste a list to begin."
            store == null -> "${viewModel.lineCount} lines · pick a store below."
            else -> "${viewModel.lineCount} lines · matched against IGDB, assigned to ${store.label}."
        }
        Text(footnote, style = AppTheme.type.caption, color = tokens.colors.faint, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
    }
}

// ---- Matching (the importer-parsing state) ------------------------------------------------------

@Composable
private fun MatchingPhase(viewModel: ImportViewModel, modifier: Modifier) {
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
        Text("Matching against IGDB", style = AppTheme.type.bodyStrong, color = tokens.colors.text)
        Spacer(Modifier.height(6.dp))
        Text("$done of $total", style = AppTheme.type.caption, color = tokens.colors.faint)
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
private fun ReviewPhase(viewModel: ImportViewModel, modifier: Modifier) {
    val tokens = AppTheme.tokens
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(viewModel::backToIntake)
            Column {
                Text("Review games", style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
                Text("${viewModel.candidates.size} found · ${viewModel.checkedCount} selected", style = AppTheme.type.caption, color = tokens.colors.faint)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (viewModel.failed) {
            Notice("Couldn't reach IGDB — your selections are kept. Try Confirm again.", ErrorRed)
            Spacer(Modifier.height(6.dp))
        }

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
            MatchTag("Matched", tokens.colors.accent)
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
    MatchTag("Pick a match", Amber)
    Spacer(Modifier.height(7.dp))
    Text("“${candidate.rawTitle}”", style = AppTheme.type.bodyStrong, color = tokens.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(2.dp))
    Text("${options.size} possible matches — pick the right one.", style = AppTheme.type.caption, color = tokens.colors.faint)
    Spacer(Modifier.height(6.dp))
    options.forEachIndexed { index, option ->
        IgdbResultRow(result = option, selected = candidate.pickedIndex == index, onClick = { candidate.pick(index) })
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
            MatchTag("No IGDB match", tokens.colors.muted)
            Spacer(Modifier.height(4.dp))
            Text(candidate.rawTitle, style = AppTheme.type.bodyStrong, color = tokens.colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Adds as a manual game.", style = AppTheme.type.caption, color = tokens.colors.faint)
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
private fun DonePhase(summary: ImportSummary, onDone: () -> Unit, modifier: Modifier) {
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
        Text("Imported ${summary.total} games", style = AppTheme.type.display.copy(fontSize = 22.sp), color = tokens.colors.text)
        Spacer(Modifier.height(6.dp))
        Text(
            "${summary.added} added · ${summary.attached} already in your library.",
            style = AppTheme.type.body,
            color = tokens.colors.faint,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Import another", onDone, leadingIcon = AppIcons.Import)
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
        if (checked) Icon(AppIcons.Check, "Selected", Modifier.size(16.dp), tint = tokens.colors.accent)
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
        Icon(AppIcons.ChevronLeft, "Back", Modifier.size(18.dp), tint = tokens.colors.muted)
    }
}

@Composable
private fun CancelAction(onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    Text(
        "Cancel",
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
            "“$existing” is already in your library — you can add it anyway.",
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
        Store.entries.forEach { store ->
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
