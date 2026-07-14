package hr.kotwave.gameslibrary.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters

@Database(entities = [Game::class, Ownership::class, ExternalGame::class, SyncDismissal::class], version = 6)
@ConstructedBy(GamesLibraryDatabaseConstructor::class)
@TypeConverters(Converters::class)
abstract class GamesLibraryDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}

// KSP generates the actual implementation per target.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object GamesLibraryDatabaseConstructor : RoomDatabaseConstructor<GamesLibraryDatabase> {
    override fun initialize(): GamesLibraryDatabase
}
