package hr.kotwave.gameslibrary.di

import androidx.room.Room
import hr.kotwave.gameslibrary.data.DATABASE_FILE_NAME
import hr.kotwave.gameslibrary.data.GamesLibraryDatabase
import hr.kotwave.gameslibrary.data.buildGamesLibraryDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual val platformModule: Module = module {
    single<GamesLibraryDatabase> {
        val dbFile = File(appDataDirectory(), DATABASE_FILE_NAME)
        Room.databaseBuilder<GamesLibraryDatabase>(name = dbFile.absolutePath)
            .buildGamesLibraryDatabase()
    }
}

/** Per-OS application data directory; created if absent. */
private fun appDataDirectory(): File {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")
    val base = when {
        os.contains("win") -> System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming"
        os.contains("mac") -> "$userHome/Library/Application Support"
        else -> System.getenv("XDG_DATA_HOME") ?: "$userHome/.local/share"
    }
    return File(base, "GamesLibrary").apply { mkdirs() }
}
