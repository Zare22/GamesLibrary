package hr.kotwave.gameslibrary.importer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.data.sync.ImportEntry
import hr.kotwave.gameslibrary.data.sync.ImportSummary
import hr.kotwave.gameslibrary.data.sync.SyncReviewPick
import hr.kotwave.gameslibrary.data.sync.SyncTailRow
import hr.kotwave.gameslibrary.igdb.IgdbClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** Spacing between IGDB calls so a long paste stays under IGDB's request-rate ceiling. */
private val IGDB_THROTTLE = 280.milliseconds

/** Which leg of the paste Import funnel is on screen. */
sealed interface ImportPhase {
    data object Intake : ImportPhase
    data object Matching : ImportPhase
    data object Review : ImportPhase
    data class Done(val summary: ImportSummary) : ImportPhase
}

/** What a confirmed Review merges into: the paste Import, or a store sync's unmatched tail. */
sealed interface ImportTarget {
    data object Paste : ImportTarget
    data class SyncTail(val store: Store) : ImportTarget
}

/** A confirmed sync-tail Review's merge counts plus the uids it settled (added, attached, or dismissed). */
data class SyncReviewResult(val added: Int, val updated: Int, val handledUids: Set<String>)

/**
 * Drives the paste Import funnel: Intake (paste + Store) → Matching (name-search each line against
 * IGDB, classify) → Review (every Candidate, confident ones pre-checked) → Confirm. The VM does the
 * IGDB networking; the additive merge lives in [GameRepository.confirmImport] (`:shared`).
 */
class ImportViewModel(
    private val repository: GameRepository,
    private val igdbClient: IgdbClient,
) : ViewModel() {

    var phase by mutableStateOf<ImportPhase>(ImportPhase.Intake)
        private set

    var target by mutableStateOf<ImportTarget>(ImportTarget.Paste)
        private set

    /** The last confirmed sync-tail Review's outcome, for the hosting store screen to absorb. */
    var syncOutcome by mutableStateOf<SyncReviewResult?>(null)
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

    val dismissedCount: Int get() = candidates.count { it.dismissed }

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
        target = ImportTarget.Paste
        val lines = parserFor(selected).parse(pasteText)
        runMatching(lines)
    }

    /**
     * Catalog intake (the Battle.net picker): matches a fixed list of [titles] for [selected] through
     * the same funnel as a paste, bypassing the paste parser. Each title is its own line.
     */
    fun startFromTitles(selected: Store, titles: List<String>) {
        store = selected
        target = ImportTarget.Paste
        runMatching(titles.map { ParsedLine(title = it, raw = it) })
    }

    /**
     * Sync-tail intake: matches a sync's needs-review [rows] for [selected] through the same funnel,
     * carrying each row's store uids so Confirm can merge through the store's sync (`*_SYNC`).
     */
    fun startFromSyncTail(selected: Store, rows: List<SyncTailRow>) {
        store = selected
        target = ImportTarget.SyncTail(selected)
        runMatching(rows.map { ParsedLine(title = it.name, raw = it.name) }, rows.map { it.uids })
    }

    /**
     * Name-searches each [lines] title against IGDB and classifies it, advancing to Review. Throttled
     * and cancelable; an IGDB failure returns to Intake flagged. A no-op on empty input. [uidsPerLine]
     * is index-aligned with [lines] on a sync-tail run; empty for a paste.
     */
    private fun runMatching(lines: List<ParsedLine>, uidsPerLine: List<List<String>> = emptyList()) {
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
                    built += ImportCandidate(line.raw, classification, alreadyInLibrary, uidsPerLine.getOrElse(index) { emptyList() })
                    matchProgress = index + 1
                }
                candidates = built
                phase = ImportPhase.Review
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
                phase = ImportPhase.Intake
            }
        }
    }

    /**
     * Confirms the checked Candidates: hydrates each matched/picked line to its full IGDB metadata
     * ([IgdbClient.fetchGame]) and hands them, plus the unmatched ones, to the [target]'s merge —
     * [GameRepository.confirmImport] for a paste, [GameRepository.confirmSyncReview] (with the
     * dismissed rows) for a sync tail. Additive only. An IGDB failure leaves Review intact so the
     * user can retry.
     */
    fun confirm() {
        when (val current = target) {
            ImportTarget.Paste -> confirmPaste()
            is ImportTarget.SyncTail -> confirmSyncTail(current.store)
        }
    }

    private fun confirmPaste() {
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
            } catch (_: Exception) {
                failed = true
            } finally {
                importing = false
            }
        }
    }

    private fun confirmSyncTail(selected: Store) {
        if (importing) return
        val checked = candidates.filter { it.checked }
        val dismissed = candidates.filter { it.dismissed }
        if (checked.isEmpty() && dismissed.isEmpty()) return
        confirmJob?.cancel()
        failed = false
        importing = true
        confirmJob = viewModelScope.launch {
            try {
                val picks = ArrayList<SyncReviewPick>(checked.size)
                val bare = ArrayList<SyncTailRow>()
                checked.forEachIndexed { index, candidate ->
                    val result = candidate.resolved
                    if (result != null) {
                        if (index > 0) delay(IGDB_THROTTLE)
                        val full = igdbClient.fetchGame(result.igdbId) ?: return@forEachIndexed
                        picks += SyncReviewPick(full, candidate.uids)
                    } else {
                        bare += SyncTailRow(candidate.rawTitle, candidate.uids)
                    }
                }
                val dismissedRows = dismissed.map { SyncTailRow(it.rawTitle, it.uids) }
                val outcome = repository.confirmSyncReview(selected, picks, bare, dismissedRows)
                val handled = (picks.flatMap { it.uids } + bare.flatMap { it.uids } + dismissedRows.flatMap { it.uids }).toSet()
                syncOutcome = SyncReviewResult(outcome.added, outcome.updated, handled)
                candidates = emptyList()
                phase = ImportPhase.Intake
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
            } finally {
                importing = false
            }
        }
    }

    /** Marks the last sync-tail outcome absorbed by the hosting store screen. */
    fun consumeSyncOutcome() {
        syncOutcome = null
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
        target = ImportTarget.Paste
        candidates = emptyList()
        matchProgress = 0
        matchTotal = 0
        failed = false
        importing = false
        phase = ImportPhase.Intake
    }
}
