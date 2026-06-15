package hr.kotwave.gameslibrary.di

import androidx.room.Room
import hr.kotwave.gameslibrary.data.DATABASE_FILE_NAME
import hr.kotwave.gameslibrary.data.GamesLibraryDatabase
import hr.kotwave.gameslibrary.data.buildGamesLibraryDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<GamesLibraryDatabase> {
        val context = androidContext().applicationContext
        val dbFile = context.getDatabasePath(DATABASE_FILE_NAME)
        Room.databaseBuilder<GamesLibraryDatabase>(
            context = context,
            name = dbFile.absolutePath,
        ).buildGamesLibraryDatabase()
    }
}
