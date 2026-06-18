package hr.kotwave.gameslibrary.igdb

import org.koin.core.module.Module
import org.koin.dsl.module

val igdbModule: Module = module {
    single { IgdbConfig(clientId = IgdbCredentials.CLIENT_ID, clientSecret = IgdbCredentials.CLIENT_SECRET) }
    single { buildIgdbHttpClient(igdbEngine()) }
    single { TwitchTokenProvider(get(), get()) }
    single { IgdbClient(get(), get(), get()) }
}
