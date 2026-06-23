package hr.kotwave.gameslibrary.gog

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
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import java.awt.Desktop
import java.net.URI

/**
 * Desktop GOG sign-in: the system browser opens GOG's login; after signing in the user copies the
 * resulting `on_login_success?...` address and pastes it here (no native Chromium — ADR 0020).
 */
@Composable
actual fun GogConnectCapture(authUrl: String, onRedirect: (String) -> Unit) {
    val tokens = AppTheme.tokens
    LaunchedEffect(authUrl) {
        runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            }
        }
    }
    var pasted by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "A browser opened for GOG sign-in. After you sign in, copy the page address (it contains " +
                "“on_login_success”) and paste it below.",
            style = AppTheme.type.body.copy(fontSize = 13.sp),
            color = tokens.colors.muted,
        )
        OutlinedTextField(
            value = pasted,
            onValueChange = { pasted = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        PrimaryButton(
            text = "Connect",
            onClick = { onRedirect(pasted) },
            enabled = pasted.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
