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
    val userRating: Double? = null,
    val slug: String? = null,
    val firstReleaseDate: Long? = null,
    val coverImageId: String? = null,
    val developer: String? = null,
    val totalRating: Double? = null,
    val totalRatingCount: Int? = null,
    val platforms: List<Platform> = emptyList(),
    val alternativeNames: List<String> = emptyList(),
    val orphaned: Boolean = false,
    val addedAt: Long? = null,
)
