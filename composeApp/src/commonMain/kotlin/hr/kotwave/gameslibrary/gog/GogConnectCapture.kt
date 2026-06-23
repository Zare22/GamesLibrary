package hr.kotwave.gameslibrary.gog

import androidx.compose.runtime.Composable

/**
 * The platform browser/redirect-capture leg of GOG sign-in: opens [authUrl] and surfaces the post-login
 * redirect (a full `on_login_success?...&code=` URL, or a pasted one) via [onRedirect]. Android renders an
 * in-app WebView that intercepts the redirect; Desktop opens the system browser and takes a pasted URL (no
 * native Chromium — ADR 0020). Parsing the code out of the redirect is `:shared` ([GogAuth.extractCode]).
 */
@Composable
expect fun GogConnectCapture(authUrl: String, onRedirect: (String) -> Unit)

/** GOG's post-login redirect; the auth `code` rides on its query (ADR 0020). */
internal const val GOG_REDIRECT_PREFIX = "https://embed.gog.com/on_login_success"
