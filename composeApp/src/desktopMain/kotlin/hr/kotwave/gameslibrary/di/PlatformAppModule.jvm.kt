package hr.kotwave.gameslibrary.di

import hr.kotwave.gameslibrary.steam.DesktopSteamAuthFlow
import hr.kotwave.gameslibrary.steam.SteamAuthFlow
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformAppModule: Module = module {
    single<SteamAuthFlow> { DesktopSteamAuthFlow() }
}
