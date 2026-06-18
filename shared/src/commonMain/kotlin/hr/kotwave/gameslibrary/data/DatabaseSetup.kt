package hr.kotwave.gameslibrary.data

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

internal const val DATABASE_FILE_NAME = "games_library.db"

/** Finishes a platform-provided [RoomDatabase.Builder] with the bundled SQLite driver. */
internal fun RoomDatabase.Builder<GamesLibraryDatabase>.buildGamesLibraryDatabase(): GamesLibraryDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .build()
