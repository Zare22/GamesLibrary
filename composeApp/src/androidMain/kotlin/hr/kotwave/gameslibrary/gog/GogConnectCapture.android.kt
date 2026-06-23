package hr.kotwave.gameslibrary.gog

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Android GOG sign-in: a WebView loads GOG's login and intercepts the redirect to `on_login_success`,
 * handing the full URL (with `?code=`) back. JavaScript is on because GOG's login page needs it.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun GogConnectCapture(authUrl: String, onRedirect: (String) -> Unit) {
    val fired = remember { booleanArrayOf(false) }
    fun capture(url: String?): Boolean {
        if (url != null && !fired[0] && url.startsWith(GOG_REDIRECT_PREFIX)) {
            fired[0] = true
            onRedirect(url)
            return true
        }
        return false
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(440.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                        capture(request?.url?.toString())

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        if (capture(url)) view?.stopLoading()
                    }
                }
                loadUrl(authUrl)
            }
        },
    )
}
