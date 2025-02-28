package hr.kotwave.gameslibrary.database

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            GamesLibraryDatabase::class.java,
            "games_library"
        ).fallbackToDestructiveMigration().build()
    }

    single { get<GamesLibraryDatabase>().gameDao() }
}