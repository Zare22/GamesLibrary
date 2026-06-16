package hr.kotwave.gameslibrary.ui.model

/** Play status for the status pip + filter chips. */
enum class Status(val label: String) {
    BACKLOG("Backlog"),
    PLAYING("Playing"),
    COMPLETED("Completed"),
    DROPPED("Dropped"),
}
