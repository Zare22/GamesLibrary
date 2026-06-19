package hr.kotwave.gameslibrary.data

/** The outcome of a Steam sync: how many Games were newly added vs already present (ownership ensured). */
data class SteamSyncSummary(val added: Int, val updated: Int) {
    val total: Int get() = added + updated
}
