package hr.kotwave.gameslibrary.di

import hr.kotwave.gameslibrary.detail.DetailViewModel
import hr.kotwave.gameslibrary.importer.ImportViewModel
import hr.kotwave.gameslibrary.library.LibraryViewModel
import hr.kotwave.gameslibrary.settings.SettingsViewModel
import hr.kotwave.gameslibrary.steam.SteamViewModel
import hr.kotwave.gameslibrary.transfer.LibraryTransferViewModel
import hr.kotwave.gameslibrary.wishlist.WishlistViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule: Module = module {
    viewModelOf(::LibraryViewModel)
    viewModelOf(::DetailViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::SteamViewModel)
    viewModelOf(::ImportViewModel)
    viewModelOf(::WishlistViewModel)
    viewModelOf(::LibraryTransferViewModel)
}
