package hr.kotwave.gameslibrary.mirror

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.mirror.host.LanAddress
import hr.kotwave.gameslibrary.mirror.host.LanAddressKind
import hr.kotwave.gameslibrary.mirror.host.MirrorCertProvenance
import hr.kotwave.gameslibrary.mirror.host.MirrorHost
import hr.kotwave.gameslibrary.mirror.host.MirrorHostEvent
import hr.kotwave.gameslibrary.mirror.host.MirrorHosting
import hr.kotwave.gameslibrary.mirror.host.enumerateLanAddresses
import hr.kotwave.gameslibrary.mirror.wire.MIRROR_DEFAULT_PORT
import hr.kotwave.gameslibrary.mirror.wire.MIRROR_PORT_ATTEMPTS
import hr.kotwave.gameslibrary.mirror.wire.mirrorVerifyCode
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.cd_back
import hr.kotwave.gameslibrary.resources.mirror_host_applied_added
import hr.kotwave.gameslibrary.resources.mirror_host_applied_removed
import hr.kotwave.gameslibrary.resources.mirror_host_applied_title
import hr.kotwave.gameslibrary.resources.mirror_host_applied_updated
import hr.kotwave.gameslibrary.resources.mirror_host_fallback_address
import hr.kotwave.gameslibrary.resources.mirror_host_fallback_note
import hr.kotwave.gameslibrary.resources.mirror_host_fallback_pin
import hr.kotwave.gameslibrary.resources.mirror_host_fallback_port
import hr.kotwave.gameslibrary.resources.mirror_host_fallback_subtitle
import hr.kotwave.gameslibrary.resources.mirror_host_fallback_title
import hr.kotwave.gameslibrary.resources.mirror_host_feed_paired
import hr.kotwave.gameslibrary.resources.mirror_host_feed_pulled
import hr.kotwave.gameslibrary.resources.mirror_host_feed_waiting_push
import hr.kotwave.gameslibrary.resources.mirror_host_firewall_body
import hr.kotwave.gameslibrary.resources.mirror_host_firewall_title
import hr.kotwave.gameslibrary.resources.mirror_host_identity_changed
import hr.kotwave.gameslibrary.resources.mirror_host_locked_body
import hr.kotwave.gameslibrary.resources.mirror_host_no_port_body
import hr.kotwave.gameslibrary.resources.mirror_host_no_port_title
import hr.kotwave.gameslibrary.resources.mirror_host_pill_locked
import hr.kotwave.gameslibrary.resources.mirror_host_pill_running
import hr.kotwave.gameslibrary.resources.mirror_host_pill_starting
import hr.kotwave.gameslibrary.resources.mirror_host_pill_stopped
import hr.kotwave.gameslibrary.resources.mirror_host_pill_waiting
import hr.kotwave.gameslibrary.resources.mirror_host_privacy_body
import hr.kotwave.gameslibrary.resources.mirror_host_privacy_title
import hr.kotwave.gameslibrary.resources.mirror_host_qr_caption
import hr.kotwave.gameslibrary.resources.mirror_host_start
import hr.kotwave.gameslibrary.resources.mirror_host_starting_body
import hr.kotwave.gameslibrary.resources.mirror_host_starting_first
import hr.kotwave.gameslibrary.resources.mirror_host_step_1
import hr.kotwave.gameslibrary.resources.mirror_host_step_2
import hr.kotwave.gameslibrary.resources.mirror_host_step_3
import hr.kotwave.gameslibrary.resources.mirror_host_steps_title
import hr.kotwave.gameslibrary.resources.mirror_host_still_hosting
import hr.kotwave.gameslibrary.resources.mirror_host_stop
import hr.kotwave.gameslibrary.resources.mirror_host_stopped_body
import hr.kotwave.gameslibrary.resources.mirror_host_title
import hr.kotwave.gameslibrary.resources.mirror_host_try_again
import hr.kotwave.gameslibrary.resources.mirror_net_ethernet
import hr.kotwave.gameslibrary.resources.mirror_net_other
import hr.kotwave.gameslibrary.resources.mirror_net_virtual
import hr.kotwave.gameslibrary.resources.mirror_net_wifi
import hr.kotwave.gameslibrary.resources.mirror_settings_host_subtitle
import hr.kotwave.gameslibrary.resources.mirror_settings_host_title
import hr.kotwave.gameslibrary.ui.components.ContentColumn
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.components.SecondaryButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.screens.SettingsCard
import hr.kotwave.gameslibrary.ui.screens.SettingsItem
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private val OkGreen = Color(0xFF7DF0B6)

@Composable
actual fun MirrorSettingsSection(onOpenMirror: () -> Unit, onMirrorNow: () -> Unit) {
    SettingsCard {
        SettingsItem(
            icon = AppIcons.Mirror,
            iconTint = AppTheme.tokens.colors.accent,
            title = stringResource(Res.string.mirror_settings_host_title),
            subtitle = stringResource(Res.string.mirror_settings_host_subtitle),
            onClick = onOpenMirror,
        )
    }
}

@Composable
actual fun MirrorScreen(onBack: () -> Unit, onMirrorNow: () -> Unit) {
    ContentColumn { MirrorHostingScreen(onBack) }
}

private sealed interface HostingUi {
    data object Starting : HostingUi
    data object Stopped : HostingUi
    data object StartFailed : HostingUi
    data class Hosting(
        val hosting: MirrorHosting,
        val addresses: List<LanAddress>,
        val selectedIp: String,
        val feed: List<FeedEntry> = emptyList(),
        val applied: MirrorHostEvent.Applied? = null,
        val locked: Boolean = false,
    ) : HostingUi
}

private data class FeedEntry(val event: MirrorHostEvent, val time: String)

/** The desktop hosting screen (07 + states S1–S9): auto-starts on open, stops on dispose. */
@Composable
private fun MirrorHostingScreen(onBack: () -> Unit) {
    val host = koinInject<MirrorHost>()
    val scope = rememberCoroutineScope()
    var ui by remember { mutableStateOf<HostingUi>(HostingUi.Starting) }

    val start: () -> Unit = {
        ui = HostingUi.Starting
        scope.launch {
            ui = try {
                val hosting = host.start()
                val addresses = enumerateLanAddresses()
                HostingUi.Hosting(hosting, addresses, addresses.firstOrNull()?.ip ?: "127.0.0.1")
            } catch (cancelled: CancellationException) {
                host.stop()
                throw cancelled
            } catch (_: Exception) {
                HostingUi.StartFailed
            }
        }
    }

    DisposableEffect(Unit) {
        start()
        onDispose { host.stop() }
    }

    LaunchedEffect(Unit) {
        host.events.collect { event ->
            val current = ui as? HostingUi.Hosting ?: return@collect
            ui = when (event) {
                is MirrorHostEvent.Paired, MirrorHostEvent.Pulled ->
                    current.copy(feed = current.feed + FeedEntry(event, nowHhMm()), applied = null)

                is MirrorHostEvent.Applied -> current.copy(applied = event)
                MirrorHostEvent.PairingLocked -> current.copy(locked = true)
            }
        }
    }

    val tokens = AppTheme.tokens
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
    ) {
        HostingHeader(
            ui = ui,
            onBack = onBack,
            onStop = {
                host.stop()
                ui = HostingUi.Stopped
            },
        )
        Spacer(Modifier.height(tokens.spacing.lg))
        when (val state = ui) {
            HostingUi.Starting -> StartingCard()
            HostingUi.Stopped -> StoppedCard(onStart = start)
            HostingUi.StartFailed -> StartFailedCard(onRetry = start)
            is HostingUi.Hosting -> HostingContent(
                state = state,
                onSelectIp = { ip -> ui = state.copy(selectedIp = ip) },
            )
        }
    }
}

@Composable
private fun HostingHeader(ui: HostingUi, onBack: () -> Unit, onStop: () -> Unit) {
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
        Text(stringResource(Res.string.mirror_host_title), style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
        HostPill(ui)
        Spacer(Modifier.weight(1f))
        if (ui is HostingUi.Hosting) {
            SecondaryButton(
                text = stringResource(Res.string.mirror_host_stop),
                onClick = onStop,
                leadingIcon = AppIcons.Stop,
            )
        }
    }
}

@Composable
private fun HostPill(ui: HostingUi) {
    val (text, color) = when (ui) {
        HostingUi.Starting -> stringResource(Res.string.mirror_host_pill_starting) to AppTheme.tokens.colors.faint
        HostingUi.Stopped, HostingUi.StartFailed -> stringResource(Res.string.mirror_host_pill_stopped) to AppTheme.tokens.colors.faint
        is HostingUi.Hosting -> when {
            ui.locked -> stringResource(Res.string.mirror_host_pill_locked) to AppTheme.tokens.colors.warning
            ui.feed.any { it.event == MirrorHostEvent.Pulled } && ui.applied == null ->
                stringResource(Res.string.mirror_host_pill_running) to OkGreen

            else -> stringResource(Res.string.mirror_host_pill_waiting) to OkGreen
        }
    }
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier.clip(shape).background(color.copy(alpha = 0.10f)).border(1.dp, color.copy(alpha = 0.35f), shape)
            .padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text, style = AppTheme.type.caption.copy(fontSize = 12.sp), color = color)
    }
}

@Composable
private fun StartingCard() {
    val tokens = AppTheme.tokens
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.lg),
    ) {
        CircularProgressIndicator(Modifier.size(28.dp), color = tokens.colors.accent, strokeWidth = 3.dp)
        Column {
            Text(stringResource(Res.string.mirror_host_starting_body), style = AppTheme.type.body, color = tokens.colors.muted)
            Text(stringResource(Res.string.mirror_host_starting_first), style = AppTheme.type.caption, color = tokens.colors.faint)
        }
    }
}

@Composable
private fun StoppedCard(onStart: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
        Text(stringResource(Res.string.mirror_host_stopped_body), style = AppTheme.type.body, color = tokens.colors.muted)
        PrimaryButton(
            text = stringResource(Res.string.mirror_host_start),
            onClick = onStart,
            leadingIcon = AppIcons.Play,
        )
    }
}

@Composable
private fun StartFailedCard(onRetry: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.lg)
    Column(
        Modifier.widthIn(max = 560.dp).clip(shape)
            .background(tokens.colors.error.copy(alpha = 0.06f)).border(1.dp, tokens.colors.error.copy(alpha = 0.30f), shape)
            .padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Text(stringResource(Res.string.mirror_host_no_port_title), style = AppTheme.type.bodyStrong, color = tokens.colors.error)
        Text(
            stringResource(
                Res.string.mirror_host_no_port_body,
                MIRROR_DEFAULT_PORT,
                MIRROR_DEFAULT_PORT + MIRROR_PORT_ATTEMPTS - 1,
            ),
            style = AppTheme.type.body.copy(fontSize = 13.sp),
            color = tokens.colors.muted,
        )
        SecondaryButton(text = stringResource(Res.string.mirror_host_try_again), onClick = onRetry)
    }
}

@Composable
private fun HostingContent(state: HostingUi.Hosting, onSelectIp: (String) -> Unit) {
    val tokens = AppTheme.tokens
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.lg)) {
        if (state.addresses.size > 1) {
            AddressChips(state.addresses, state.selectedIp, onSelectIp)
        }
        when {
            state.locked -> LockedCard()
            state.applied != null -> AppliedCard(state.applied)
            state.feed.any { it.event == MirrorHostEvent.Pulled } -> FeedCard(state.feed)
            else -> PairingArea(state)
        }
    }
}

@Composable
private fun AddressChips(addresses: List<LanAddress>, selectedIp: String, onSelect: (String) -> Unit) {
    val tokens = AppTheme.tokens
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
        addresses.forEach { address ->
            val selected = address.ip == selectedIp
            val shape = RoundedCornerShape(999.dp)
            val border = if (selected) tokens.colors.accent.copy(alpha = 0.45f) else tokens.colors.border
            Row(
                Modifier.clip(shape)
                    .background(if (selected) tokens.colors.accent.copy(alpha = 0.12f) else tokens.colors.surface)
                    .border(1.dp, border, shape)
                    .clickable { onSelect(address.ip) }
                    .padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
            ) {
                if (selected) {
                    Icon(AppIcons.Check, null, Modifier.size(13.dp), tint = tokens.colors.accent)
                }
                Text(
                    address.kind.label(),
                    style = AppTheme.type.caption.copy(fontSize = 10.5.sp),
                    color = if (selected) tokens.colors.accent else tokens.colors.faint,
                )
                Text(address.ip, style = AppTheme.type.caption.copy(fontSize = 12.sp), color = tokens.colors.text)
            }
        }
    }
}

@Composable
private fun LanAddressKind.label(): String = when (this) {
    LanAddressKind.WIFI -> stringResource(Res.string.mirror_net_wifi)
    LanAddressKind.ETHERNET -> stringResource(Res.string.mirror_net_ethernet)
    LanAddressKind.OTHER -> stringResource(Res.string.mirror_net_other)
    LanAddressKind.VIRTUAL -> stringResource(Res.string.mirror_net_virtual)
}

@Composable
private fun PairingArea(state: HostingUi.Hosting) {
    val tokens = AppTheme.tokens
    val payloadJson = remember(state.hosting, state.selectedIp) { state.hosting.payload(state.selectedIp).encode() }
    val qr = remember(payloadJson) { qrImageBitmap(payloadJson, QR_SIZE_PX) }
    val verifyCode = remember(state.hosting) { mirrorVerifyCode(state.hosting.fingerprint) }

    Row(horizontalArrangement = Arrangement.spacedBy(30.dp), verticalAlignment = Alignment.Top) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
        ) {
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(qr, null, Modifier.size(232.dp))
                Text(
                    verifyCode,
                    style = AppTheme.type.brand.copy(fontSize = 13.sp, letterSpacing = 2.5.sp),
                    color = Color(0xFF3A4150),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Text(
                stringResource(Res.string.mirror_host_qr_caption),
                style = AppTheme.type.caption,
                color = tokens.colors.faint,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 250.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
            StepsCard()
            FallbackCard(state, verifyCode)
            if (state.hosting.certProvenance == MirrorCertProvenance.GENERATED_FIRST) {
                InfoBand(
                    title = stringResource(Res.string.mirror_host_firewall_title),
                    body = stringResource(Res.string.mirror_host_firewall_body),
                    color = tokens.colors.warning,
                )
            }
            if (state.hosting.certProvenance == MirrorCertProvenance.REGENERATED) {
                InfoBand(title = null, body = stringResource(Res.string.mirror_host_identity_changed), color = AppTheme.tokens.colors.accent)
            }
            InfoBand(
                title = stringResource(Res.string.mirror_host_privacy_title),
                body = stringResource(Res.string.mirror_host_privacy_body),
                color = AppTheme.tokens.colors.accent,
            )
        }
    }
}

@Composable
private fun StepsCard() {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.lg)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape)
            .padding(tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Text(stringResource(Res.string.mirror_host_steps_title), style = AppTheme.type.bodyStrong, color = tokens.colors.text)
        listOf(
            stringResource(Res.string.mirror_host_step_1),
            stringResource(Res.string.mirror_host_step_2),
            stringResource(Res.string.mirror_host_step_3),
        ).forEachIndexed { index, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape)
                        .background(tokens.colors.accent.copy(alpha = 0.12f))
                        .border(1.dp, tokens.colors.accent.copy(alpha = 0.40f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", style = AppTheme.type.caption.copy(fontSize = 11.sp), color = tokens.colors.accent)
                }
                Text(step, style = AppTheme.type.body.copy(fontSize = 13.sp), color = tokens.colors.muted)
            }
        }
    }
}

@Composable
private fun FallbackCard(state: HostingUi.Hosting, verifyCode: String) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.lg)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape)
            .padding(tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Text(stringResource(Res.string.mirror_host_fallback_title), style = AppTheme.type.bodyStrong, color = tokens.colors.text)
        Text(stringResource(Res.string.mirror_host_fallback_subtitle), style = AppTheme.type.caption, color = tokens.colors.faint)
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.Bottom) {
            FallbackValue(stringResource(Res.string.mirror_host_fallback_address), state.selectedIp)
            FallbackValue(stringResource(Res.string.mirror_host_fallback_port), state.hosting.port.toString())
            PinDigits(stringResource(Res.string.mirror_host_fallback_pin), state.hosting.secret)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(tokens.colors.border))
        Text(
            stringResource(Res.string.mirror_host_fallback_note, verifyCode),
            style = AppTheme.type.caption,
            color = tokens.colors.muted,
        )
    }
}

@Composable
private fun FallbackValue(label: String, value: String) {
    val tokens = AppTheme.tokens
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = AppTheme.type.section, color = tokens.colors.faint)
        Text(value, style = AppTheme.type.brand.copy(fontSize = 17.sp), color = tokens.colors.text)
    }
}

@Composable
private fun PinDigits(label: String, secret: String) {
    val tokens = AppTheme.tokens
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = AppTheme.type.section, color = tokens.colors.faint)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            secret.chunked(3).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    group.forEach { digit ->
                        val shape = RoundedCornerShape(10.dp)
                        Box(
                            Modifier.width(34.dp).height(44.dp).clip(shape)
                                .background(Color.Black.copy(alpha = 0.3f))
                                .border(1.dp, tokens.colors.borderStrong, shape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$digit", style = AppTheme.type.brand.copy(fontSize = 19.sp), color = tokens.colors.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBand(title: String?, body: String, color: Color) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.lg)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(color.copy(alpha = 0.08f)).border(1.dp, color.copy(alpha = 0.28f), shape)
            .padding(tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.micro),
    ) {
        if (title != null) {
            Text(title, style = AppTheme.type.section, color = color)
        }
        Text(body, style = AppTheme.type.body.copy(fontSize = 12.5.sp), color = tokens.colors.muted)
    }
}

/** S3: the live feed while a Mirror is underway — pulled rows done, the push still pending. */
@Composable
private fun FeedCard(feed: List<FeedEntry>) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.lg)
    Column(
        Modifier.widthIn(max = 560.dp).clip(shape).background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape)
            .padding(tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        feed.forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Icon(AppIcons.Check, null, Modifier.size(15.dp), tint = OkGreen)
                Text(entry.feedLabel(), style = AppTheme.type.body.copy(fontSize = 13.sp), color = tokens.colors.muted, modifier = Modifier.weight(1f))
                Text(entry.time, style = AppTheme.type.caption, color = tokens.colors.faint)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            CircularProgressIndicator(Modifier.size(15.dp), color = tokens.colors.accent, strokeWidth = 2.dp)
            Text(
                stringResource(Res.string.mirror_host_feed_waiting_push),
                style = AppTheme.type.body.copy(fontSize = 13.sp),
                color = tokens.colors.muted,
            )
        }
    }
}

@Composable
private fun FeedEntry.feedLabel(): String = when (event) {
    is MirrorHostEvent.Paired -> stringResource(Res.string.mirror_host_feed_paired)
    else -> stringResource(Res.string.mirror_host_feed_pulled)
}

/** S4: the host-side summary once a push applied; counts are what changed on this computer. */
@Composable
private fun AppliedCard(applied: MirrorHostEvent.Applied) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.lg)
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        Column(
            Modifier.widthIn(max = 560.dp).clip(shape)
                .background(OkGreen.copy(alpha = 0.07f)).border(1.dp, OkGreen.copy(alpha = 0.30f), shape)
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Icon(AppIcons.Check, null, Modifier.size(18.dp), tint = OkGreen)
                Text(stringResource(Res.string.mirror_host_applied_title), style = AppTheme.type.bodyStrong, color = OkGreen)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xl)) {
                AppliedCount("+${applied.added}", stringResource(Res.string.mirror_host_applied_added), OkGreen)
                AppliedCount("${applied.updated}", stringResource(Res.string.mirror_host_applied_updated), AppTheme.tokens.colors.accent)
                AppliedCount("${applied.removed}", stringResource(Res.string.mirror_host_applied_removed), tokens.colors.error)
            }
        }
        Text(stringResource(Res.string.mirror_host_still_hosting), style = AppTheme.type.caption, color = tokens.colors.faint)
    }
}

@Composable
private fun AppliedCount(value: String, label: String, color: Color) {
    val tokens = AppTheme.tokens
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = AppTheme.type.brand.copy(fontSize = 22.sp), color = color)
        Text(label, style = AppTheme.type.section, color = tokens.colors.faint)
    }
}

/** S5: five wrong PINs locked pairing until the next hosting start. */
@Composable
private fun LockedCard() {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.lg)
    Column(
        Modifier.widthIn(max = 560.dp).clip(shape)
            .background(tokens.colors.warning.copy(alpha = 0.06f)).border(1.dp, tokens.colors.warning.copy(alpha = 0.30f), shape)
            .padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Text(stringResource(Res.string.mirror_host_pill_locked), style = AppTheme.type.bodyStrong, color = tokens.colors.warning)
        Text(stringResource(Res.string.mirror_host_locked_body), style = AppTheme.type.body.copy(fontSize = 13.sp), color = tokens.colors.muted)
    }
}

private const val QR_SIZE_PX = 640

private fun nowHhMm(): String {
    val time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    return "%02d:%02d".format(time.hour, time.minute)
}
