package hr.kotwave.gameslibrary.app

import android.app.Application
import hr.kotwave.gameslibrary.database.databaseModule
import hr.kotwave.gameslibrary.koin.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GamesLibraryApp : Application(){
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@GamesLibraryApp)
            modules(
                appModule,
                databaseModule
            )
        }
    }
}