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
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.battlenet_intro
import hr.kotwave.gameslibrary.resources.cd_back
import hr.kotwave.gameslibrary.resources.cd_selected
import hr.kotwave.gameslibrary.resources.error_igdb_unreachable
import hr.kotwave.gameslibrary.resources.import_add_button
import hr.kotwave.gameslibrary.importer.DonePhase
import hr.kotwave.gameslibrary.importer.ImportPhase
import hr.kotwave.gameslibrary.importer.ImportViewModel
import hr.kotwave.gameslibrary.importer.MatchingPhase
import hr.kotwave.gameslibrary.importer.ReviewPhase
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.components.actionWidth
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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

    Column(modifier.fillMaxSize().padding(horizontal = tokens.spacing.lg)) {
        Spacer(Modifier.height(tokens.spacing.md))
        Header(onBack)
        Spacer(Modifier.height(tokens.spacing.micro))
        Text(stringResource(Res.string.battlenet_intro), style = AppTheme.type.body, color = tokens.colors.faint)

        if (viewModel.failed) {
            Spacer(Modifier.height(tokens.spacing.sm))
            IgdbUnreachableNotice()
        }

        LazyColumn(Modifier.weight(1f).padding(top = tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
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
            item { Spacer(Modifier.height(tokens.spacing.micro)) }
        }

        Column(Modifier.fillMaxWidth().padding(top = tokens.spacing.sm, bottom = tokens.spacing.lg)) {
            PrimaryButton(
                text = stringResource(Res.string.import_add_button, selectedCount),
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
    val shape = RoundedCornerShape(tokens.radii.md)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        Box(
            Modifier.size(36.dp).clip(shape)
                .background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.ChevronLeft, stringResource(Res.string.cd_back), Modifier.size(18.dp), tint = tokens.colors.muted)
        }
        Text("Battle.net", style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
    }
}

@Composable
private fun SectionHeader(label: String) {
    val tokens = AppTheme.tokens
    Text(
        label.uppercase(),
        style = AppTheme.type.section.copy(fontSize = 11.sp),
        color = tokens.colors.accent,
        modifier = Modifier.padding(top = tokens.spacing.xs, start = tokens.spacing.micro, bottom = tokens.spacing.micro),
    )
}

@Composable
private fun CatalogRow(title: String, checked: Boolean, accent: Color, onToggle: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.tile)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.surface)
            .border(1.dp, if (checked) accent.copy(alpha = 0.45f) else tokens.colors.border, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Text(title, style = AppTheme.type.bodyStrong.copy(fontSize = 14.sp), color = tokens.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        CheckBox(checked, accent)
    }
}

@Composable
private fun CheckBox(checked: Boolean, accent: Color) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.sm)
    Box(
        Modifier.size(26.dp).clip(shape)
            .background(if (checked) accent.copy(alpha = 0.18f) else tokens.colors.surface)
            .border(1.dp, if (checked) accent.copy(alpha = 0.60f) else tokens.colors.borderStrong, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(AppIcons.Check, stringResource(Res.string.cd_selected), Modifier.size(16.dp), tint = accent)
    }
}

@Composable
private fun IgdbUnreachableNotice() {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(tokens.colors.error.copy(alpha = 0.10f))
            .border(1.dp, tokens.colors.error.copy(alpha = 0.35f), shape).padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Icon(AppIcons.Close, null, Modifier.size(14.dp), tint = tokens.colors.error)
        Text(stringResource(Res.string.error_igdb_unreachable), style = AppTheme.type.caption, color = tokens.colors.muted)
    }
}
