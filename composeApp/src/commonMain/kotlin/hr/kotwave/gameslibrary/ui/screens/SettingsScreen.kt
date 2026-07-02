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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.Game
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.cd_dismiss
import hr.kotwave.gameslibrary.resources.settings_about_subtitle
import hr.kotwave.gameslibrary.resources.settings_about_title
import hr.kotwave.gameslibrary.resources.settings_account_sync
import hr.kotwave.gameslibrary.resources.settings_battlenet_subtitle
import hr.kotwave.gameslibrary.resources.settings_coming_soon
import hr.kotwave.gameslibrary.resources.settings_connect_subtitle
import hr.kotwave.gameslibrary.resources.settings_credits_subtitle
import hr.kotwave.gameslibrary.resources.settings_credits_title
import hr.kotwave.gameslibrary.resources.settings_export_cancelled
import hr.kotwave.gameslibrary.resources.settings_export_ok
import hr.kotwave.gameslibrary.resources.settings_export_subtitle
import hr.kotwave.gameslibrary.resources.settings_export_title
import hr.kotwave.gameslibrary.resources.settings_gallery_subtitle
import hr.kotwave.gameslibrary.resources.settings_gallery_title
import hr.kotwave.gameslibrary.resources.settings_import_subtitle
import hr.kotwave.gameslibrary.resources.settings_import_title
import hr.kotwave.gameslibrary.resources.settings_orphaned_body
import hr.kotwave.gameslibrary.resources.settings_orphaned_title
import hr.kotwave.gameslibrary.resources.settings_paste_subtitle
import hr.kotwave.gameslibrary.resources.settings_paste_title
import hr.kotwave.gameslibrary.resources.settings_rematch_all
import hr.kotwave.gameslibrary.resources.settings_rematching
import hr.kotwave.gameslibrary.resources.settings_section_about
import hr.kotwave.gameslibrary.resources.settings_section_connections
import hr.kotwave.gameslibrary.resources.settings_section_developer
import hr.kotwave.gameslibrary.resources.settings_section_library
import hr.kotwave.gameslibrary.resources.settings_section_maintenance
import hr.kotwave.gameslibrary.resources.settings_title
import hr.kotwave.gameslibrary.settings.SettingsViewModel
import hr.kotwave.gameslibrary.transfer.LIBRARY_EXPORT_FILENAME
import hr.kotwave.gameslibrary.transfer.LibraryTransferViewModel
import hr.kotwave.gameslibrary.transfer.rememberLibraryFileIo
import hr.kotwave.gameslibrary.ui.components.SecondaryButton
import hr.kotwave.gameslibrary.ui.components.actionWidth
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.glyph
import hr.kotwave.gameslibrary.ui.model.label
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val OrphanRed = Color(0xFFF4707A)
private val ExportGreen = Color(0xFF7DF0B6)

/** Stores with no sync yet — shown as disabled "Coming soon" connections (06-settings.html). */
private val COMING_SOON_STORES = listOf(Store.PSN, Store.XBOX, Store.NINTENDO)

/** Settings: connections, library export/import, about, plus Orphaned maintenance and the dev gallery. */
@Composable
fun SettingsScreen(
    onOpenGallery: () -> Unit,
    onOpenGame: (Long) -> Unit,
    onOpenSteam: () -> Unit,
    onOpenGog: () -> Unit,
    onOpenBattleNet: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenPasteImport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
    transfer: LibraryTransferViewModel = koinViewModel(),
) {
    val tokens = AppTheme.tokens
    val orphaned by viewModel.orphanedGames.collectAsState()
    val fileIo = rememberLibraryFileIo()
    val scope = rememberCoroutineScope()
    var exportNote by remember { mutableStateOf<String?>(null) }
    val exportedMsg = stringResource(Res.string.settings_export_ok, LIBRARY_EXPORT_FILENAME)
    val cancelledMsg = stringResource(Res.string.settings_export_cancelled)

    val onExport = {
        scope.launch {
            val json = transfer.buildExport()
            fileIo.export(LIBRARY_EXPORT_FILENAME, json) { ok ->
                exportNote = if (ok) exportedMsg else cancelledMsg
            }
        }
        Unit
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(stringResource(Res.string.settings_title), style = AppTheme.type.display, color = tokens.colors.text)

        exportNote?.let { note ->
            Spacer(Modifier.height(14.dp))
            ExportNotice(note) { exportNote = null }
        }

        if (orphaned.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(Res.string.settings_section_maintenance))
            OrphanedSection(
                games = orphaned,
                retrying = viewModel.retrying,
                onRetryAll = viewModel::retryAllOrphaned,
                onOpenGame = onOpenGame,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(Res.string.settings_section_connections))
        SettingsCard {
            SettingsItem(
                icon = AppIcons.Steam,
                iconTint = tokens.store.glyph(Store.STEAM),
                title = "Steam",
                subtitle = stringResource(Res.string.settings_connect_subtitle),
                onClick = onOpenSteam,
            )
            HairlineDivider()
            StoreConnectionItem(
                store = Store.GOG,
                subtitle = stringResource(Res.string.settings_connect_subtitle),
                onClick = onOpenGog,
            )
            HairlineDivider()
            StoreConnectionItem(
                store = Store.BATTLE_NET,
                subtitle = stringResource(Res.string.settings_battlenet_subtitle),
                onClick = onOpenBattleNet,
            )
            COMING_SOON_STORES.forEach { store ->
                HairlineDivider()
                ComingSoonItem(store)
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(Res.string.settings_section_library))
        SettingsCard {
            SettingsItem(
                icon = AppIcons.Export,
                iconTint = ExportGreen,
                title = stringResource(Res.string.settings_export_title),
                subtitle = stringResource(Res.string.settings_export_subtitle, LIBRARY_EXPORT_FILENAME),
                onClick = onExport,
            )
            HairlineDivider()
            SettingsItem(
                icon = AppIcons.ImportFile,
                iconTint = tokens.colors.accent,
                title = stringResource(Res.string.settings_import_title),
                subtitle = stringResource(Res.string.settings_import_subtitle, LIBRARY_EXPORT_FILENAME),
                onClick = onOpenImport,
            )
            HairlineDivider()
            SettingsItem(
                icon = AppIcons.Import,
                iconTint = tokens.colors.muted,
                title = stringResource(Res.string.settings_paste_title),
                subtitle = stringResource(Res.string.settings_paste_subtitle),
                onClick = onOpenPasteImport,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(Res.string.settings_section_about))
        SettingsCard {
            StaticItem(
                icon = AppIcons.Settings,
                title = stringResource(Res.string.settings_about_title),
                subtitle = stringResource(Res.string.settings_about_subtitle),
            )
            HairlineDivider()
            StaticItem(
                icon = AppIcons.Sliders,
                title = stringResource(Res.string.settings_credits_title),
                subtitle = stringResource(Res.string.settings_credits_subtitle),
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(Res.string.settings_section_developer))
        SettingsCard {
            SettingsItem(
                icon = AppIcons.Grid,
                iconTint = tokens.colors.muted,
                title = stringResource(Res.string.settings_gallery_title),
                subtitle = stringResource(Res.string.settings_gallery_subtitle),
                onClick = onOpenGallery,
            )
        }
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

/** A grouped card holding [SettingsItem]s separated by [HairlineDivider]s (the mockup's `.card`). */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape),
    ) {
        content()
    }
}

@Composable
private fun HairlineDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(AppTheme.tokens.colors.border))
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        ItemIcon(icon, iconTint)
        ItemText(title, subtitle, Modifier.weight(1f))
        Icon(AppIcons.ChevronRight, null, Modifier.size(17.dp), tint = AppTheme.tokens.colors.faint)
    }
}

/** A non-navigating info row (no chevron) — the About section. */
@Composable
private fun StaticItem(icon: ImageVector, title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        ItemIcon(icon, AppTheme.tokens.colors.muted)
        ItemText(title, subtitle, Modifier.weight(1f))
    }
}

/** A navigating connection row badged with the store's glyph (for stores without a brand icon). */
@Composable
private fun StoreConnectionItem(store: Store, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        GlyphIcon(store)
        ItemText(store.label, subtitle, Modifier.weight(1f))
        Icon(AppIcons.ChevronRight, null, Modifier.size(17.dp), tint = AppTheme.tokens.colors.faint)
    }
}

/** A disabled connection row with a "Coming soon" pill (06-settings.html). */
@Composable
private fun ComingSoonItem(store: Store) {
    val tokens = AppTheme.tokens
    Row(
        Modifier.fillMaxWidth().padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        GlyphIcon(store)
        ItemText(store.label, stringResource(Res.string.settings_account_sync), Modifier.weight(1f))
        Text(
            stringResource(Res.string.settings_coming_soon),
            style = AppTheme.type.caption.copy(fontSize = 10.5.sp),
            color = tokens.colors.faint,
            modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(tokens.colors.surfaceRaised)
                .border(1.dp, tokens.colors.border, RoundedCornerShape(7.dp)).padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ItemIcon(icon: ImageVector, tint: Color) {
    val tokens = AppTheme.tokens
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(tokens.colors.surfaceRaised)
            .border(1.dp, tokens.colors.border, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
    }
}

@Composable
private fun GlyphIcon(store: Store) {
    val tokens = AppTheme.tokens
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(tokens.colors.surfaceRaised)
            .border(1.dp, tokens.colors.border, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(store.glyph, style = AppTheme.type.brand.copy(fontSize = 14.sp), color = tokens.store.glyph(store))
    }
}

@Composable
private fun ItemText(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val tokens = AppTheme.tokens
    Column(modifier) {
        Text(title, style = AppTheme.type.bodyStrong, color = tokens.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = AppTheme.type.caption, color = tokens.colors.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ExportNotice(text: String, onDismiss: () -> Unit) {
    val tokens = AppTheme.tokens
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ExportGreen.copy(alpha = 0.08f))
            .border(1.dp, ExportGreen.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .clickable(onClick = onDismiss).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(AppIcons.Check, null, Modifier.size(14.dp), tint = ExportGreen)
        Text(text, style = AppTheme.type.caption, color = tokens.colors.muted, modifier = Modifier.weight(1f))
        Icon(AppIcons.Close, stringResource(Res.string.cd_dismiss), Modifier.size(13.dp), tint = tokens.colors.faint)
    }
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
                Text(pluralStringResource(Res.plurals.settings_orphaned_title, games.size, games.size), style = AppTheme.type.bodyStrong, color = tokens.colors.text)
                Text(
                    stringResource(Res.string.settings_orphaned_body),
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
            text = if (retrying) stringResource(Res.string.settings_rematching) else stringResource(Res.string.settings_rematch_all),
            onClick = onRetryAll,
            leadingIcon = AppIcons.Sync,
            enabled = !retrying,
            modifier = Modifier.actionWidth(),
        )
    }
}
