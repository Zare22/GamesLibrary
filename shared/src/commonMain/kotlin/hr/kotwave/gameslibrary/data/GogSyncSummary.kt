package hr.kotwave.gameslibrary.data

/** The outcome of a GOG sync: how many Games were newly added vs already present (ownership ensured). */
data class GogSyncSummary(val added: Int, val updated: Int) {
    val total: Int get() = added + updated
}
