package hr.kotwave.gameslibrary.gog

import org.koin.core.module.Module
import org.koin.dsl.module

val gogModule: Module = module {
    single { GogConfig() }
    // Each HttpClient is built inline (not a bare single) so it never collides with IGDB's/Steam's HttpClient.
    single { GogClient(buildGogHttpClient(gogEngine()), get()) }
    single { GogAuth(buildGogHttpClient(gogEngine()), get()) }
}
