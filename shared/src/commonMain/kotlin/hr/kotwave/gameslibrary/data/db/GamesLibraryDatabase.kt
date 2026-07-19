package hr.kotwave.gameslibrary.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import hr.kotwave.gameslibrary.data.ExternalGame
import hr.kotwave.gameslibrary.data.Game
import hr.kotwave.gameslibrary.data.MirrorBaseline
import hr.kotwave.gameslibrary.data.Ownership
import hr.kotwave.gameslibrary.data.SyncDismissal

@Database(
    entities = [Game::class, Ownership::class, ExternalGame::class, SyncDismissal::class, MirrorBaseline::class],
    version = 7,
)
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
