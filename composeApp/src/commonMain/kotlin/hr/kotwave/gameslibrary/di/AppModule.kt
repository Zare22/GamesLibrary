package hr.kotwave.gameslibrary.di

import hr.kotwave.gameslibrary.detail.DetailViewModel
import hr.kotwave.gameslibrary.library.LibraryViewModel
import hr.kotwave.gameslibrary.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule: Module = module {
    viewModelOf(::LibraryViewModel)
    viewModelOf(::DetailViewModel)
    viewModelOf(::SettingsViewModel)
}
