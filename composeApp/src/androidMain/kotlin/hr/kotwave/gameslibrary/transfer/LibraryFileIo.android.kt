package hr.kotwave.gameslibrary.transfer

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val JSON_MIME = "application/json"

/** MIME types accepted when opening; widened past `application/json` so mislabelled files stay pickable. */
private val OPEN_MIME_TYPES = arrayOf("application/json", "text/json", "application/octet-stream", "text/plain")

@Composable
actual fun rememberLibraryFileIo(): LibraryFileIo {
    val context = LocalContext.current
    val io = remember(context) { AndroidLibraryFileIo(context.applicationContext) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(JSON_MIME),
    ) { uri -> io.onCreateResult(uri) }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> io.onOpenResult(uri) }

    io.bind(createLauncher, openLauncher)
    return io
}

/**
 * Drives Android SAF: `CreateDocument` returns the destination Uri only after the user picks, so the
 * content and result callback are held until then; `OpenDocument` likewise delivers the source Uri.
 */
private class AndroidLibraryFileIo(private val appContext: Context) : LibraryFileIo {

    private var createLauncher: ActivityResultLauncher<String>? = null
    private var openLauncher: ActivityResultLauncher<Array<String>>? = null

    private var pendingContent: String? = null
    private var exportResult: ((Boolean) -> Unit)? = null
    private var importText: ((String?) -> Unit)? = null

    fun bind(create: ActivityResultLauncher<String>, open: ActivityResultLauncher<Array<String>>) {
        createLauncher = create
        openLauncher = open
    }

    override fun export(suggestedName: String, content: String, onResult: (Boolean) -> Unit) {
        pendingContent = content
        exportResult = onResult
        createLauncher?.launch(suggestedName) ?: onResult(false)
    }

    override fun import(onText: (String?) -> Unit) {
        importText = onText
        openLauncher?.launch(OPEN_MIME_TYPES) ?: onText(null)
    }

    fun onCreateResult(uri: Uri?) {
        val content = pendingContent
        val callback = exportResult
        pendingContent = null
        exportResult = null
        if (uri == null || content == null) {
            callback?.invoke(false)
            return
        }
        val ok = runCatching {
            appContext.contentResolver.openOutputStream(uri)?.use { it.write(content.encodeToByteArray()) }
                ?: error("no output stream")
        }.isSuccess
        callback?.invoke(ok)
    }

    fun onOpenResult(uri: Uri?) {
        val callback = importText
        importText = null
        if (uri == null) {
            callback?.invoke(null)
            return
        }
        val text = runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        }.getOrNull()
        callback?.invoke(text)
    }
}
