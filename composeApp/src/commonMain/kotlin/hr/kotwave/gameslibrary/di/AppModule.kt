package hr.kotwave.gameslibrary.di

import hr.kotwave.gameslibrary.detail.DetailViewModel
import hr.kotwave.gameslibrary.epic.EpicViewModel
import hr.kotwave.gameslibrary.gog.GogViewModel
import hr.kotwave.gameslibrary.importer.ImportViewModel
import hr.kotwave.gameslibrary.importer.SharedTextInbox
import hr.kotwave.gameslibrary.library.LibraryViewModel
import hr.kotwave.gameslibrary.psn.PsnViewModel
import hr.kotwave.gameslibrary.settings.SettingsViewModel
import hr.kotwave.gameslibrary.steam.SteamViewModel
import hr.kotwave.gameslibrary.transfer.LibraryTransferViewModel
import hr.kotwave.gameslibrary.wishlist.WishlistViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule: Module = module {
    single { SharedTextInbox() }
    viewModelOf(::LibraryViewModel)
    viewModelOf(::DetailViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::SteamViewModel)
    viewModelOf(::GogViewModel)
    viewModelOf(::PsnViewModel)
    viewModelOf(::EpicViewModel)
    viewModelOf(::ImportViewModel)
    viewModelOf(::WishlistViewModel)
    viewModelOf(::LibraryTransferViewModel)
}
