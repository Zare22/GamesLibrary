package hr.kotwave.gameslibrary.gog

import androidx.compose.runtime.Composable

/**
 * The platform browser leg of GOG sign-in: opens [authUrl] and surfaces the post-login redirect URL
 * (a full `on_login_success?...&code=`) via [onRedirect]. Both platforms open the system browser (a Custom
 * Tab on Android, the desktop system browser) and take a pasted redirect URL — GOG's login won't complete
 * in an embedded WebView (reCAPTCHA/anti-bot), so the earlier Android WebView (ADR 0020) was dropped.
 * Parsing the code out of the redirect is `:shared` ([GogAuth.extractCode]).
 */
@Composable
expect fun GogConnectCapture(authUrl: String, onRedirect: (String) -> Unit)
