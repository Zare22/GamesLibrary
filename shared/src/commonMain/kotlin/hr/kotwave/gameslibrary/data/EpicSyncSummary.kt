package hr.kotwave.gameslibrary.data

/** The outcome of an Epic sync: how many Games were newly added vs already present (ownership ensured). */
data class EpicSyncSummary(val added: Int, val updated: Int) {
    val total: Int get() = added + updated
}
