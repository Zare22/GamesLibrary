package hr.kotwave.gameslibrary.importer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds text shared into the app from an external entry point until the Import Intake consumes it.
 * A Koin single so the platform entry point (the Android share-sheet) and the Compose funnel share
 * one instance. Stays empty on Desktop, where file-drop feeds the Intake on-screen instead.
 */
class SharedTextInbox {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    /** Records text shared into the app, replacing any prior share not yet taken by the Intake. */
    fun offer(text: String) {
        _pending.value = text
    }

    /** Drops the pending share once the Intake has taken it. */
    fun clear() {
        _pending.value = null
    }
}
