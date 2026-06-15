package hr.kotwave.gameslibrary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)
