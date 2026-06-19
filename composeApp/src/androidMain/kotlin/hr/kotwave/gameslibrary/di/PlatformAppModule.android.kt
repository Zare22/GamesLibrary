package hr.kotwave.gameslibrary.di

import hr.kotwave.gameslibrary.steam.AndroidSteamAuthFlow
import hr.kotwave.gameslibrary.steam.SteamAuthFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformAppModule: Module = module {
    single<SteamAuthFlow> { AndroidSteamAuthFlow(androidContext().applicationContext) }
}
