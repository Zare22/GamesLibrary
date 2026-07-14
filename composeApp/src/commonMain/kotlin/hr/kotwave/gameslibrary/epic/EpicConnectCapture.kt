package hr.kotwave.gameslibrary.epic

import androidx.compose.runtime.Composable

/**
 * The platform browser leg of Epic sign-in: opens [signInUrl] (Epic sign-in bouncing to the page
 * that renders `{"authorizationCode":"…"}`), offers a tap-through to reopen it, and surfaces the
 * pasted value via [onCode]. Both platforms open the system browser (a Custom Tab on Android, the
 * desktop system browser) — never a WebView, which Epic's hCaptcha breaks. Extracting the code out
 * of the paste is `:shared` ([EpicAuth.extractAuthorizationCode]).
 */
@Composable
expect fun EpicConnectCapture(signInUrl: String, onCode: (String) -> Unit)
