package hr.kotwave.gameslibrary.psn

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
import hr.kotwave.gameslibrary.resources.psn_account
import hr.kotwave.gameslibrary.resources.psn_connect
import hr.kotwave.gameslibrary.resources.psn_connect_note
import hr.kotwave.gameslibrary.resources.psn_fail_network
import hr.kotwave.gameslibrary.resources.psn_fail_paste
import hr.kotwave.gameslibrary.resources.psn_fail_rejected
import hr.kotwave.gameslibrary.resources.psn_finishing
import hr.kotwave.gameslibrary.resources.psn_hero_connect_body
import hr.kotwave.gameslibrary.resources.psn_hero_connect_title
import hr.kotwave.gameslibrary.resources.psn_hero_connected_body
import hr.kotwave.gameslibrary.resources.psn_hero_connected_title
import hr.kotwave.gameslibrary.resources.psn_owned_count
import hr.kotwave.gameslibrary.resources.psn_sync_fail_igdb
import hr.kotwave.gameslibrary.resources.psn_sync_fail_merge
import hr.kotwave.gameslibrary.resources.psn_sync_fail_psn
import hr.kotwave.gameslibrary.resources.psn_sync_fail_query
import hr.kotwave.gameslibrary.resources.psn_sync_fail_token
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
 * The PSN screen: connect a PlayStation account (npsso paste), then additively sync the purchased ∪
 * played library. The sync's id-unmatched tail goes through the Import funnel's Matching → Review
 * legs in place (its own [ImportViewModel], the Battle.net pattern). The user's own token authorizes
 * the pull; the paste parser stays available as the fallback intake.
 */
@Composable
fun PsnScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PsnViewModel = koinViewModel(),
    importViewModel: ImportViewModel = koinViewModel(),
) {
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
    val psn = tokens.store.psn
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
    ) {
        Header(onBack)
        Spacer(Modifier.height(tokens.spacing.md))
        Hero(connected = viewModel.connected, psn = psn)

        Spacer(Modifier.height(tokens.spacing.md))
        if (viewModel.connected) {
            val stage = viewModel.syncFailure
            ConnectedStoreCard(
                viewModel = viewModel,
                importViewModel = importViewModel,
                brand = StoreBrand(accent = psn, glyph = Store.PSN.glyph, avatarDark = Color(0xFF0D1830)),
                accountLabel = stringResource(Res.string.psn_account),
                ownedCountLabel = { count -> pluralStringResource(Res.plurals.psn_owned_count, count, count) },
                syncFailureMessage = if (stage != null) stage.message() else null,
            )
        } else {
            ConnectSection(viewModel = viewModel, psn = psn)
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
        Text("PlayStation", style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
    }
}

@Composable
private fun Hero(connected: Boolean, psn: Color) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.xl)
    val iconShape = RoundedCornerShape(tokens.radii.xl)
    Box(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1730), Color(0xFF071022))))
            .border(1.dp, psn.copy(alpha = 0.25f), shape)
            .padding(horizontal = tokens.spacing.xl, vertical = tokens.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(70.dp).clip(iconShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF15264A), Color(0xFF0D1830))))
                    .border(1.dp, psn.copy(alpha = 0.40f), iconShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(Store.PSN.glyph, style = AppTheme.type.brand.copy(fontSize = 30.sp), color = AppTheme.tokens.store.glyph(Store.PSN))
            }
            Spacer(Modifier.height(tokens.spacing.md))
            Text(
                if (connected) stringResource(Res.string.psn_hero_connected_title) else stringResource(Res.string.psn_hero_connect_title),
                style = AppTheme.type.display.copy(fontSize = 22.sp),
                color = tokens.colors.text,
            )
            Spacer(Modifier.height(tokens.spacing.xs))
            Text(
                if (connected) {
                    stringResource(Res.string.psn_hero_connected_body)
                } else {
                    stringResource(Res.string.psn_hero_connect_body)
                },
                style = AppTheme.type.body.copy(fontSize = 13.5.sp),
                color = tokens.colors.muted,
            )
        }
    }
}

@Composable
private fun ConnectSection(viewModel: PsnViewModel, psn: Color) {
    val tokens = AppTheme.tokens
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(tokens.radii.xl)) {
        Column(Modifier.fillMaxWidth().padding(tokens.spacing.md)) {
            when (val state = viewModel.connectState) {
                PsnConnectState.AwaitingNpsso -> {
                    PsnConnectCapture(
                        signInUrl = viewModel.signInUrl,
                        npssoUrl = viewModel.npssoUrl,
                        onNpsso = viewModel::onNpssoCaptured,
                    )
                    Spacer(Modifier.height(tokens.spacing.sm))
                    Text(
                        stringResource(Res.string.common_cancel),
                        style = AppTheme.type.caption,
                        color = tokens.colors.faint,
                        modifier = Modifier.clickable(onClick = viewModel::cancelConnect),
                    )
                }
                PsnConnectState.Exchanging -> ConnectingRow(psn, stringResource(Res.string.psn_finishing))
                else -> {
                    val failure = state as? PsnConnectState.Failed
                    StoreConnectPrompt(
                        note = stringResource(Res.string.psn_connect_note),
                        connectLabel = stringResource(Res.string.psn_connect),
                        onConnect = viewModel::connect,
                        failureMessage = if (failure != null) failure.reason.message() else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun PsnConnectFailure.message(): String = when (this) {
    PsnConnectFailure.Paste -> stringResource(Res.string.psn_fail_paste)
    PsnConnectFailure.Rejected -> stringResource(Res.string.psn_fail_rejected)
    PsnConnectFailure.Network -> stringResource(Res.string.psn_fail_network)
}

@Composable
private fun PsnSyncFailure.message(): String = when (this) {
    PsnSyncFailure.TokenRefresh -> stringResource(Res.string.psn_sync_fail_token)
    PsnSyncFailure.PsnFetch -> stringResource(Res.string.psn_sync_fail_psn)
    PsnSyncFailure.QueryOutdated -> stringResource(Res.string.psn_sync_fail_query)
    PsnSyncFailure.IgdbMatch -> stringResource(Res.string.psn_sync_fail_igdb)
    PsnSyncFailure.Merge -> stringResource(Res.string.psn_sync_fail_merge)
}
