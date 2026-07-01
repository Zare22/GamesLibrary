package hr.kotwave.gameslibrary.battlenet

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.importer.DonePhase
import hr.kotwave.gameslibrary.importer.ImportPhase
import hr.kotwave.gameslibrary.importer.ImportViewModel
import hr.kotwave.gameslibrary.importer.MatchingPhase
import hr.kotwave.gameslibrary.importer.ReviewPhase
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.components.actionWidth
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

private val ErrorRed = Color(0xFFF4707A)

/**
 * The Battle.net picker: tick owned titles from the fixed [BattleNetCatalog], then the same Matching →
 * Review → Confirm funnel as a paste Import (Store.BATTLE_NET, additive). Drives its own
 * [ImportViewModel] instance: the catalog stands in for the paste Intake, the rest of the funnel is
 * the reused Import screens.
 */
@Composable
fun BattleNetScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = koinViewModel(),
) {
    when (val phase = viewModel.phase) {
        ImportPhase.Intake -> CatalogPhase(viewModel, onBack, modifier)
        ImportPhase.Matching -> MatchingPhase(viewModel, modifier)
        ImportPhase.Review -> ReviewPhase(viewModel, modifier)
        is ImportPhase.Done -> DonePhase(phase.summary, viewModel::reset, modifier)
    }
}

@Composable
private fun CatalogPhase(viewModel: ImportViewModel, onBack: () -> Unit, modifier: Modifier) {
    val tokens = AppTheme.tokens
    val accent = tokens.store.accent(Store.BATTLE_NET)
    val checked = remember { mutableStateMapOf<String, Boolean>() }
    val selectedCount = checked.count { it.value }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Header(onBack)
        Spacer(Modifier.height(4.dp))
        Text("Tick the games you own — they’re matched against IGDB before anything is added.", style = AppTheme.type.body, color = tokens.colors.faint)

        if (viewModel.failed) {
            Spacer(Modifier.height(12.dp))
            IgdbUnreachableNotice()
        }

        LazyColumn(Modifier.weight(1f).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BattleNetCatalog.sections.forEach { section ->
                item(key = "section:${section.label}") { SectionHeader(section.label) }
                items(section.titles, key = { it }) { title ->
                    CatalogRow(
                        title = title,
                        checked = checked[title] == true,
                        accent = accent,
                        onToggle = { checked[title] = checked[title] != true },
                    )
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }

        Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp)) {
            PrimaryButton(
                text = "Add $selectedCount to library",
                onClick = { viewModel.startFromTitles(Store.BATTLE_NET, BattleNetCatalog.titles.filter { checked[it] == true }) },
                leadingIcon = AppIcons.Check,
                enabled = selectedCount > 0,
                modifier = Modifier.actionWidth(),
            )
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                .background(tokens.colors.surface).border(1.dp, tokens.colors.border, RoundedCornerShape(11.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.ChevronLeft, "Back", Modifier.size(18.dp), tint = tokens.colors.muted)
        }
        Text("Battle.net", style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label.uppercase(),
        style = AppTheme.type.section.copy(fontSize = 11.sp),
        color = AppTheme.tokens.colors.accent,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun CatalogRow(title: String, checked: Boolean, accent: Color, onToggle: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.surface)
            .border(1.dp, if (checked) accent.copy(alpha = 0.45f) else tokens.colors.border, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = AppTheme.type.bodyStrong.copy(fontSize = 14.sp), color = tokens.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        CheckBox(checked, accent)
    }
}

@Composable
private fun CheckBox(checked: Boolean, accent: Color) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.size(26.dp).clip(shape)
            .background(if (checked) accent.copy(alpha = 0.18f) else tokens.colors.surface)
            .border(1.dp, if (checked) accent.copy(alpha = 0.60f) else tokens.colors.borderStrong, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(AppIcons.Check, "Selected", Modifier.size(16.dp), tint = accent)
    }
}

@Composable
private fun IgdbUnreachableNotice() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ErrorRed.copy(alpha = 0.10f))
            .border(1.dp, ErrorRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp)).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(AppIcons.Close, null, Modifier.size(14.dp), tint = ErrorRed)
        Text("Couldn’t reach IGDB — check your connection and try again.", style = AppTheme.type.caption, color = AppTheme.tokens.colors.muted)
    }
}
