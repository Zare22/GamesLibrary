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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.glyph
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

private val GogError = Color(0xFFF4707A)

/**
 * The GOG screen: connect a GOG account (OAuth2), then additively sync the owned library. The user's own
 * token authorizes the pull; there is no private-profile caveat as on Steam.
 */
@Composable
fun GogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GogViewModel = koinViewModel(),
) {
    val tokens = AppTheme.tokens
    val gog = tokens.store.gog
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Header(onBack)
        Spacer(Modifier.height(14.dp))
        Hero(connected = viewModel.connected, gog = gog)

        Spacer(Modifier.height(16.dp))
        if (viewModel.connected) {
            ConnectedCard(viewModel = viewModel, gog = gog)
        } else {
            ConnectSection(viewModel = viewModel, gog = gog)
        }

        Spacer(Modifier.height(20.dp))
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
        Text("GOG", style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
    }
}

@Composable
private fun Hero(connected: Boolean, gog: Color) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1030), Color(0xFF120A22))))
            .border(1.dp, gog.copy(alpha = 0.25f), shape)
            .padding(horizontal = 22.dp, vertical = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(70.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2A1B3D), Color(0xFF17102A))))
                    .border(1.dp, gog.copy(alpha = 0.40f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(Store.GOG.glyph, style = AppTheme.type.brand.copy(fontSize = 30.sp), color = gog)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (connected) "Connected to GOG" else "Connect GOG",
                style = AppTheme.type.display.copy(fontSize = 22.sp),
                color = tokens.colors.text,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (connected) {
                    "GamesLibrary auto-imports the games you own and keeps your GOG library in sync."
                } else {
                    "Sign in to auto-import the games you own. GamesLibrary only reads your owned games — no purchases."
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
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            when (val state = viewModel.connectState) {
                is GogConnectState.AwaitingLogin -> {
                    GogConnectCapture(authUrl = state.authUrl, onRedirect = viewModel::onRedirectCaptured)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Cancel",
                        style = AppTheme.type.caption,
                        color = tokens.colors.faint,
                        modifier = Modifier.clickable(onClick = viewModel::cancelConnect),
                    )
                }
                GogConnectState.Exchanging -> ConnectingRow(gog, "Finishing GOG sign-in…")
                else -> {
                    Text(
                        "GamesLibrary opens GOG in a browser and only reads the list of games you own.",
                        style = AppTheme.type.caption.copy(fontSize = 11.5.sp),
                        color = tokens.colors.faint,
                    )
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(
                        text = "Connect GOG",
                        onClick = viewModel::connect,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state is GogConnectState.Failed) {
                        Spacer(Modifier.height(10.dp))
                        Text(state.reason.message(), style = AppTheme.type.caption, color = GogError)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectingRow(gog: Color, label: String) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(18.dp), color = gog, strokeWidth = 2.dp)
        Text(label, style = AppTheme.type.body.copy(fontSize = 13.sp), color = tokens.colors.muted)
    }
}

private fun GogConnectFailure.message(): String = when (this) {
    GogConnectFailure.Auth -> "That sign-in didn't return a GOG code. Try connecting again."
    GogConnectFailure.Network -> "Couldn't reach GOG — check your connection and try again."
}

@Composable
private fun ConnectedCard(viewModel: GogViewModel, gog: Color) {
    val tokens = AppTheme.tokens
    val ok = tokens.status.playing
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(gog, Color(0xFF17102A))))
                        .border(1.dp, gog.copy(alpha = 0.40f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(Store.GOG.glyph, style = AppTheme.type.brand.copy(fontSize = 20.sp), color = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("GOG account", style = AppTheme.type.bodyStrong, color = tokens.colors.text)
                    Text("Connected", style = AppTheme.type.caption, color = tokens.colors.faint)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(ok))
                    Text("Connected", style = AppTheme.type.caption.copy(fontSize = 11.5.sp), color = ok)
                }
            }

            Divider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val count = viewModel.ownedCount
                    Text(
                        if (count != null) "$count games owned on GOG" else "Sync to pull your owned games",
                        style = AppTheme.type.body.copy(fontSize = 12.5.sp),
                        color = tokens.colors.muted,
                    )
                }
                SyncButton(syncing = viewModel.syncing, gog = gog, onClick = viewModel::sync)
            }

            viewModel.lastSummary?.let { summary ->
                Divider()
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    SyncStat(summary.added.toString(), "ADDED", tokens.status.playing)
                    SyncStat(summary.updated.toString(), "ALREADY HAD", tokens.colors.text)
                    SyncStat(summary.total.toString(), "SYNCED", tokens.colors.text)
                }
            }

            if (viewModel.syncFailed) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Couldn't reach GOG or IGDB — check your connection and try again.",
                    style = AppTheme.type.caption,
                    color = GogError,
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Disconnect",
                style = AppTheme.type.caption,
                color = tokens.colors.faint,
                modifier = Modifier.clickable(onClick = viewModel::disconnect),
            )
        }
    }
}

@Composable
private fun SyncButton(syncing: Boolean, gog: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Row(
        Modifier.clip(shape)
            .background(gog.copy(alpha = 0.12f))
            .border(1.dp, gog.copy(alpha = 0.45f), shape)
            .clickable(enabled = !syncing, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(AppIcons.Sync, null, Modifier.size(15.dp), tint = gog)
        Text(
            if (syncing) "Syncing…" else "Sync now",
            style = AppTheme.type.button.copy(fontSize = 13.5.sp),
            color = gog,
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
private fun Divider() {
    Spacer(Modifier.height(15.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(AppTheme.tokens.colors.border))
    Spacer(Modifier.height(15.dp))
}
