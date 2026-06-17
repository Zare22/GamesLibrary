package hr.kotwave.gameslibrary.data

import androidx.room.Embedded
import androidx.room.Relation

/** A Game with its Ownerships, for Library tiles (store badges + status dot). */
data class GameWithOwnerships(
    @Embedded val game: Game,
    @Relation(parentColumn = "id", entityColumn = "gameId")
    val ownerships: List<Ownership>,
)
