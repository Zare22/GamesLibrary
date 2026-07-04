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
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.xl),
    ) {
        Text(stringResource(Res.string.settings_title), style = AppTheme.type.display, color = tokens.colors.text)

        exportNote?.let { note ->
            Spacer(Modifier.height(tokens.spacing.md))
            ExportNotice(note) { exportNote = null }
        }

        if (orphaned.isNotEmpty()) {
            Spacer(Modifier.height(tokens.spacing.lg))
            SectionLabel(stringResource(Res.string.settings_section_maintenance))
            OrphanedSection(
                games = orphaned,
                retrying = viewModel.retrying,
                onRetryAll = viewModel::retryAllOrphaned,
                onOpenGame = onOpenGame,
            )
        }

        Spacer(Modifier.height(tokens.spacing.lg))
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

        Spacer(Modifier.height(tokens.spacing.lg))
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

        Spacer(Modifier.height(tokens.spacing.lg))
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

        Spacer(Modifier.height(tokens.spacing.lg))
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
    val tokens = AppTheme.tokens
    Text(
        text,
        style = AppTheme.type.section,
        color = tokens.colors.faint,
        modifier = Modifier.padding(start = tokens.spacing.micro, bottom = tokens.spacing.sm),
    )
}

/** A grouped card holding [SettingsItem]s separated by [HairlineDivider]s (the mockup's `.card`). */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.lg)
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
    val tokens = AppTheme.tokens
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(tokens.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        ItemIcon(icon, iconTint)
        ItemText(title, subtitle, Modifier.weight(1f))
        Icon(AppIcons.ChevronRight, null, Modifier.size(17.dp), tint = tokens.colors.faint)
    }
}

/** A non-navigating info row (no chevron) — the About section. */
@Composable
private fun StaticItem(icon: ImageVector, title: String, subtitle: String) {
    val tokens = AppTheme.tokens
    Row(
        Modifier.fillMaxWidth().padding(tokens.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        ItemIcon(icon, tokens.colors.muted)
        ItemText(title, subtitle, Modifier.weight(1f))
    }
}

/** A navigating connection row badged with the store's glyph (for stores without a brand icon). */
@Composable
private fun StoreConnectionItem(store: Store, subtitle: String, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(tokens.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        GlyphIcon(store)
        ItemText(store.label, subtitle, Modifier.weight(1f))
        Icon(AppIcons.ChevronRight, null, Modifier.size(17.dp), tint = tokens.colors.faint)
    }
}

/** A disabled connection row with a "Coming soon" pill (06-settings.html). */
@Composable
private fun ComingSoonItem(store: Store) {
    val tokens = AppTheme.tokens
    val pillShape = RoundedCornerShape(tokens.radii.sm)
    Row(
        Modifier.fillMaxWidth().padding(tokens.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        GlyphIcon(store)
        ItemText(store.label, stringResource(Res.string.settings_account_sync), Modifier.weight(1f))
        Text(
            stringResource(Res.string.settings_coming_soon),
            style = AppTheme.type.caption.copy(fontSize = 10.5.sp),
            color = tokens.colors.faint,
            modifier = Modifier.clip(pillShape).background(tokens.colors.surfaceRaised)
                .border(1.dp, tokens.colors.border, pillShape).padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.micro),
        )
    }
}

@Composable
private fun ItemIcon(icon: ImageVector, tint: Color) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Box(
        Modifier.size(36.dp).clip(shape).background(tokens.colors.surfaceRaised)
            .border(1.dp, tokens.colors.border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
    }
}

@Composable
private fun GlyphIcon(store: Store) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Box(
        Modifier.size(36.dp).clip(shape).background(tokens.colors.surfaceRaised)
            .border(1.dp, tokens.colors.border, shape),
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
    val shape = RoundedCornerShape(tokens.radii.md)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(ExportGreen.copy(alpha = 0.08f))
            .border(1.dp, ExportGreen.copy(alpha = 0.30f), shape)
            .clickable(onClick = onDismiss).padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
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
    val shape = RoundedCornerShape(tokens.radii.lg)
    val rowShape = RoundedCornerShape(tokens.radii.md)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(OrphanRed.copy(alpha = 0.06f))
            .border(1.dp, OrphanRed.copy(alpha = 0.30f), shape)
            .padding(tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            Box(
                Modifier.size(36.dp).clip(rowShape)
                    .background(OrphanRed.copy(alpha = 0.14f)).border(1.dp, OrphanRed.copy(alpha = 0.40f), rowShape),
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
                Modifier.fillMaxWidth().clip(rowShape)
                    .background(tokens.colors.surface).border(1.dp, tokens.colors.border, rowShape)
                    .clickable { onOpenGame(game.id) }.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
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
