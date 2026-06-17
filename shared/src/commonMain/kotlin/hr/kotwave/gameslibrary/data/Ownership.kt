package hr.kotwave.gameslibrary.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ownership",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["gameId", "store"], unique = true),
        Index(value = ["gameId"]),
    ],
)
data class Ownership(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val store: Store,
    val source: Source = Source.MANUAL,
)
