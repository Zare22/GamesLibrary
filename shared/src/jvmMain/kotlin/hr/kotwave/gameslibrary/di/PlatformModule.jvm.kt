package hr.kotwave.gameslibrary.di

import androidx.room.Room
import hr.kotwave.gameslibrary.data.DATABASE_FILE_NAME
import hr.kotwave.gameslibrary.data.GamesLibraryDatabase
import hr.kotwave.gameslibrary.data.buildGamesLibraryDatabase
import hr.kotwave.gameslibrary.secure.FileSecureStorage
import hr.kotwave.gameslibrary.secure.KeyringSecureStorage
import hr.kotwave.gameslibrary.secure.SecureStorage
import hr.kotwave.gameslibrary.secure.createKeyringBackend
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual val platformModule: Module = module {
    single<GamesLibraryDatabase> {
        val dbFile = File(appDataDirectory(), DATABASE_FILE_NAME)
        Room.databaseBuilder<GamesLibraryDatabase>(name = dbFile.absolutePath)
            .buildGamesLibraryDatabase()
    }
    single<SecureStorage> {
        val dir = appDataDirectory()
        KeyringSecureStorage(
            service = "GamesLibrary",
            backend = createKeyringBackend(),
            fallback = FileSecureStorage(dir),
            legacyFile = File(dir, FileSecureStorage.FILE_NAME),
        )
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
