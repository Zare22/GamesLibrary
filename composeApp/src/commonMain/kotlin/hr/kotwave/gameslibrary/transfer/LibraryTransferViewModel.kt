package hr.kotwave.gameslibrary.transfer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.ImportSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Which leg of the library-import flow is on screen. */
sealed interface TransferPhase {
    /** Waiting for the file picker (the screen triggers it once on entry). */
    data object Picking : TransferPhase
    data object Working : TransferPhase
    data object Review : TransferPhase
    data class Done(val summary: ImportSummary) : TransferPhase
    data class Failed(val message: String) : TransferPhase
}

/**
 * One imported game on its way through Review: its [row] classification, a user-controlled [checked]
 * state (everything pre-checked; uncheck to skip), and — for a [ImportRowKind.TitleCollision] row —
 * whether to [mergeByTitle] onto the existing same-titled Game or add it as new.
 */
@Stable
class LibraryImportCandidate(val row: LibraryImportRow) {
    var checked by mutableStateOf(true)
    var mergeByTitle by mutableStateOf(true)

    val decision: LibraryImportDecision get() = LibraryImportDecision(row.game, mergeByTitle)
}

/**
 * Drives library export and import. Export builds the JSON (the screen hands it to [LibraryFileIo]);
 * import decodes + classifies a picked file against the current library, lets the user review per
 * row, then applies it additively via [GameRepository.importLibrary] (ADR 0006). Mirrors the paste
 * [hr.kotwave.gameslibrary.importer.ImportViewModel] phase machine.
 */
class LibraryTransferViewModel(private val repository: GameRepository) : ViewModel() {

    var phase by mutableStateOf<TransferPhase>(TransferPhase.Picking)
        private set

    var candidates by mutableStateOf<List<LibraryImportCandidate>>(emptyList())
        private set

    var importing by mutableStateOf(false)
        private set

    val checkedCount: Int get() = candidates.count { it.checked }

    /** Serializes the library for export; the screen passes the result to [LibraryFileIo.export]. */
    suspend fun buildExport(): String = repository.exportLibrary()

    /** Decodes + classifies the picked file's [text], moving to Review (or Failed on a bad/empty file). */
    fun onFilePicked(text: String) {
        phase = TransferPhase.Working
        viewModelScope.launch {
            val export = try {
                LibraryTransfer.decode(text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                phase = TransferPhase.Failed("That doesn't look like a GamesLibrary export file.")
                return@launch
            }
            val rows = repository.classifyImport(export)
            if (rows.isEmpty()) {
                phase = TransferPhase.Failed("The file has no games to import.")
                return@launch
            }
            candidates = rows.map { LibraryImportCandidate(it) }
            phase = TransferPhase.Review
        }
    }

    /** Applies the checked candidates additively. An empty selection is a no-op. */
    fun confirm() {
        if (importing) return
        val decisions = candidates.filter { it.checked }.map { it.decision }
        if (decisions.isEmpty()) return
        importing = true
        viewModelScope.launch {
            try {
                phase = TransferPhase.Done(repository.importLibrary(decisions))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                phase = TransferPhase.Failed("Import failed — your library is unchanged.")
            } finally {
                importing = false
            }
        }
    }
}
