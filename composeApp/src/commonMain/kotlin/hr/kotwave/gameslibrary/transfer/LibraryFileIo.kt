package hr.kotwave.gameslibrary.transfer

import androidx.compose.runtime.Composable

/** Default filename suggested when exporting the library. */
const val LIBRARY_EXPORT_FILENAME = "GamesLibrary.json"

/**
 * Active, user-triggered file access for library export/import — unlike the passive paste-import
 * file-drop. Bound per platform (Android = Storage Access Framework; Desktop = an AWT file dialog).
 * The callbacks fire on the main thread.
 */
interface LibraryFileIo {

    /** Prompts for a save location and writes [content] there; [onResult] reports success (false if cancelled/failed). */
    fun export(suggestedName: String, content: String, onResult: (Boolean) -> Unit)

    /** Prompts to pick a file and reads its text; [onText] gets the text, or null if cancelled/unreadable. */
    fun import(onText: (String?) -> Unit)
}

/** The platform [LibraryFileIo], remembered across recompositions (it registers platform launchers). */
@Composable
expect fun rememberLibraryFileIo(): LibraryFileIo
