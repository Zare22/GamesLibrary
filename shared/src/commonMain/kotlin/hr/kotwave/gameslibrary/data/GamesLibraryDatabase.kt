package hr.kotwave.gameslibrary.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [Game::class], version = 1)
@ConstructedBy(GamesLibraryDatabaseConstructor::class)
abstract class GamesLibraryDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}

// KSP generates the actual implementation per target.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object GamesLibraryDatabaseConstructor : RoomDatabaseConstructor<GamesLibraryDatabase> {
    override fun initialize(): GamesLibraryDatabase
}
