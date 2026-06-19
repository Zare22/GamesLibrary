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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

private val SteamWarn = Color(0xFFFFCE4A)
private const val STEAM_PRIVACY_URL = "https://steamcommunity.com/my/edit/settings"

/**
 * The Steam screen: connect (a temporary SteamID entry until OpenID lands), then additively sync the
 * owned library. A private profile returns zero games — the privacy helper explains how to fix it.
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Header(onBack)
        Spacer(Modifier.height(14.dp))
        Hero(connected = viewModel.connected, steam = steam)

        Spacer(Modifier.height(16.dp))
        if (viewModel.connected) {
            ConnectedCard(viewModel = viewModel, steam = steam)
        } else {
            ConnectSection(viewModel = viewModel)
        }

        Spacer(Modifier.height(16.dp))
        PrivacyHelper(highlighted = viewModel.lastSummary != null && viewModel.ownedCount == 0)

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
        Text("Steam", style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
    }
}

@Composable
private fun Hero(connected: Boolean, steam: Color) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF0A121D))))
            .border(1.dp, steam.copy(alpha = 0.25f), shape)
            .padding(horizontal = 22.dp, vertical = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(70.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1B2838), Color(0xFF0E1722))))
                    .border(1.dp, steam.copy(alpha = 0.40f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Steam, null, Modifier.size(34.dp), tint = steam)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (connected) "Connected to Steam" else "Connect Steam",
                style = AppTheme.type.display.copy(fontSize = 22.sp),
                color = tokens.colors.text,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (connected) {
                    "GamesLibrary auto-imports the games you own and keeps your Steam library in sync."
                } else {
                    "Sign in to auto-import the games you own. Steam shares only your public game list — no password, no purchases."
                },
                style = AppTheme.type.body.copy(fontSize = 13.5.sp),
                color = tokens.colors.muted,
            )
        }
    }
}

@Composable
private fun ConnectSection(viewModel: SteamViewModel) {
    val tokens = AppTheme.tokens
    var input by remember { mutableStateOf("") }
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Temporary: paste your public SteamID64 (17 digits). Real \"Sign in through Steam\" arrives next.",
                style = AppTheme.type.caption.copy(fontSize = 11.5.sp),
                color = tokens.colors.faint,
            )
            Spacer(Modifier.height(12.dp))
            SteamIdField(value = input, onValueChange = { input = it }, error = viewModel.idError)
            if (viewModel.idError) {
                Spacer(Modifier.height(8.dp))
                Text("That isn't a SteamID64 — it's 17 digits.", style = AppTheme.type.caption, color = Color(0xFFF4707A))
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                text = "Connect",
                onClick = { viewModel.connect(input) },
                leadingIcon = AppIcons.Check,
                enabled = input.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SteamIdField(value: String, onValueChange: (String) -> Unit, error: Boolean) {
    val tokens = AppTheme.tokens
    GlassSurface(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(15.dp),
        borderColor = if (error) Color(0xFFF4707A) else tokens.colors.borderStrong,
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 15.dp), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text("7656119…", style = AppTheme.type.body, color = tokens.colors.faint)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = AppTheme.type.body.copy(color = tokens.colors.text),
                cursorBrush = SolidColor(tokens.colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ConnectedCard(viewModel: SteamViewModel, steam: Color) {
    val tokens = AppTheme.tokens
    val ok = tokens.status.playing
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(steam, Color(0xFF0E1722))))
                        .border(1.dp, steam.copy(alpha = 0.40f), RoundedCornerShape(14.dp)),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        viewModel.steamId ?: "",
                        style = AppTheme.type.bodyStrong,
                        color = tokens.colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("Signed in via SteamID", style = AppTheme.type.caption, color = tokens.colors.faint)
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
                        if (count != null) "$count games owned on Steam" else "Sync to pull your owned games",
                        style = AppTheme.type.body.copy(fontSize = 12.5.sp),
                        color = tokens.colors.muted,
                    )
                }
                SyncButton(syncing = viewModel.syncing, steam = steam, onClick = viewModel::sync)
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
                    "Couldn't reach Steam or IGDB — check your connection and the API key.",
                    style = AppTheme.type.caption,
                    color = Color(0xFFF4707A),
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
private fun SyncButton(syncing: Boolean, steam: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Row(
        Modifier.clip(shape)
            .background(steam.copy(alpha = 0.12f))
            .border(1.dp, steam.copy(alpha = 0.45f), shape)
            .clickable(enabled = !syncing, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(AppIcons.Sync, null, Modifier.size(15.dp), tint = steam)
        Text(
            if (syncing) "Syncing…" else "Sync now",
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
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(SteamWarn.copy(alpha = if (highlighted) 0.10f else 0.06f))
            .border(1.dp, SteamWarn.copy(alpha = if (highlighted) 0.45f else 0.25f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                    .background(SteamWarn.copy(alpha = 0.14f)).border(1.dp, SteamWarn.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Check, null, Modifier.size(17.dp), tint = SteamWarn)
            }
            Column(Modifier.weight(1f)) {
                Text("Set your privacy right", style = AppTheme.type.bodyStrong, color = tokens.colors.text)
                Text("Steam only shares games when these match", style = AppTheme.type.caption.copy(fontSize = 11.sp), color = tokens.colors.faint)
            }
        }
        PrivacyStep("1", "Open Steam → Profile → Edit Profile → Privacy Settings.")
        PrivacyStep("2", "Set Game details to Public — not Friends Only.")
        PrivacyStep("3", "Leave \"Always keep my total playtime private\" unchecked.")
        if (highlighted) {
            Text(
                "Steam returned 0 games — your Game details are likely private.",
                style = AppTheme.type.caption,
                color = SteamWarn,
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                .background(tokens.colors.surface).border(1.dp, tokens.colors.border, RoundedCornerShape(13.dp))
                .clickable { uriHandler.openUri(STEAM_PRIVACY_URL) }
                .padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Text("Open Steam privacy settings", style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp), color = tokens.colors.muted)
        }
    }
}

@Composable
private fun PrivacyStep(number: String, text: String) {
    val tokens = AppTheme.tokens
    Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
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
    Spacer(Modifier.height(15.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(AppTheme.tokens.colors.border))
    Spacer(Modifier.height(15.dp))
}
