package hr.kotwave.gameslibrary.steam

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * The platform browser leg of Steam OpenID sign-in: binds a loopback listener, opens a browser to
 * the URL built from the listener's ephemeral port, captures the redirect, and returns its `openid.*`
 * callback params (null if the user cancels or it times out). Building the `return_to` from the port
 * and verifying the params is `:shared` ([SteamBounce] + [SteamOpenId]). Android = a Custom Tab;
 * Desktop = the system browser.
 */
interface SteamAuthFlow {
    suspend fun authenticate(buildAuthUrl: (port: Int) -> String): Map<String, String>?
}

/** How long to wait for the OpenID redirect before giving up (the browser leg is user-driven). */
internal val AUTH_TIMEOUT: Duration = 3.minutes

/** Served to the browser once the redirect is captured, so the user knows to return to the app. */
internal const val STEAM_DONE_PAGE: String =
    """<!doctype html>
<html><head><meta charset="utf-8"><title>GamesLibrary</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  html,body{height:100%;margin:0}
  body{display:flex;align-items:center;justify-content:center;
    background:#0a0f17;color:#e7ecf3;font-family:system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
  .card{text-align:center;padding:40px 32px}
  .tick{font-size:44px;line-height:1;color:#46d39a}
  h1{font-size:20px;margin:18px 0 6px;font-weight:600}
  p{margin:0;color:#8b97a8;font-size:14px}
</style></head>
<body><div class="card">
  <div class="tick">&#10003;</div>
  <h1>Signed in to Steam</h1>
  <p>You can close this tab and return to GamesLibrary.</p>
</div></body></html>"""
