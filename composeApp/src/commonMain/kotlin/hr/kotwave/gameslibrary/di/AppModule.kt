package hr.kotwave.gameslibrary.di

import hr.kotwave.gameslibrary.detail.DetailViewModel
import hr.kotwave.gameslibrary.epic.EpicViewModel
import hr.kotwave.gameslibrary.gog.GogViewModel
import hr.kotwave.gameslibrary.importer.ImportViewModel
import hr.kotwave.gameslibrary.importer.SharedTextInbox
import hr.kotwave.gameslibrary.library.LibraryViewModel
import hr.kotwave.gameslibrary.mirror.MirrorPairingViewModel
import hr.kotwave.gameslibrary.mirror.MirrorSessionViewModel
import hr.kotwave.gameslibrary.psn.PsnViewModel
import hr.kotwave.gameslibrary.settings.SettingsViewModel
import hr.kotwave.gameslibrary.steam.SteamViewModel
import hr.kotwave.gameslibrary.transfer.LibraryTransferViewModel
import hr.kotwave.gameslibrary.wishlist.WishlistViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule: Module = module {
    single { SharedTextInbox() }
    // Explicit calls, not viewModelOf: the injectable fetch-cert / clock / pinning lambdas keep their defaults.
    viewModel { MirrorPairingViewModel(get(), get()) }
    viewModel { MirrorSessionViewModel(get(), get()) }
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
