package hr.kotwave.gameslibrary

import android.app.Application
import hr.kotwave.gameslibrary.di.appModule
import hr.kotwave.gameslibrary.di.sharedModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GamesLibraryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GamesLibraryApplication)
            modules(sharedModules + appModule)
        }
    }
}
