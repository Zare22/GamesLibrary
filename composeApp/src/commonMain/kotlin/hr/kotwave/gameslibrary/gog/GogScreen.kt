package hr.kotwave.gameslibrary.gog

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
import hr.kotwave.gameslibrary.resources.gog_account
import hr.kotwave.gameslibrary.resources.gog_connect
import hr.kotwave.gameslibrary.resources.gog_connect_note
import hr.kotwave.gameslibrary.resources.gog_fail_auth
import hr.kotwave.gameslibrary.resources.gog_fail_network
import hr.kotwave.gameslibrary.resources.gog_finishing
import hr.kotwave.gameslibrary.resources.gog_hero_connect_body
import hr.kotwave.gameslibrary.resources.gog_hero_connect_title
import hr.kotwave.gameslibrary.resources.gog_hero_connected_body
import hr.kotwave.gameslibrary.resources.gog_hero_connected_title
import hr.kotwave.gameslibrary.resources.gog_owned_count
import hr.kotwave.gameslibrary.resources.gog_sync_fail_gog
import hr.kotwave.gameslibrary.resources.gog_sync_fail_igdb
import hr.kotwave.gameslibrary.resources.gog_sync_fail_merge
import hr.kotwave.gameslibrary.resources.gog_sync_fail_token
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
 * The GOG screen: connect a GOG account (OAuth2), then additively sync the owned library. The sync's
 * id-unmatched tail goes through the Import funnel's Matching → Review legs in place (its own
 * [ImportViewModel], the Battle.net pattern). The user's own token authorizes the pull; there is no
 * private-profile caveat as on Steam.
 */
@Composable
fun GogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GogViewModel = koinViewModel(),
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
    val gog = tokens.store.gog
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
    ) {
        Header(onBack)
        Spacer(Modifier.height(tokens.spacing.md))
        Hero(connected = viewModel.connected, gog = gog)

        Spacer(Modifier.height(tokens.spacing.md))
        if (viewModel.connected) {
            val stage = viewModel.syncFailure
            ConnectedStoreCard(
                viewModel = viewModel,
                brand = StoreBrand(accent = gog, glyph = Store.GOG.glyph, avatarDark = Color(0xFF17102A)),
                accountLabel = stringResource(Res.string.gog_account),
                ownedCountLabel = { count -> pluralStringResource(Res.plurals.gog_owned_count, count, count) },
                syncFailureMessage = if (stage != null) stage.message() else null,
                reviewFailed = importViewModel.failed,
                onReview = { importViewModel.startFromSyncTail(Store.GOG, viewModel.reviewTail) },
            )
        } else {
            ConnectSection(viewModel = viewModel, gog = gog)
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
        Text("GOG", style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
    }
}

@Composable
private fun Hero(connected: Boolean, gog: Color) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.xl)
    val iconShape = RoundedCornerShape(tokens.radii.xl)
    Box(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1030), Color(0xFF120A22))))
            .border(1.dp, gog.copy(alpha = 0.25f), shape)
            .padding(horizontal = tokens.spacing.xl, vertical = tokens.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(70.dp).clip(iconShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2A1B3D), Color(0xFF17102A))))
                    .border(1.dp, gog.copy(alpha = 0.40f), iconShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(Store.GOG.glyph, style = AppTheme.type.brand.copy(fontSize = 30.sp), color = gog)
            }
            Spacer(Modifier.height(tokens.spacing.md))
            Text(
                if (connected) stringResource(Res.string.gog_hero_connected_title) else stringResource(Res.string.gog_hero_connect_title),
                style = AppTheme.type.display.copy(fontSize = 22.sp),
                color = tokens.colors.text,
            )
            Spacer(Modifier.height(tokens.spacing.xs))
            Text(
                if (connected) {
                    stringResource(Res.string.gog_hero_connected_body)
                } else {
                    stringResource(Res.string.gog_hero_connect_body)
                },
                style = AppTheme.type.body.copy(fontSize = 13.5.sp),
                color = tokens.colors.muted,
            )
        }
    }
}

@Composable
private fun ConnectSection(viewModel: GogViewModel, gog: Color) {
    val tokens = AppTheme.tokens
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(tokens.radii.xl)) {
        Column(Modifier.fillMaxWidth().padding(tokens.spacing.md)) {
            when (val state = viewModel.connectState) {
                is GogConnectState.AwaitingLogin -> {
                    GogConnectCapture(authUrl = state.authUrl, onRedirect = viewModel::onRedirectCaptured)
                    Spacer(Modifier.height(tokens.spacing.sm))
                    Text(
                        stringResource(Res.string.common_cancel),
                        style = AppTheme.type.caption,
                        color = tokens.colors.faint,
                        modifier = Modifier.clickable(onClick = viewModel::cancelConnect),
                    )
                }
                GogConnectState.Exchanging -> ConnectingRow(gog, stringResource(Res.string.gog_finishing))
                else -> {
                    val failure = state as? GogConnectState.Failed
                    StoreConnectPrompt(
                        note = stringResource(Res.string.gog_connect_note),
                        connectLabel = stringResource(Res.string.gog_connect),
                        onConnect = viewModel::connect,
                        failureMessage = if (failure != null) failure.reason.message() else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun GogConnectFailure.message(): String = when (this) {
    GogConnectFailure.Auth -> stringResource(Res.string.gog_fail_auth)
    GogConnectFailure.Network -> stringResource(Res.string.gog_fail_network)
}

@Composable
private fun GogSyncStage.message(): String = when (this) {
    GogSyncStage.TokenRefresh -> stringResource(Res.string.gog_sync_fail_token)
    GogSyncStage.GogFetch -> stringResource(Res.string.gog_sync_fail_gog)
    GogSyncStage.IgdbMatch -> stringResource(Res.string.gog_sync_fail_igdb)
    GogSyncStage.Merge -> stringResource(Res.string.gog_sync_fail_merge)
}
