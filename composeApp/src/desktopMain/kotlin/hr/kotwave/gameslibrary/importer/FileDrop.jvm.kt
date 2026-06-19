package hr.kotwave.gameslibrary.importer

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import java.awt.datatransfer.DataFlavor
import java.io.File

private val TEXT_EXTENSIONS = setOf("txt", "csv")

/** Accepts a dropped .txt/.csv file (or several) and hands the concatenated text to [onText]. */
@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.importFileDrop(onText: (String) -> Unit): Modifier = composed {
    val target = object : DragAndDropTarget {
        override fun onDrop(event: DragAndDropEvent): Boolean {
            val transferable = event.awtTransferable
            if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false
            @Suppress("UNCHECKED_CAST")
            val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
            val text = files
                .filter { it.extension.lowercase() in TEXT_EXTENSIONS }
                .joinToString("\n") { it.readText() }
            if (text.isBlank()) return false
            onText(text)
            return true
        }
    }
    dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
            event.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
        },
        target = target,
    )
}
