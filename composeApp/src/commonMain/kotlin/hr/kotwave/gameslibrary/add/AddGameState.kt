package hr.kotwave.gameslibrary.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.IgdbGame
import hr.kotwave.gameslibrary.data.IgdbSearchResult
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.igdb.IgdbClient
import hr.kotwave.gameslibrary.search.IgdbSearchState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

enum class AddMode { OWN, WISHLIST }

/**
 * State for the Add screen: IGDB search up top, then an adding card for the picked result (or a
 * manual fallback). Drives both the phone full-screen Add and the desktop Add modal, so it depends
 * only on [GameRepository] + [IgdbClient] — no ViewModelStoreOwner.
 */
@Stable
class AddGameState(
    private val repository: GameRepository,
    private val igdbClient: IgdbClient,
    private val scope: CoroutineScope,
) {
    private val search = IgdbSearchState(igdbClient, scope)

    val query: String get() = search.query
    val results: List<IgdbSearchResult> get() = search.results
    val searching: Boolean get() = search.searching
    val searchFailed: Boolean get() = search.searchFailed

    /** The picked IGDB result, fully fetched, being configured for add. Null in the manual path. */
    var selected by mutableStateOf<IgdbGame?>(null)
        private set
    var loadingSelection by mutableStateOf(false)
        private set

    /** True when adding a raw title by hand (no IGDB match). */
    var manualEntry by mutableStateOf(false)
        private set

    /** Set after a matched add that turned out to already exist, so the UI can say so. */
    var alreadyInLibrary by mutableStateOf(false)
        private set

    var mode by mutableStateOf(AddMode.OWN)
        private set
    var selectedStores by mutableStateOf<Set<Store>>(emptySet())
        private set
    var status by mutableStateOf(Status.BACKLOG)
        private set

    /** A manual-entry title that matches an existing Game case-insensitively. */
    var similarTitle by mutableStateOf<String?>(null)
        private set

    /** True once a game is picked (matched) or manual entry is started. */
    val configuring: Boolean get() = selected != null || manualEntry

    val canSave: Boolean get() = selected != null || (manualEntry && title.isNotBlank())

    var title by mutableStateOf("")
        private set

    private var similarJob: Job? = null

    fun updateQuery(value: String) {
        selected = null
        manualEntry = false
        alreadyInLibrary = false
        search.updateQuery(value)
    }

    fun selectResult(result: IgdbSearchResult) {
        search.cancelPending()
        manualEntry = false
        alreadyInLibrary = false
        loadingSelection = true
        scope.launch {
            selected = try {
                igdbClient.fetchGame(result.igdbId) ?: result.toFallbackGame()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                result.toFallbackGame()
            } finally {
                loadingSelection = false
            }
        }
    }

    /** Switch to adding the typed query as a raw, unmatched Game. */
    fun addManually() {
        search.cancelPending()
        selected = null
        alreadyInLibrary = false
        manualEntry = true
        updateTitle(query.trim())
    }

    /** Return from the adding card to the search results. */
    fun clearSelection() {
        selected = null
        manualEntry = false
        alreadyInLibrary = false
    }

    fun updateTitle(value: String) {
        title = value
        similarJob?.cancel()
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            similarTitle = null
            return
        }
        similarJob = scope.launch {
            delay(250.milliseconds)
            similarTitle = repository.similarTitles(trimmed).firstOrNull()?.name
        }
    }

    fun selectMode(value: AddMode) {
        mode = value
    }

    fun toggleStore(store: Store) {
        selectedStores = if (store in selectedStores) selectedStores - store else selectedStores + store
    }

    fun selectStatus(value: Status) {
        status = value
    }

    fun save(onSaved: () -> Unit) {
        if (!canSave) return
        val matched = selected
        val wishlist = mode == AddMode.WISHLIST
        val stores = if (wishlist) emptySet() else selectedStores
        scope.launch {
            if (matched != null) {
                val result = repository.addMatchedGame(matched, wishlist = wishlist, status = status, stores = stores)
                if (result.alreadyExisted) alreadyInLibrary = true else onSaved()
            } else {
                val name = title.trim()
                when (mode) {
                    AddMode.WISHLIST -> repository.addWishlistGame(name)
                    AddMode.OWN -> repository.addOwnedGame(name = name, status = status, stores = selectedStores)
                }
                onSaved()
            }
        }
    }
}

private fun IgdbSearchResult.toFallbackGame(): IgdbGame =
    IgdbGame(
        igdbId = igdbId,
        name = name,
        coverImageId = coverImageId,
        firstReleaseDate = firstReleaseDate,
        developer = developer,
    )

@Composable
fun rememberAddGameState(
    repository: GameRepository = koinInject(),
    igdbClient: IgdbClient = koinInject(),
): AddGameState {
    val scope = rememberCoroutineScope()
    return remember(repository, igdbClient) { AddGameState(repository, igdbClient, scope) }
}
