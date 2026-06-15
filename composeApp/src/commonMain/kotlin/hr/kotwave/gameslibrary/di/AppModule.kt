package hr.kotwave.gameslibrary.di

import hr.kotwave.gameslibrary.library.GameListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule: Module = module {
    viewModelOf(::GameListViewModel)
}
