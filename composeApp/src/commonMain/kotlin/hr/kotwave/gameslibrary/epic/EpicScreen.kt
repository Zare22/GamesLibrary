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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import hr.kotwave.gameslibrary.store.ConnectedStoreCard
import hr.kotwave.gameslibrary.store.ConnectingRow
import hr.kotwave.gameslibrary.store.StoreBrand
import hr.kotwave.gameslibrary.store.StoreConnectPrompt
import hr.kotwave.gameslibrary.ui.components.GlassSurface
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
            val stage = viewModel.syncFailure
            ConnectedStoreCard(
                viewModel = viewModel,
                brand = StoreBrand(
                    accent = epic,
                    glyph = Store.EPIC.glyph,
                    avatarDark = Color(0xFF15171D),
                    avatarLight = Color(0xFF2A2E38),
                ),
                accountLabel = stringResource(Res.string.epic_account),
                ownedCountLabel = { count -> pluralStringResource(Res.plurals.epic_owned_count, count, count) },
                syncFailureMessage = if (stage != null) stage.message() else null,
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
                    val failure = state as? EpicConnectState.Failed
                    StoreConnectPrompt(
                        note = stringResource(Res.string.epic_connect_note),
                        connectLabel = stringResource(Res.string.epic_connect),
                        onConnect = viewModel::connect,
                        failureMessage = if (failure != null) failure.reason.message() else null,
                    )
                }
            }
        }
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
