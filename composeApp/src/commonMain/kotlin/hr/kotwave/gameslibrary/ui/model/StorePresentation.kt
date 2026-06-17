package hr.kotwave.gameslibrary.ui.model

import hr.kotwave.gameslibrary.data.Store

/** Single-letter badge glyph for a [Store]. */
val Store.glyph: String
    get() = when (this) {
        Store.STEAM -> "S"
        Store.GOG -> "G"
        Store.PSN -> "P"
        Store.XBOX -> "X"
        Store.EPIC -> "E"
        Store.NINTENDO -> "N"
        Store.ITCH -> "i"
    }

/** Display name for a [Store]. */
val Store.label: String
    get() = when (this) {
        Store.STEAM -> "Steam"
        Store.GOG -> "GOG"
        Store.PSN -> "PlayStation"
        Store.XBOX -> "Xbox"
        Store.EPIC -> "Epic"
        Store.NINTENDO -> "Nintendo"
        Store.ITCH -> "itch.io"
    }
