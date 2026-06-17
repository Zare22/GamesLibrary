package hr.kotwave.gameslibrary.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "game",
    indices = [Index(value = ["igdbId"], unique = true)],
)
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val igdbId: Long? = null,
    val wishlist: Boolean = false,
    val status: Status? = null,
)
