package hr.kotwave.gameslibrary.epic

import org.koin.core.module.Module
import org.koin.dsl.module

val epicModule: Module = module {
    single { EpicConfig() }
    // Each HttpClient is built inline (not a bare single) so it never collides with the other stores' HttpClients.
    single { EpicClient(buildEpicHttpClient(epicEngine()), get()) }
    single { EpicAuth(buildEpicHttpClient(epicEngine()), get()) }
}
