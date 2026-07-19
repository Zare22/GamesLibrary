package hr.kotwave.gameslibrary.data.sync

/** The outcome of a store sync: how many Games were newly added vs already present (ownership ensured). */
data class SyncSummary(val added: Int, val updated: Int) {
    val total: Int get() = added + updated
}
