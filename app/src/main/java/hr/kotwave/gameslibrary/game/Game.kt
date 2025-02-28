package hr.kotwave.gameslibrary.game

import androidx.room.Entity
import androidx.room.PrimaryKey
import hr.kotwave.gameslibrary.platform.Platform

@Entity(tableName = "game")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val platform: Platform
)