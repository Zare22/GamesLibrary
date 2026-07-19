package hr.kotwave.gameslibrary.epic

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.importer.ImportPhase
import hr.kotwave.gameslibrary.importer.ImportViewModel
import hr.kotwave.gameslibrary.importer.MatchingPhase
import hr.kotwave.gameslibrary.importer.ReviewPhase
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.cd_back
import hr.kotwave.gameslibrary.resources.common_cancel
import hr.kotwave.gameslibrary.resources.error_igdb_unreachable
import hr.kotwave.gameslibrary.resources.epic_account
import hr.kotwave.gameslibrary.resources.epic_connect
import hr.kotwave.gameslibrary.resources.epic_connect_note
import hr.kotwave.gameslibrary.resources.epic_fail_network
import hr.kotwave.gameslibrary.resources.epic_fail_paste
import hr.kotwave.gameslibrary.resources.epic_fail_rejected
import hr.kotwave.gameslibrary.resources.epic_finishing
import hr.kotwave.gameslibrary.resources.epic_hero_connect_body
import hr.kotwave.gameslibrary.resources.epic_hero_connect_title
import hr.kotwave.gameslibrary.resources.epic_hero_connected_body
import hr.kotwave.gameslibrary.resources.epic_hero_connected_title
import hr.kotwave.gameslibrary.resources.epic_owned_count
import hr.kotwave.gameslibrary.resources.epic_sync_fail_catalog
import hr.kotwave.gameslibrary.resources.epic_sync_fail_epic
import hr.kotwave.gameslibrary.resources.epic_sync_fail_igdb
import hr.kotwave.gameslibrary.resources.epic_sync_fail_merge
import hr.kotwave.gameslibrary.resources.epic_sync_fail_token
import hr.kotwave.gameslibrary.resources.store_connected
import hr.kotwave.gameslibrary.resources.store_disconnect
import hr.kotwave.gameslibrary.resources.store_sync_now
import hr.kotwave.gameslibrary.resources.store_sync_prompt
import hr.kotwave.gameslibrary.resources.store_syncing
import hr.kotwave.gameslibrary.resources.sync_stat_added
import hr.kotwave.gameslibrary.resources.sync_stat_already
import hr.kotwave.gameslibrary.resources.sync_stat_review
import hr.kotwave.gameslibrary.resources.sync_stat_synced
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.components.actionWidth
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.glyph
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Epic screen: connect an Epic Games account (authorizationCode paste), then additively sync
 * the owned library incl. free claims. The sync's id-unmatched tail goes through the Import funnel's
 * Matching → Review legs in place (its own [ImportViewModel], the Battle.net pattern). The user's own
 * token authorizes the pull; the paste parser stays available as the fallback intake.
 */
@Composable
fun EpicScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EpicViewModel = koinViewModel(),
    importViewModel: ImportViewModel = koinViewModel(),
) {
    importViewModel.syncOutcome?.let { outcome ->
        LaunchedEffect(outcome) {
            viewModel.absorbReview(outcome)
            importViewModel.consumeSyncOutcome()
        }
    }
    when (importViewModel.phase) {
        ImportPhase.Matching -> {
            MatchingPhase(importViewModel, modifier)
            return
        }
        ImportPhase.Review -> {
            ReviewPhase(importViewModel, modifier)
            return
        }
        else -> Unit
    }
    val tokens = AppTheme.tokens
    val epic = tokens.store.epic
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
    ) {
        Header(onBack)
        Spacer(Modifier.height(tokens.spacing.md))
        Hero(connected = viewModel.connected, epic = epic)

        Spacer(Modifier.height(tokens.spacing.md))
        if (viewModel.connected) {
            ConnectedCard(
                viewModel = viewModel,
                epic = epic,
                reviewFailed = importViewModel.failed,
                onReview = { importViewModel.startFromSyncTail(Store.EPIC, viewModel.reviewTail) },
            )
        } else {
            ConnectSection(viewModel = viewModel, epic = epic)
        }

        Spacer(Modifier.height(tokens.spacing.lg))
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
        Text("Epic Games", style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
    }
}

@Composable
private fun Hero(connected: Boolean, epic: Color) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.xl)
    val iconShape = RoundedCornerShape(tokens.radii.xl)
    Box(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1E26), Color(0xFF0E1015))))
            .border(1.dp, epic.copy(alpha = 0.25f), shape)
            .padding(horizontal = tokens.spacing.xl, vertical = tokens.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(70.dp).clip(iconShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2A2E38), Color(0xFF15171D))))
                    .border(1.dp, epic.copy(alpha = 0.40f), iconShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(Store.EPIC.glyph, style = AppTheme.type.brand.copy(fontSize = 30.sp), color = AppTheme.tokens.store.glyph(Store.EPIC))
            }
            Spacer(Modifier.height(tokens.spacing.md))
            Text(
                if (connected) stringResource(Res.string.epic_hero_connected_title) else stringResource(Res.string.epic_hero_connect_title),
                style = AppTheme.type.display.copy(fontSize = 22.sp),
                color = tokens.colors.text,
            )
            Spacer(Modifier.height(tokens.spacing.xs))
            Text(
                if (connected) {
                    stringResource(Res.string.epic_hero_connected_body)
                } else {
                    stringResource(Res.string.epic_hero_connect_body)
                },
                style = AppTheme.type.body.copy(fontSize = 13.5.sp),
                color = tokens.colors.muted,
            )
        }
    }
}

@Composable
private fun ConnectSection(viewModel: EpicViewModel, epic: Color) {
    val tokens = AppTheme.tokens
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(tokens.radii.xl)) {
        Column(Modifier.fillMaxWidth().padding(tokens.spacing.md)) {
            when (val state = viewModel.connectState) {
                EpicConnectState.AwaitingCode -> {
                    EpicConnectCapture(
                        signInUrl = viewModel.signInUrl,
                        onCode = viewModel::onCodeCaptured,
                    )
                    Spacer(Modifier.height(tokens.spacing.sm))
                    Text(
                        stringResource(Res.string.common_cancel),
                        style = AppTheme.type.caption,
                        color = tokens.colors.faint,
                        modifier = Modifier.clickable(onClick = viewModel::cancelConnect),
                    )
                }
                EpicConnectState.Exchanging -> ConnectingRow(epic, stringResource(Res.string.epic_finishing))
                else -> {
                    Text(
                        stringResource(Res.string.epic_connect_note),
                        style = AppTheme.type.caption.copy(fontSize = 11.5.sp),
                        color = tokens.colors.faint,
                    )
                    Spacer(Modifier.height(tokens.spacing.md))
                    PrimaryButton(
                        text = stringResource(Res.string.epic_connect),
                        onClick = viewModel::connect,
                        modifier = Modifier.actionWidth(),
                    )
                    if (state is EpicConnectState.Failed) {
                        Spacer(Modifier.height(tokens.spacing.sm))
                        Text(state.reason.message(), style = AppTheme.type.caption, color = tokens.colors.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectingRow(epic: Color, label: String) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        CircularProgressIndicator(Modifier.size(18.dp), color = epic, strokeWidth = 2.dp)
        Text(label, style = AppTheme.type.body.copy(fontSize = 13.sp), color = tokens.colors.muted)
    }
}

@Composable
private fun EpicConnectFailure.message(): String = when (this) {
    EpicConnectFailure.Paste -> stringResource(Res.string.epic_fail_paste)
    EpicConnectFailure.Rejected -> stringResource(Res.string.epic_fail_rejected)
    EpicConnectFailure.Network -> stringResource(Res.string.epic_fail_network)
}

@Composable
private fun EpicSyncFailure.message(): String = when (this) {
    EpicSyncFailure.TokenRefresh -> stringResource(Res.string.epic_sync_fail_token)
    EpicSyncFailure.EpicFetch -> stringResource(Res.string.epic_sync_fail_epic)
    EpicSyncFailure.CatalogResolve -> stringResource(Res.string.epic_sync_fail_catalog)
    EpicSyncFailure.IgdbMatch -> stringResource(Res.string.epic_sync_fail_igdb)
    EpicSyncFailure.Merge -> stringResource(Res.string.epic_sync_fail_merge)
}

@Composable
private fun ConnectedCard(viewModel: EpicViewModel, epic: Color, reviewFailed: Boolean, onReview: () -> Unit) {
    val tokens = AppTheme.tokens
    val ok = tokens.status.playing
    val avatarShape = RoundedCornerShape(tokens.radii.tile)
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(tokens.radii.xl)) {
        Column(Modifier.fillMaxWidth().padding(tokens.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Box(
                    Modifier.size(46.dp).clip(avatarShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF2A2E38), Color(0xFF15171D))))
                        .border(1.dp, epic.copy(alpha = 0.40f), avatarShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(Store.EPIC.glyph, style = AppTheme.type.brand.copy(fontSize = 20.sp), color = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.epic_account), style = AppTheme.type.bodyStrong, color = tokens.colors.text)
                    Text(stringResource(Res.string.store_connected), style = AppTheme.type.caption, color = tokens.colors.faint)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(ok))
                    Text(stringResource(Res.string.store_connected), style = AppTheme.type.caption.copy(fontSize = 11.5.sp), color = ok)
                }
            }

            Divider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val count = viewModel.ownedCount
                    Text(
                        if (count != null) pluralStringResource(Res.plurals.epic_owned_count, count, count) else stringResource(Res.string.store_sync_prompt),
                        style = AppTheme.type.body.copy(fontSize = 12.5.sp),
                        color = tokens.colors.muted,
                    )
                }
                SyncButton(syncing = viewModel.syncing, epic = epic, onClick = viewModel::sync)
            }

            viewModel.lastSummary?.let { summary ->
                Divider()
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.lg)) {
                    SyncStat(summary.added.toString(), stringResource(Res.string.sync_stat_added), tokens.status.playing)
                    SyncStat(summary.updated.toString(), stringResource(Res.string.sync_stat_already), tokens.colors.text)
                    SyncStat(summary.total.toString(), stringResource(Res.string.sync_stat_synced), tokens.colors.text)
                    if (viewModel.reviewTail.isNotEmpty()) {
                        ReviewStat(viewModel.reviewTail.size, enabled = !viewModel.syncing, onClick = onReview)
                    }
                }
            }

            viewModel.syncFailure?.let { failure ->
                Spacer(Modifier.height(tokens.spacing.sm))
                Text(
                    failure.message(),
                    style = AppTheme.type.caption,
                    color = tokens.colors.error,
                )
            }

            if (reviewFailed) {
                Spacer(Modifier.height(tokens.spacing.sm))
                Text(
                    stringResource(Res.string.error_igdb_unreachable),
                    style = AppTheme.type.caption,
                    color = tokens.colors.error,
                )
            }

            Spacer(Modifier.height(tokens.spacing.md))
            Text(
                stringResource(Res.string.store_disconnect),
                style = AppTheme.type.caption,
                color = tokens.colors.faint,
                modifier = Modifier.clickable(onClick = viewModel::disconnect),
            )
        }
    }
}

@Composable
private fun SyncButton(syncing: Boolean, epic: Color, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Row(
        Modifier.clip(shape)
            .background(epic.copy(alpha = 0.12f))
            .border(1.dp, epic.copy(alpha = 0.45f), shape)
            .clickable(enabled = !syncing, onClick = onClick)
            .padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Icon(AppIcons.Sync, null, Modifier.size(15.dp), tint = AppTheme.tokens.store.glyph(Store.EPIC))
        Text(
            if (syncing) stringResource(Res.string.store_syncing) else stringResource(Res.string.store_sync_now),
            style = AppTheme.type.button.copy(fontSize = 13.5.sp),
            color = AppTheme.tokens.store.glyph(Store.EPIC),
        )
    }
}

@Composable
private fun SyncStat(value: String, label: String, color: Color) {
    Column {
        Text(value, style = AppTheme.type.numeric.copy(fontSize = 18.sp), color = color)
        Text(label, style = AppTheme.type.caption.copy(fontSize = 10.sp), color = AppTheme.tokens.colors.faint)
    }
}

@Composable
private fun ReviewStat(count: Int, enabled: Boolean, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(
        Modifier.clip(RoundedCornerShape(tokens.radii.sm))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = tokens.spacing.micro),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.micro)) {
            Text("$count", style = AppTheme.type.numeric.copy(fontSize = 18.sp), color = tokens.colors.warning)
            Icon(AppIcons.ChevronRight, null, Modifier.size(14.dp), tint = tokens.colors.warning)
        }
        Text(stringResource(Res.string.sync_stat_review), style = AppTheme.type.caption.copy(fontSize = 10.sp), color = AppTheme.tokens.colors.faint)
    }
}

@Composable
private fun Divider() {
    val tokens = AppTheme.tokens
    Spacer(Modifier.height(tokens.spacing.md))
    Box(Modifier.fillMaxWidth().height(1.dp).background(tokens.colors.border))
    Spacer(Modifier.height(tokens.spacing.md))
}
