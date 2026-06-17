package hr.kotwave.gameslibrary.data

/** The user's play relationship to an owned Game. Null when the Game is Wishlisted. */
enum class Status {
    BACKLOG,
    PLAYING,
    COMPLETED,
    DROPPED,
}
