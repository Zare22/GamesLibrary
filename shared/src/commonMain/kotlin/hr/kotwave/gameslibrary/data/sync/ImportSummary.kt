package hr.kotwave.gameslibrary.data.sync

/** The outcome of a paste Import: how many Games were newly added vs already present (ownership attached). */
data class ImportSummary(val added: Int, val attached: Int) {
    val total: Int get() = added + attached
}
