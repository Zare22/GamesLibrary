package hr.kotwave.gameslibrary.koin

import hr.kotwave.gameslibrary.game.GameRepository
import hr.kotwave.gameslibrary.game.SelectedGameViewModel
import hr.kotwave.gameslibrary.network.HttpClientProvider
import hr.kotwave.gameslibrary.steam.SteamService
import hr.kotwave.gameslibrary.steam.SteamServiceImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single { HttpClientProvider.create() }
    singleOf(::SteamServiceImpl).bind(SteamService::class)
    single { GameRepository(get(), get()) }
    viewModel { SelectedGameViewModel(get()) }
}

