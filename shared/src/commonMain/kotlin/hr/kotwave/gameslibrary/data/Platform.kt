package hr.kotwave.gameslibrary.data

import kotlinx.serialization.Serializable

/** A hardware/OS a Game runs on (PC, PS5, Switch). IGDB-sourced; stored as JSON on the Game. */
@Serializable
data class Platform(val name: String, val abbreviation: String? = null)
