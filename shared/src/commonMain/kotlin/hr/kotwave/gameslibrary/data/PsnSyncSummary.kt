package hr.kotwave.gameslibrary.data

/** The outcome of a PSN sync: how many Games were newly added vs already present (ownership ensured). */
data class PsnSyncSummary(val added: Int, val updated: Int) {
    val total: Int get() = added + updated
}
