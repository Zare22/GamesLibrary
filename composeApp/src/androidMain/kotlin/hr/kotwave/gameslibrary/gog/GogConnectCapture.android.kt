package hr.kotwave.gameslibrary.gog

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.theme.AppTheme

/**
 * Android GOG sign-in: a Custom Tab opens GOG's login, then the user copies the resulting
 * `on_login_success?...` address and pastes it here. A bare WebView can't be used — GOG's reCAPTCHA/
 * anti-bot won't complete the login embedded — so this uses the same browser+paste model as Desktop.
 */
@Composable
actual fun GogConnectCapture(authUrl: String, onRedirect: (String) -> Unit) {
    val tokens = AppTheme.tokens
    val context = LocalContext.current
    LaunchedEffect(authUrl) {
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(authUrl))
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
