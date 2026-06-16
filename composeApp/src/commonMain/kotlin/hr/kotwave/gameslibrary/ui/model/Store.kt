package hr.kotwave.gameslibrary.ui.model

/** Storefront for the store badge + accent colors. */
enum class Store(val glyph: String, val label: String) {
    STEAM("S", "Steam"),
    GOG("G", "GOG"),
    PSN("P", "PlayStation"),
    XBOX("X", "Xbox"),
    EPIC("E", "Epic"),
    NINTENDO("N", "Nintendo"),
    ITCH("i", "itch.io"),
}
