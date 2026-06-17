package hr.kotwave.gameslibrary.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

enum class AddMode { OWN, WISHLIST }

/**
 * Form state for manually adding a Game. Drives both the phone full-screen Add and the desktop
 * Add modal, so it depends only on [GameRepository] — no ViewModelStoreOwner.
 */
@Stable
class AddGameState(
    private val repository: GameRepository,
    private val scope: CoroutineScope,
) {
    var title by mutableStateOf("")
        private set
    var mode by mutableStateOf(AddMode.OWN)
        private set
    var selectedStores by mutableStateOf<Set<Store>>(emptySet())
        private set
    var status by mutableStateOf(Status.BACKLOG)
        private set

    /** An existing Game title that matches case-insensitively, for the soft duplicate warning. */
    var similarTitle by mutableStateOf<String?>(null)
        private set

    val canSave: Boolean get() = title.isNotBlank()

    private var similarJob: Job? = null

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
        val name = title.trim()
        scope.launch {
            when (mode) {
                AddMode.WISHLIST -> repository.addWishlistGame(name)
                AddMode.OWN -> repository.addOwnedGame(name = name, status = status, stores = selectedStores)
            }
            onSaved()
        }
    }
}

@Composable
fun rememberAddGameState(repository: GameRepository = koinInject()): AddGameState {
    val scope = rememberCoroutineScope()
    return remember(repository) { AddGameState(repository, scope) }
}
