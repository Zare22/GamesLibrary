package hr.kotwave.gameslibrary.mirror

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import hr.kotwave.gameslibrary.mirror.wire.MIRROR_DEFAULT_PORT
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.cd_back
import hr.kotwave.gameslibrary.resources.common_cancel
import hr.kotwave.gameslibrary.resources.mirror_error_locked_body
import hr.kotwave.gameslibrary.resources.mirror_error_locked_title
import hr.kotwave.gameslibrary.resources.mirror_error_not_pairing_body
import hr.kotwave.gameslibrary.resources.mirror_error_not_pairing_title
import hr.kotwave.gameslibrary.resources.mirror_error_unreachable_firewall
import hr.kotwave.gameslibrary.resources.mirror_error_unreachable_hosting
import hr.kotwave.gameslibrary.resources.mirror_error_unreachable_title
import hr.kotwave.gameslibrary.resources.mirror_error_unreachable_wifi
import hr.kotwave.gameslibrary.resources.mirror_error_version_body
import hr.kotwave.gameslibrary.resources.mirror_error_version_title
import hr.kotwave.gameslibrary.resources.mirror_error_wrong_pin_body
import hr.kotwave.gameslibrary.resources.mirror_error_wrong_pin_title
import hr.kotwave.gameslibrary.resources.mirror_pair_camera_denied
import hr.kotwave.gameslibrary.resources.mirror_pair_privacy_footnote
import hr.kotwave.gameslibrary.resources.mirror_pair_scan_hint
import hr.kotwave.gameslibrary.resources.mirror_pair_title
import hr.kotwave.gameslibrary.resources.mirror_pair_type_instead
import hr.kotwave.gameslibrary.resources.mirror_paired_body
import hr.kotwave.gameslibrary.resources.mirror_paired_done
import hr.kotwave.gameslibrary.resources.mirror_paired_replaces
import hr.kotwave.gameslibrary.resources.mirror_paired_title
import hr.kotwave.gameslibrary.resources.mirror_settings_mirror_now
import hr.kotwave.gameslibrary.resources.mirror_settings_not_mirrored_yet
import hr.kotwave.gameslibrary.resources.mirror_settings_pair_subtitle
import hr.kotwave.gameslibrary.resources.mirror_settings_pair_title
import hr.kotwave.gameslibrary.resources.mirror_settings_paired_pill
import hr.kotwave.gameslibrary.resources.mirror_settings_paired_subtitle
import hr.kotwave.gameslibrary.resources.mirror_settings_paired_title
import hr.kotwave.gameslibrary.resources.mirror_settings_repair_pill
import hr.kotwave.gameslibrary.resources.mirror_settings_repair_subtitle
import hr.kotwave.gameslibrary.resources.mirror_settings_unpair
import hr.kotwave.gameslibrary.resources.mirror_typed_address
import hr.kotwave.gameslibrary.resources.mirror_typed_connect
import hr.kotwave.gameslibrary.resources.mirror_typed_footnote
import hr.kotwave.gameslibrary.resources.mirror_typed_pin
import hr.kotwave.gameslibrary.resources.mirror_typed_title
import hr.kotwave.gameslibrary.resources.mirror_unpair_body
import hr.kotwave.gameslibrary.resources.mirror_unpair_title
import hr.kotwave.gameslibrary.resources.mirror_verify_body
import hr.kotwave.gameslibrary.resources.mirror_verify_heading
import hr.kotwave.gameslibrary.resources.mirror_verify_hint
import hr.kotwave.gameslibrary.resources.mirror_verify_match
import hr.kotwave.gameslibrary.resources.mirror_verify_mismatch
import hr.kotwave.gameslibrary.resources.mirror_verify_title
import hr.kotwave.gameslibrary.resources.mirror_host_fallback_port
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.components.SecondaryButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.screens.HairlineDivider
import hr.kotwave.gameslibrary.ui.screens.ItemIcon
import hr.kotwave.gameslibrary.ui.screens.ItemText
import hr.kotwave.gameslibrary.ui.screens.SettingsCard
import hr.kotwave.gameslibrary.ui.screens.SettingsItem
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val OkGreen = Color(0xFF7DF0B6)
private val WarnAmber = Color(0xFFFFD24A)
private val ErrorRed = Color(0xFFF4707A)

/** The phone's pairing flow: scan → (typed → verify) → paired; errors E1–E5 overlay in place. */
@Composable
actual fun MirrorScreen(onBack: () -> Unit) {
    val viewModel: MirrorPairingViewModel = koinViewModel()
    val tokens = AppTheme.tokens
    Box(Modifier.fillMaxSize()) {
        when (val step = viewModel.step) {
            PairingStep.Scan -> ScanStep(viewModel, onBack)
            PairingStep.Typed -> TypedStep(viewModel, onBack = viewModel::backToScan)
            is PairingStep.Verify -> VerifyStep(step, viewModel)
            is PairingStep.Paired -> PairedStep(step, onDone = onBack)
        }
        viewModel.error?.let { error ->
            Box(
                Modifier.fillMaxSize().background(Color(0x99060810)).clickable(onClick = viewModel::dismissError),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(Modifier.padding(tokens.spacing.md)) { ErrorCard(error) }
            }
        }
        if (viewModel.busy) {
            Box(Modifier.fillMaxSize().background(Color(0x66060810)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = tokens.colors.accent)
            }
        }
    }
}

@Composable
private fun ScanStep(viewModel: MirrorPairingViewModel, onBack: () -> Unit) {
    val tokens = AppTheme.tokens
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
        denied = !result
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md)) {
        FlowHeader(stringResource(Res.string.mirror_pair_title), onBack)
        Spacer(Modifier.height(tokens.spacing.md))
        val frameShape = RoundedCornerShape(tokens.radii.xl)
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(frameShape)
                .background(Color.Black).border(1.dp, tokens.colors.border, frameShape),
            contentAlignment = Alignment.Center,
        ) {
            if (granted) {
                QrCameraPreview(onQr = viewModel::onQrScanned)
                Box(
                    Modifier.align(Alignment.Center).size(230.dp)
                        .border(2.dp, tokens.colors.accent.copy(alpha = 0.85f), RoundedCornerShape(26.dp)),
                )
                Text(
                    stringResource(Res.string.mirror_pair_scan_hint),
                    style = AppTheme.type.caption,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(tokens.spacing.lg),
                )
            } else if (denied) {
                Text(
                    stringResource(Res.string.mirror_pair_camera_denied),
                    style = AppTheme.type.body,
                    color = tokens.colors.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(tokens.spacing.xl),
                )
            }
        }
        Spacer(Modifier.height(tokens.spacing.md))
        Row(
            Modifier.fillMaxWidth().clickable(onClick = viewModel::startTyped).padding(tokens.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Sliders, null, Modifier.size(16.dp), tint = tokens.colors.accent)
            Text(stringResource(Res.string.mirror_pair_type_instead), style = AppTheme.type.bodyStrong, color = tokens.colors.accent)
        }
        Text(
            stringResource(Res.string.mirror_pair_privacy_footnote),
            style = AppTheme.type.caption,
            color = tokens.colors.faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = tokens.spacing.sm),
        )
    }
}

/** CameraX preview bound to the composition's lifecycle, decoding QR frames via ML Kit. */
@OptIn(ExperimentalGetImage::class)
@Composable
private fun QrCameraPreview(onQr: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    DisposableEffect(Unit) {
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
        val future = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val mediaImage = proxy.image
                if (mediaImage == null) {
                    proxy.close()
                    return@setAnalyzer
                }
                scanner.process(InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees))
                    .addOnSuccessListener { barcodes -> barcodes.firstOrNull()?.rawValue?.let(onQr) }
                    .addOnCompleteListener { proxy.close() }
            }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, executor)
        onDispose {
            runCatching { future.get().unbindAll() }
            scanner.close()
        }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun TypedStep(viewModel: MirrorPairingViewModel, onBack: () -> Unit) {
    val tokens = AppTheme.tokens
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(MIRROR_DEFAULT_PORT.toString()) }
    var pin by remember { mutableStateOf("") }
    val valid = address.isNotBlank() && port.toIntOrNull() != null && pin.length == 6

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
    ) {
        FlowHeader(stringResource(Res.string.mirror_typed_title), onBack)
        Spacer(Modifier.height(tokens.spacing.lg))
        PairingField(stringResource(Res.string.mirror_typed_address), address, { address = it.trim() }, KeyboardType.Uri)
        Spacer(Modifier.height(tokens.spacing.md))
        PairingField(stringResource(Res.string.mirror_host_fallback_port), port, { port = it.filter(Char::isDigit).take(5) }, KeyboardType.Number)
        Spacer(Modifier.height(tokens.spacing.md))
        PairingField(stringResource(Res.string.mirror_typed_pin), pin, { pin = it.filter(Char::isDigit).take(6) }, KeyboardType.NumberPassword)
        Spacer(Modifier.height(tokens.spacing.xl))
        PrimaryButton(
            text = stringResource(Res.string.mirror_typed_connect),
            onClick = { viewModel.onTypedConnect(address, port.toInt(), pin) },
            enabled = valid && !viewModel.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(tokens.spacing.sm))
        Text(
            stringResource(Res.string.mirror_typed_footnote),
            style = AppTheme.type.caption,
            color = tokens.colors.faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PairingField(label: String, value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.micro)) {
        Text(label, style = AppTheme.type.section, color = tokens.colors.faint)
        Box(
            Modifier.fillMaxWidth().clip(shape).background(tokens.colors.surface)
                .border(1.dp, tokens.colors.border, shape)
                .padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.sm),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = AppTheme.type.body.copy(color = tokens.colors.text),
                cursorBrush = SolidColor(tokens.colors.accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun VerifyStep(step: PairingStep.Verify, viewModel: MirrorPairingViewModel) {
    val tokens = AppTheme.tokens
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
    ) {
        FlowHeader(stringResource(Res.string.mirror_verify_title), onBack = viewModel::onVerifyRejected)
        Spacer(Modifier.height(tokens.spacing.xl))
        Text(
            stringResource(Res.string.mirror_verify_heading),
            style = AppTheme.type.display,
            color = tokens.colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(tokens.spacing.sm))
        Text(
            stringResource(Res.string.mirror_verify_body),
            style = AppTheme.type.body,
            color = tokens.colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(tokens.spacing.xl))
        Text(
            step.verifyCode,
            style = AppTheme.type.brand.copy(fontSize = 26.sp, letterSpacing = 4.sp),
            color = tokens.colors.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(tokens.spacing.xl))
        val shape = RoundedCornerShape(tokens.radii.lg)
        Text(
            stringResource(Res.string.mirror_verify_hint),
            style = AppTheme.type.caption,
            color = tokens.colors.muted,
            modifier = Modifier.fillMaxWidth().clip(shape)
                .background(tokens.colors.accent.copy(alpha = 0.08f))
                .border(1.dp, tokens.colors.accent.copy(alpha = 0.28f), shape)
                .padding(tokens.spacing.md),
        )
        Spacer(Modifier.height(tokens.spacing.xl))
        PrimaryButton(
            text = stringResource(Res.string.mirror_verify_match),
            onClick = viewModel::onVerifyConfirmed,
            leadingIcon = AppIcons.Check,
            enabled = !viewModel.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(tokens.spacing.sm))
        SecondaryButton(
            text = stringResource(Res.string.mirror_verify_mismatch),
            onClick = viewModel::onVerifyRejected,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PairedStep(step: PairingStep.Paired, onDone: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(Modifier.fillMaxSize().padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md)) {
        Column(
            Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape)
                    .background(OkGreen.copy(alpha = 0.12f)).border(1.dp, OkGreen.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Check, null, Modifier.size(34.dp), tint = OkGreen)
            }
            Spacer(Modifier.height(tokens.spacing.lg))
            Text(stringResource(Res.string.mirror_paired_title), style = AppTheme.type.display, color = tokens.colors.text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(tokens.spacing.sm))
            Text(step.endpoint, style = AppTheme.type.bodyStrong, color = tokens.colors.muted)
            Text(
                stringResource(Res.string.mirror_paired_body),
                style = AppTheme.type.body,
                color = tokens.colors.muted,
                textAlign = TextAlign.Center,
            )
            if (step.replacedPrevious) {
                Spacer(Modifier.height(tokens.spacing.sm))
                Text(stringResource(Res.string.mirror_paired_replaces), style = AppTheme.type.caption, color = tokens.colors.faint)
            }
        }
        PrimaryButton(
            text = stringResource(Res.string.mirror_settings_mirror_now),
            onClick = {},
            leadingIcon = AppIcons.Sync,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(tokens.spacing.sm))
        SecondaryButton(text = stringResource(Res.string.mirror_paired_done), onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FlowHeader(title: String, onBack: () -> Unit) {
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
        Text(title, style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
    }
}

@Composable
private fun ErrorCard(error: PairingError) {
    when (error) {
        is PairingError.WrongPin -> ErrorSpec(
            icon = AppIcons.Close,
            color = WarnAmber,
            title = stringResource(Res.string.mirror_error_wrong_pin_title),
            body = pluralStringResource(Res.plurals.mirror_error_wrong_pin_body, error.remainingAttempts, error.remainingAttempts),
        )

        PairingError.Locked -> ErrorSpec(
            icon = AppIcons.Close,
            color = ErrorRed,
            title = stringResource(Res.string.mirror_error_locked_title),
            body = stringResource(Res.string.mirror_error_locked_body),
        )

        is PairingError.Unreachable -> ErrorSpec(
            icon = AppIcons.Close,
            color = WarnAmber,
            title = stringResource(Res.string.mirror_error_unreachable_title, error.endpoint),
            body = null,
        ) {
            listOf(
                stringResource(Res.string.mirror_error_unreachable_wifi),
                stringResource(Res.string.mirror_error_unreachable_hosting),
                stringResource(Res.string.mirror_error_unreachable_firewall),
            ).forEach { fix ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.tokens.spacing.xs)) {
                    Icon(AppIcons.Check, null, Modifier.size(14.dp), tint = OkGreen)
                    Text(fix, style = AppTheme.type.body.copy(fontSize = 13.sp), color = AppTheme.tokens.colors.muted)
                }
            }
        }

        PairingError.NotPairingCode -> ErrorSpec(
            icon = AppIcons.Qr,
            color = WarnAmber,
            title = stringResource(Res.string.mirror_error_not_pairing_title),
            body = stringResource(Res.string.mirror_error_not_pairing_body),
        )

        PairingError.VersionMismatch -> ErrorSpec(
            icon = AppIcons.Sync,
            color = WarnAmber,
            title = stringResource(Res.string.mirror_error_version_title),
            body = stringResource(Res.string.mirror_error_version_body),
        )
    }
}

@Composable
private fun ErrorSpec(
    icon: ImageVector,
    color: Color,
    title: String,
    body: String?,
    extra: (@Composable () -> Unit)? = null,
) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.lg)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(tokens.colors.bg2)
            .border(1.dp, color.copy(alpha = 0.35f), shape).padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(tokens.radii.md))
                    .background(color.copy(alpha = 0.14f)).border(1.dp, color.copy(alpha = 0.40f), RoundedCornerShape(tokens.radii.md)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = color)
            }
            Text(title, style = AppTheme.type.bodyStrong, color = tokens.colors.text)
        }
        body?.let { Text(it, style = AppTheme.type.body.copy(fontSize = 13.sp), color = tokens.colors.muted) }
        extra?.invoke()
    }
}

/** Settings › Mirror on the phone: pair row (P1), the paired card (P2), or pair-again (P3). */
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun MirrorSettingsSection(onOpenMirror: () -> Unit) {
    val viewModel: MirrorPairingViewModel = koinViewModel()
    var confirmUnpair by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refreshPairedState() }

    when (val paired = viewModel.pairedState) {
        null -> SettingsCard {
            SettingsItem(
                icon = AppIcons.Qr,
                iconTint = AppTheme.tokens.colors.accent,
                title = stringResource(Res.string.mirror_settings_pair_title),
                subtitle = stringResource(Res.string.mirror_settings_pair_subtitle),
                onClick = onOpenMirror,
            )
        }

        else ->
            if (paired.needsRepair) {
                RepairCard(onOpenMirror)
            } else {
                PairedCard(paired, onUnpair = { confirmUnpair = true })
            }
    }

    if (confirmUnpair) {
        ModalBottomSheet(
            onDismissRequest = { confirmUnpair = false },
            containerColor = AppTheme.tokens.colors.bg2,
        ) {
            UnpairSheetContent(
                onUnpair = {
                    confirmUnpair = false
                    viewModel.unpair()
                },
                onCancel = { confirmUnpair = false },
            )
        }
    }
}

/** P2: the paired card — coordinates, last-Mirror line, and the (for now inert) Mirror actions. */
@Composable
private fun PairedCard(paired: PairedState, onUnpair: () -> Unit) {
    val tokens = AppTheme.tokens
    SettingsCard {
        Column(Modifier.padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                ItemIcon(AppIcons.Mirror, tokens.colors.accent)
                Box(Modifier.weight(1f)) {
                    ItemText(
                        title = stringResource(Res.string.mirror_settings_paired_title),
                        subtitle = paired.pairedAtLabel
                            ?.let { stringResource(Res.string.mirror_settings_paired_subtitle, paired.endpoint, it) }
                            ?: paired.endpoint,
                    )
                }
                StatusPill(stringResource(Res.string.mirror_settings_paired_pill), OkGreen)
            }
            HairlineDivider()
            Text(
                stringResource(Res.string.mirror_settings_not_mirrored_yet),
                style = AppTheme.type.caption,
                color = tokens.colors.faint,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                PrimaryButton(
                    text = stringResource(Res.string.mirror_settings_mirror_now),
                    onClick = {},
                    leadingIcon = AppIcons.Sync,
                    enabled = false,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = stringResource(Res.string.mirror_settings_unpair),
                    onClick = onUnpair,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** P3: pinning failed after a desktop reset — the row reopens the scanner. */
@Composable
private fun RepairCard(onOpenMirror: () -> Unit) {
    val tokens = AppTheme.tokens
    SettingsCard {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpenMirror).padding(tokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
        ) {
            ItemIcon(AppIcons.Mirror, WarnAmber)
            Box(Modifier.weight(1f)) {
                ItemText(
                    title = stringResource(Res.string.mirror_settings_paired_title),
                    subtitle = stringResource(Res.string.mirror_settings_repair_subtitle),
                )
            }
            StatusPill(stringResource(Res.string.mirror_settings_repair_pill), WarnAmber)
            Icon(AppIcons.ChevronRight, null, Modifier.size(17.dp), tint = tokens.colors.faint)
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier.clip(shape).background(color.copy(alpha = 0.10f)).border(1.dp, color.copy(alpha = 0.35f), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(text, style = AppTheme.type.caption.copy(fontSize = 10.5.sp), color = color)
    }
}

/** U1: the unpair confirmation sheet. */
@Composable
private fun UnpairSheetContent(onUnpair: () -> Unit, onCancel: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(
        Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Text(stringResource(Res.string.mirror_unpair_title), style = AppTheme.type.bodyStrong.copy(fontSize = 17.sp), color = tokens.colors.text)
        Text(stringResource(Res.string.mirror_unpair_body), style = AppTheme.type.body.copy(fontSize = 13.sp), color = tokens.colors.muted)
        Spacer(Modifier.height(tokens.spacing.xs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = onCancel, modifier = Modifier.weight(1f))
            PrimaryButton(text = stringResource(Res.string.mirror_settings_unpair), onClick = onUnpair, leadingIcon = AppIcons.Close, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(tokens.spacing.lg))
    }
}
