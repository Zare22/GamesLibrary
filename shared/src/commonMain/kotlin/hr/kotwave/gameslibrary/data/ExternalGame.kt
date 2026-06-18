package hr.kotwave.gameslibrary.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An IGDB external-game reference: the Game's id on a specific store/service. The `(category, uid)`
 * index is the cross-store lookup used by paste Import to match a Store line to an existing Game.
 */
@Entity(
    tableName = "external_game",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["gameId"]),
        Index(value = ["category", "uid"]),
    ],
)
data class ExternalGame(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val category: Int,
    val uid: String,
    val url: String? = null,
)
