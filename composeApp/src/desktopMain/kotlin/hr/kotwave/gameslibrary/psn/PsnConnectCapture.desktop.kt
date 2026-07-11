package hr.kotwave.gameslibrary.psn

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.psn_capture_intro
import hr.kotwave.gameslibrary.resources.psn_capture_open_npsso
import hr.kotwave.gameslibrary.resources.psn_connect
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.net.URI

/**
 * Desktop PSN sign-in: the system browser opens PlayStation's site to sign in, a second click opens
 * the npsso page (same browser, same session), and the user pastes the shown value here.
 */
@Composable
actual fun PsnConnectCapture(signInUrl: String, npssoUrl: String, onNpsso: (String) -> Unit) {
    val tokens = AppTheme.tokens
    LaunchedEffect(signInUrl) {
        browse(signInUrl)
    }
    var pasted by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.psn_capture_intro),
            style = AppTheme.type.body.copy(fontSize = 13.sp),
            color = tokens.colors.muted,
        )
        Text(
            stringResource(Res.string.psn_capture_open_npsso),
            style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp),
            color = tokens.store.psnText,
            modifier = Modifier.clickable { browse(npssoUrl) },
        )
        OutlinedTextField(
            value = pasted,
            onValueChange = { pasted = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        PrimaryButton(
            text = stringResource(Res.string.psn_connect),
            onClick = { onNpsso(pasted) },
            enabled = pasted.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun browse(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}
