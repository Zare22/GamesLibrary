package hr.kotwave.gameslibrary.importer

import androidx.compose.ui.Modifier

/**
 * Makes this area a drop target for a text file (Desktop), handing its text to [onText]. A no-op on
 * Android, where the share-sheet is the external entry point (deferred to the platform-shim batch).
 */
expect fun Modifier.importFileDrop(onText: (String) -> Unit): Modifier
