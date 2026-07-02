package hr.kotwave.gameslibrary.importer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.ImportEntry
import hr.kotwave.gameslibrary.data.ImportSummary
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.igdb.IgdbClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** Spacing between IGDB calls so a long paste stays under IGDB's request-rate ceiling (ADR 0005). */
private val IGDB_THROTTLE = 280.milliseconds

/** Which leg of the paste Import funnel is on screen. */
sealed interface ImportPhase {
    data object Intake : ImportPhase
    data object Matching : ImportPhase
    data object Review : ImportPhase
    data class Done(val summary: ImportSummary) : ImportPhase
}

/**
 * Drives the paste Import funnel: Intake (paste + Store) → Matching (name-search each line against
 * IGDB, classify) → Review (every Candidate, confident ones pre-checked) → Confirm. The VM does the
 * IGDB networking; the additive merge lives in [GameRepository.confirmImport] (`:shared`, ADR 0006).
 */
class ImportViewModel(
    private val repository: GameRepository,
    private val igdbClient: IgdbClient,
) : ViewModel() {

    var phase by mutableStateOf<ImportPhase>(ImportPhase.Intake)
        private set

    var pasteText by mutableStateOf("")
        private set

    var store by mutableStateOf<Store?>(null)
        private set

    var candidates by mutableStateOf<List<ImportCandidate>>(emptyList())
        private set

    /** Lines matched so far / total, for the parsing progress (the `importer-parsing` state). */
    var matchProgress by mutableStateOf(0)
        private set
    var matchTotal by mutableStateOf(0)
        private set

    /** Matching or Confirm couldn't reach IGDB — returns to the prior screen so the user can retry. */
    var failed by mutableStateOf(false)
        private set

    var importing by mutableStateOf(false)
        private set

    private var matchJob: Job? = null
    private var confirmJob: Job? = null

    /** Non-blank lines in the paste box, for the live "N lines" counter on Intake. */
    val lineCount: Int get() = pasteText.lineSequence().count { it.isNotBlank() }

    val canParse: Boolean get() = store != null && pasteText.isNotBlank()

    val checkedCount: Int get() = candidates.count { it.checked }

    fun updateText(value: String) {
        pasteText = value
    }

    /**
     * Loads text shared into the app from an external entry point (the Android share-sheet),
     * replacing the paste and returning to Intake so the user picks a Store and Parses.
     */
    fun receiveSharedText(text: String) {
        matchJob?.cancel()
        candidates = emptyList()
        failed = false
        pasteText = text
        phase = ImportPhase.Intake
    }

    fun selectStore(value: Store) {
        store = value
    }

    /**
     * Parses the paste with the Store's parser, then name-searches each line against IGDB and
     * classifies it. Throttled and cancelable; an IGDB failure returns to Intake flagged.
     */
    fun parse() {
        val selected = store ?: return
        val lines = parserFor(selected).parse(pasteText)
        runMatching(lines)
    }

    /**
     * Catalog intake (the Battle.net picker): matches a fixed list of [titles] for [selected] through
     * the same funnel as a paste, bypassing the paste parser. Each title is its own line.
     */
    fun startFromTitles(selected: Store, titles: List<String>) {
        store = selected
        runMatching(titles.map { ParsedLine(title = it, raw = it) })
    }

    /**
     * Name-searches each [lines] title against IGDB and classifies it, advancing to Review. Throttled
     * and cancelable; an IGDB failure returns to Intake flagged. A no-op on empty input.
     */
    private fun runMatching(lines: List<ParsedLine>) {
        if (lines.isEmpty()) return
        matchJob?.cancel()
        failed = false
        matchProgress = 0
        matchTotal = lines.size
        phase = ImportPhase.Matching
        matchJob = viewModelScope.launch {
            try {
                val built = ArrayList<ImportCandidate>(lines.size)
                lines.forEachIndexed { index, line ->
                    if (index > 0) delay(IGDB_THROTTLE)
                    val classification = classifyMatch(line.title, igdbClient.searchGames(line.title))
                    val alreadyInLibrary = classification is MatchClassification.Unmatched &&
                        repository.similarTitles(line.title).isNotEmpty()
                    built += ImportCandidate(line.raw, classification, alreadyInLibrary)
                    matchProgress = index + 1
                }
                candidates = built
                phase = ImportPhase.Review
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed = true
                phase = ImportPhase.Intake
            }
        }
    }

    /**
     * Confirms the checked Candidates: hydrates each matched/picked line to its full IGDB metadata
     * ([IgdbClient.fetchGame]) and hands them, plus the unmatched ones, to [GameRepository.confirmImport].
     * Additive only. An IGDB failure leaves Review intact so the user can retry.
     */
    fun confirm() {
        if (importing) return
        val selected = store ?: return
        val checked = candidates.filter { it.checked }
        if (checked.isEmpty()) return
        confirmJob?.cancel()
        failed = false
        importing = true
        confirmJob = viewModelScope.launch {
            try {
                val entries = ArrayList<ImportEntry>(checked.size)
                checked.forEachIndexed { index, candidate ->
                    val result = candidate.resolved
                    if (result != null) {
                        if (index > 0) delay(IGDB_THROTTLE)
                        val full = igdbClient.fetchGame(result.igdbId) ?: return@forEachIndexed
                        entries += ImportEntry.Matched(full, selected)
                    } else {
                        entries += ImportEntry.Unmatched(candidate.rawTitle, selected)
                    }
                }
                phase = ImportPhase.Done(repository.confirmImport(entries))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed = true
            } finally {
                importing = false
            }
        }
    }

    /** Returns from Matching/Review to Intake (cancelling any in-flight matching), keeping the paste. */
    fun backToIntake() {
        matchJob?.cancel()
        candidates = emptyList()
        failed = false
        phase = ImportPhase.Intake
    }

    /** Clears everything for a fresh Import after one completes. */
    fun reset() {
        matchJob?.cancel()
        confirmJob?.cancel()
        pasteText = ""
        store = null
        candidates = emptyList()
        matchProgress = 0
        matchTotal = 0
        failed = false
        importing = false
        phase = ImportPhase.Intake
    }
}
