package hr.kotwave.gameslibrary.steam

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.cd_back
import hr.kotwave.gameslibrary.resources.common_cancel
import hr.kotwave.gameslibrary.resources.steam_connect_note
import hr.kotwave.gameslibrary.resources.steam_connecting
import hr.kotwave.gameslibrary.resources.steam_fail_network
import hr.kotwave.gameslibrary.resources.steam_fail_verification
import hr.kotwave.gameslibrary.resources.steam_hero_connect_body
import hr.kotwave.gameslibrary.resources.steam_hero_connect_title
import hr.kotwave.gameslibrary.resources.steam_hero_connected_body
import hr.kotwave.gameslibrary.resources.steam_hero_connected_title
import hr.kotwave.gameslibrary.resources.steam_ip_note
import hr.kotwave.gameslibrary.resources.steam_owned_count
import hr.kotwave.gameslibrary.resources.steam_privacy_open
import hr.kotwave.gameslibrary.resources.steam_privacy_step1
import hr.kotwave.gameslibrary.resources.steam_privacy_step2
import hr.kotwave.gameslibrary.resources.steam_privacy_step3
import hr.kotwave.gameslibrary.resources.steam_privacy_subtitle
import hr.kotwave.gameslibrary.resources.steam_privacy_title
import hr.kotwave.gameslibrary.resources.steam_privacy_zero
import hr.kotwave.gameslibrary.resources.steam_sign_in
import hr.kotwave.gameslibrary.resources.steam_signed_in
import hr.kotwave.gameslibrary.resources.steam_sync_fail_igdb
import hr.kotwave.gameslibrary.resources.steam_sync_fail_merge
import hr.kotwave.gameslibrary.resources.steam_sync_fail_steam
import hr.kotwave.gameslibrary.resources.store_connected
import hr.kotwave.gameslibrary.resources.store_disconnect
import hr.kotwave.gameslibrary.resources.store_sync_now
import hr.kotwave.gameslibrary.resources.store_sync_prompt
import hr.kotwave.gameslibrary.resources.store_syncing
import hr.kotwave.gameslibrary.resources.sync_stat_added
import hr.kotwave.gameslibrary.resources.sync_stat_already
import hr.kotwave.gameslibrary.resources.sync_stat_synced
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.components.actionWidth
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val SteamWarn = Color(0xFFFFCE4A)
private val SteamError = Color(0xFFF4707A)
private const val STEAM_PRIVACY_URL = "https://steamcommunity.com/my/edit/settings"

/**
 * The Steam screen: sign in through Steam (OpenID), then additively sync the owned library. A private
 * profile returns zero games — the privacy helper explains how to fix it.
 */
@Composable
fun SteamScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SteamViewModel = koinViewModel(),
) {
    val tokens = AppTheme.tokens
    val steam = tokens.store.steam
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
    ) {
        Header(onBack)
        Spacer(Modifier.height(tokens.spacing.md))
        Hero(connected = viewModel.connected, steam = steam)

        Spacer(Modifier.height(tokens.spacing.md))
        if (viewModel.connected) {
            ConnectedCard(viewModel = viewModel, steam = steam)
        } else {
            ConnectSection(viewModel = viewModel, steam = steam)
        }

        Spacer(Modifier.height(tokens.spacing.md))
        PrivacyHelper(highlighted = viewModel.lastSummary != null && viewModel.ownedCount == 0)

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
        Text("Steam", style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
    }
}

@Composable
private fun Hero(connected: Boolean, steam: Color) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.xl)
    val iconShape = RoundedCornerShape(tokens.radii.xl)
    Box(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF0A121D))))
            .border(1.dp, steam.copy(alpha = 0.25f), shape)
            .padding(horizontal = tokens.spacing.xl, vertical = tokens.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(70.dp).clip(iconShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF1B2838), Color(0xFF0E1722))))
                    .border(1.dp, steam.copy(alpha = 0.40f), iconShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Steam, null, Modifier.size(34.dp), tint = steam)
            }
            Spacer(Modifier.height(tokens.spacing.md))
            Text(
                if (connected) stringResource(Res.string.steam_hero_connected_title) else stringResource(Res.string.steam_hero_connect_title),
                style = AppTheme.type.display.copy(fontSize = 22.sp),
                color = tokens.colors.text,
            )
            Spacer(Modifier.height(tokens.spacing.xs))
            Text(
                if (connected) {
                    stringResource(Res.string.steam_hero_connected_body)
                } else {
                    stringResource(Res.string.steam_hero_connect_body)
                },
                style = AppTheme.type.body.copy(fontSize = 13.5.sp),
                color = tokens.colors.muted,
            )
        }
    }
}

@Composable
private fun ConnectSection(viewModel: SteamViewModel, steam: Color) {
    val tokens = AppTheme.tokens
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(tokens.radii.xl)) {
        Column(Modifier.fillMaxWidth().padding(tokens.spacing.md)) {
            Text(
                stringResource(Res.string.steam_connect_note),
                style = AppTheme.type.caption.copy(fontSize = 11.5.sp),
                color = tokens.colors.faint,
            )
            Spacer(Modifier.height(tokens.spacing.xs))
            Text(
                stringResource(Res.string.steam_ip_note),
                style = AppTheme.type.caption.copy(fontSize = 11.5.sp),
                color = tokens.colors.faint,
            )
            Spacer(Modifier.height(tokens.spacing.md))
            when (val state = viewModel.connectState) {
                SteamConnectState.Connecting -> ConnectingRow(steam = steam, onCancel = viewModel::cancelConnect)
                else -> {
                    PrimaryButton(
                        text = stringResource(Res.string.steam_sign_in),
                        onClick = viewModel::connect,
                        leadingIcon = AppIcons.Steam,
                        modifier = Modifier.actionWidth(),
                    )
                    if (state is SteamConnectState.Failed) {
                        Spacer(Modifier.height(tokens.spacing.sm))
                        Text(state.reason.message(), style = AppTheme.type.caption, color = SteamError)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectingRow(steam: Color, onCancel: () -> Unit) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        CircularProgressIndicator(Modifier.size(18.dp), color = steam, strokeWidth = 2.dp)
        Text(
            stringResource(Res.string.steam_connecting),
            style = AppTheme.type.body.copy(fontSize = 13.sp),
            color = tokens.colors.muted,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(Res.string.common_cancel),
            style = AppTheme.type.caption,
            color = tokens.colors.faint,
            modifier = Modifier.clickable(onClick = onCancel),
        )
    }
}

@Composable
private fun SteamConnectFailure.message(): String = when (this) {
    SteamConnectFailure.Verification -> stringResource(Res.string.steam_fail_verification)
    SteamConnectFailure.Network -> stringResource(Res.string.steam_fail_network)
}

@Composable
internal fun SteamSyncStage.message(): String = when (this) {
    SteamSyncStage.SteamFetch -> stringResource(Res.string.steam_sync_fail_steam)
    SteamSyncStage.IgdbMatch -> stringResource(Res.string.steam_sync_fail_igdb)
    SteamSyncStage.Merge -> stringResource(Res.string.steam_sync_fail_merge)
}

@Composable
private fun ConnectedCard(viewModel: SteamViewModel, steam: Color) {
    val tokens = AppTheme.tokens
    val ok = tokens.status.playing
    val avatarShape = RoundedCornerShape(tokens.radii.tile)
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(tokens.radii.xl)) {
        Column(Modifier.fillMaxWidth().padding(tokens.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Box(
                    Modifier.size(46.dp).clip(avatarShape)
                        .background(Brush.linearGradient(listOf(steam, Color(0xFF0E1722))))
                        .border(1.dp, steam.copy(alpha = 0.40f), avatarShape),
                ) {
                    viewModel.persona?.avatarUrl?.let { avatar ->
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(avatarShape),
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        viewModel.persona?.personaName ?: viewModel.steamId.orEmpty(),
                        style = AppTheme.type.bodyStrong,
                        color = tokens.colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(Res.string.steam_signed_in),
                        style = AppTheme.type.caption,
                        color = tokens.colors.faint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
                        if (count != null) pluralStringResource(Res.plurals.steam_owned_count, count, count) else stringResource(Res.string.store_sync_prompt),
                        style = AppTheme.type.body.copy(fontSize = 12.5.sp),
                        color = tokens.colors.muted,
                    )
                }
                SyncButton(syncing = viewModel.syncing, steam = steam, onClick = viewModel::sync)
            }

            viewModel.lastSummary?.let { summary ->
                Divider()
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.lg)) {
                    SyncStat(summary.added.toString(), stringResource(Res.string.sync_stat_added), tokens.status.playing)
                    SyncStat(summary.updated.toString(), stringResource(Res.string.sync_stat_already), tokens.colors.text)
                    SyncStat(summary.total.toString(), stringResource(Res.string.sync_stat_synced), tokens.colors.text)
                }
            }

            viewModel.syncFailure?.let { stage ->
                Spacer(Modifier.height(tokens.spacing.sm))
                Text(
                    stage.message(),
                    style = AppTheme.type.caption,
                    color = SteamError,
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
private fun SyncButton(syncing: Boolean, steam: Color, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Row(
        Modifier.clip(shape)
            .background(steam.copy(alpha = 0.12f))
            .border(1.dp, steam.copy(alpha = 0.45f), shape)
            .clickable(enabled = !syncing, onClick = onClick)
            .padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Icon(AppIcons.Sync, null, Modifier.size(15.dp), tint = steam)
        Text(
            if (syncing) stringResource(Res.string.store_syncing) else stringResource(Res.string.store_sync_now),
            style = AppTheme.type.button.copy(fontSize = 13.5.sp),
            color = steam,
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
private fun PrivacyHelper(highlighted: Boolean) {
    val tokens = AppTheme.tokens
    val uriHandler = LocalUriHandler.current
    val shape = RoundedCornerShape(tokens.radii.xl)
    val iconShape = RoundedCornerShape(tokens.radii.md)
    val buttonShape = RoundedCornerShape(tokens.radii.md)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(SteamWarn.copy(alpha = if (highlighted) 0.10f else 0.06f))
            .border(1.dp, SteamWarn.copy(alpha = if (highlighted) 0.45f else 0.25f), shape)
            .padding(tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            Box(
                Modifier.size(34.dp).clip(iconShape)
                    .background(SteamWarn.copy(alpha = 0.14f)).border(1.dp, SteamWarn.copy(alpha = 0.35f), iconShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Check, null, Modifier.size(17.dp), tint = SteamWarn)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.steam_privacy_title), style = AppTheme.type.bodyStrong, color = tokens.colors.text)
                Text(stringResource(Res.string.steam_privacy_subtitle), style = AppTheme.type.caption.copy(fontSize = 11.sp), color = tokens.colors.faint)
            }
        }
        PrivacyStep("1", stringResource(Res.string.steam_privacy_step1))
        PrivacyStep("2", stringResource(Res.string.steam_privacy_step2))
        PrivacyStep("3", stringResource(Res.string.steam_privacy_step3))
        if (highlighted) {
            Text(
                stringResource(Res.string.steam_privacy_zero),
                style = AppTheme.type.caption,
                color = SteamWarn,
            )
        }
        Spacer(Modifier.height(tokens.spacing.micro))
        Row(
            Modifier.actionWidth().clip(buttonShape)
                .background(tokens.colors.surface).border(1.dp, tokens.colors.border, buttonShape)
                .clickable { uriHandler.openUri(STEAM_PRIVACY_URL) }
                .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs, Alignment.CenterHorizontally),
        ) {
            Text(stringResource(Res.string.steam_privacy_open), style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp), color = tokens.colors.muted)
        }
    }
}

@Composable
private fun PrivacyStep(number: String, text: String) {
    val tokens = AppTheme.tokens
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        Box(
            Modifier.size(21.dp).clip(CircleShape)
                .background(SteamWarn.copy(alpha = 0.15f)).border(1.dp, SteamWarn.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, style = AppTheme.type.caption.copy(fontSize = 11.sp), color = SteamWarn)
        }
        Text(text, style = AppTheme.type.body.copy(fontSize = 13.sp), color = tokens.colors.muted, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Divider() {
    val tokens = AppTheme.tokens
    Spacer(Modifier.height(tokens.spacing.md))
    Box(Modifier.fillMaxWidth().height(1.dp).background(tokens.colors.border))
    Spacer(Modifier.height(tokens.spacing.md))
}
