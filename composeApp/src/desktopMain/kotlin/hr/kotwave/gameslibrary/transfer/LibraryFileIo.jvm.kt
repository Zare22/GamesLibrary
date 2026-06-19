package hr.kotwave.gameslibrary.transfer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberLibraryFileIo(): LibraryFileIo = remember { DesktopLibraryFileIo() }

/** Desktop file access via a native AWT [FileDialog] (SAVE for export, LOAD for import). */
private class DesktopLibraryFileIo : LibraryFileIo {

    override fun export(suggestedName: String, content: String, onResult: (Boolean) -> Unit) {
        val dialog = FileDialog(null as Frame?, "Export library", FileDialog.SAVE).apply {
            file = suggestedName
            isVisible = true
        }
        val chosen = dialog.toFileOrNull()
        if (chosen == null) {
            onResult(false)
            return
        }
        onResult(runCatching { chosen.writeText(content) }.isSuccess)
    }

    override fun import(onText: (String?) -> Unit) {
        val dialog = FileDialog(null as Frame?, "Import library", FileDialog.LOAD).apply {
            file = "*.json"
            isVisible = true
        }
        val chosen = dialog.toFileOrNull()
        if (chosen == null) {
            onText(null)
            return
        }
        onText(runCatching { chosen.readText() }.getOrNull())
    }

    /** The selected file, or null if the dialog was cancelled. */
    private fun FileDialog.toFileOrNull(): File? {
        val dir = directory ?: return null
        val name = file ?: return null
        return File(dir, name)
    }
}
