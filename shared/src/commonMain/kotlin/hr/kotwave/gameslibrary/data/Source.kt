package hr.kotwave.gameslibrary.data

/** How an Ownership came to be recorded (the `added_via` of an Ownership). */
enum class Source {
    MANUAL,
    STEAM_SYNC,
    PASTE_IMPORT,
}
