package hr.kotwave.gameslibrary.di

import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.GamesLibraryDatabase
import hr.kotwave.gameslibrary.data.LocalDataReset
import hr.kotwave.gameslibrary.gog.gogModule
import hr.kotwave.gameslibrary.igdb.igdbModule
import hr.kotwave.gameslibrary.psn.psnModule
import hr.kotwave.gameslibrary.steam.steamModule
import org.koin.core.module.Module
import org.koin.dsl.module

/** Supplies the platform-specific [GamesLibraryDatabase] (Android needs a Context; JVM needs a file path). */
expect val platformModule: Module

val dataModule = module {
    single { get<GamesLibraryDatabase>().gameDao() }
    single { GameRepository(get()) }
    single { LocalDataReset(get(), get()) }
}

val sharedModules: List<Module> = listOf(platformModule, dataModule, igdbModule, steamModule, gogModule, psnModule)
