package hr.kotwave.gameslibrary.epic

import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.epic_capture_intro
import hr.kotwave.gameslibrary.resources.epic_capture_reopen
import hr.kotwave.gameslibrary.resources.epic_connect
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Android Epic sign-in: a Custom Tab opens Epic's sign-in, which lands on the page showing the
 * authorizationCode JSON, and the user pastes the shown value here.
 */
@Composable
actual fun EpicConnectCapture(signInUrl: String, onCode: (String) -> Unit) {
    val tokens = AppTheme.tokens
    val context = LocalContext.current
    LaunchedEffect(signInUrl) {
        CustomTabsIntent.Builder().build().launchUrl(context, signInUrl.toUri())
    }
    var pasted by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.epic_capture_intro),
            style = AppTheme.type.body.copy(fontSize = 13.sp),
            color = tokens.colors.muted,
        )
        Text(
            stringResource(Res.string.epic_capture_reopen),
            style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp),
            color = tokens.colors.text,
            modifier = Modifier.clickable {
                CustomTabsIntent.Builder().build().launchUrl(context, signInUrl.toUri())
            },
        )
        OutlinedTextField(
            value = pasted,
            onValueChange = { pasted = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        PrimaryButton(
            text = stringResource(Res.string.epic_connect),
            onClick = { onCode(pasted) },
            enabled = pasted.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
