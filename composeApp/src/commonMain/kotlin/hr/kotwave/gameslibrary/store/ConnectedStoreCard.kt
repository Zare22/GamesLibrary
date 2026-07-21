package hr.kotwave.gameslibrary.store

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import hr.kotwave.gameslibrary.importer.ImportViewModel
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.error_igdb_unreachable
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
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Per-store brand slots the shared store-sync UI renders from: the accent, its badge [glyph], and the
 * avatar gradient's two ends — [avatarLight] defaults to the accent, which a store whose accent is too
 * light to sit under a white glyph overrides.
 */
data class StoreBrand(
    val accent: Color,
    val glyph: String,
    val avatarDark: Color,
    val avatarLight: Color = accent,
)

/**
 * The connected-account card shared by every store's Sync screen: account row, owned-count + sync
 * button, the last summary's stats, and the failure / disconnect affordances. Brand-specific bits come
 * through [brand] and the pre-resolved string slots ([accountLabel], [ownedCountLabel], [syncFailureMessage]),
 * so the card stays store-agnostic; it reads sync state off [viewModel] and drives [StoreSyncViewModel.sync]
 * / [StoreSyncViewModel.disconnect] directly. Tapping the needs-review stat runs the tail through
 * [importViewModel]'s funnel, which answers [viewModel] directly. A store that knows the signed-in account
 * fills the avatar with [avatarUrl] instead of the brand glyph and names the connection with [accountSubline].
 */
@Composable
fun ConnectedStoreCard(
    viewModel: StoreSyncViewModel<*>,
    importViewModel: ImportViewModel,
    brand: StoreBrand,
    accountLabel: String,
    ownedCountLabel: @Composable (Int) -> String,
    syncFailureMessage: String?,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    accountSubline: String? = null,
) {
    val tokens = AppTheme.tokens
    val ok = tokens.status.playing
    val avatarShape = RoundedCornerShape(tokens.radii.tile)
    GlassSurface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(tokens.radii.xl)) {
        Column(Modifier.fillMaxWidth().padding(tokens.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Box(
                    Modifier.size(46.dp).clip(avatarShape)
                        .background(Brush.linearGradient(listOf(brand.avatarLight, brand.avatarDark)))
                        .border(1.dp, brand.accent.copy(alpha = 0.40f), avatarShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(avatarShape),
                        )
                    } else {
                        Text(brand.glyph, style = AppTheme.type.brand.copy(fontSize = 20.sp), color = Color.White)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        accountLabel,
                        style = AppTheme.type.bodyStrong,
                        color = tokens.colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        accountSubline ?: stringResource(Res.string.store_connected),
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
                        if (count != null) ownedCountLabel(count) else stringResource(Res.string.store_sync_prompt),
                        style = AppTheme.type.body.copy(fontSize = 12.5.sp),
                        color = tokens.colors.muted,
                    )
                }
                SyncButton(syncing = viewModel.syncing, accent = brand.accent, onClick = viewModel::sync)
            }

            viewModel.lastSummary?.let { summary ->
                Divider()
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.lg)) {
                    SyncStat(summary.added.toString(), stringResource(Res.string.sync_stat_added), tokens.status.playing)
                    SyncStat(summary.updated.toString(), stringResource(Res.string.sync_stat_already), tokens.colors.text)
                    SyncStat(summary.total.toString(), stringResource(Res.string.sync_stat_synced), tokens.colors.text)
                    if (viewModel.reviewTail.isNotEmpty()) {
                        ReviewStat(
                            count = viewModel.reviewTail.size,
                            enabled = !viewModel.syncing,
                            onClick = { viewModel.review(importViewModel::reviewSyncTail) },
                        )
                    }
                }
            }

            syncFailureMessage?.let { message ->
                Spacer(Modifier.height(tokens.spacing.sm))
                Text(message, style = AppTheme.type.caption, color = tokens.colors.error)
            }

            if (importViewModel.failed) {
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

/** The shared idle connect prompt: a note, the connect button, and any connect failure message. */
@Composable
fun StoreConnectPrompt(
    note: String,
    connectLabel: String,
    onConnect: () -> Unit,
    failureMessage: String?,
) {
    val tokens = AppTheme.tokens
    Text(note, style = AppTheme.type.caption.copy(fontSize = 11.5.sp), color = tokens.colors.faint)
    Spacer(Modifier.height(tokens.spacing.md))
    PrimaryButton(text = connectLabel, onClick = onConnect, modifier = Modifier.actionWidth())
    failureMessage?.let { message ->
        Spacer(Modifier.height(tokens.spacing.sm))
        Text(message, style = AppTheme.type.caption, color = tokens.colors.error)
    }
}

/** A spinner + label row shown while a store connect exchange is in flight. */
@Composable
fun ConnectingRow(accent: Color, label: String) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        CircularProgressIndicator(Modifier.size(18.dp), color = accent, strokeWidth = 2.dp)
        Text(label, style = AppTheme.type.body.copy(fontSize = 13.sp), color = tokens.colors.muted)
    }
}

@Composable
private fun SyncButton(syncing: Boolean, accent: Color, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Row(
        Modifier.clip(shape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.45f), shape)
            .clickable(enabled = !syncing, onClick = onClick)
            .padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Icon(AppIcons.Sync, null, Modifier.size(15.dp), tint = accent)
        Text(
            if (syncing) stringResource(Res.string.store_syncing) else stringResource(Res.string.store_sync_now),
            style = AppTheme.type.button.copy(fontSize = 13.5.sp),
            color = accent,
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
