package hr.kotwave.gameslibrary.psn

import org.koin.core.module.Module
import org.koin.dsl.module

val psnModule: Module = module {
    single { PsnConfig() }
    // Each HttpClient is built inline (not a bare single) so it never collides with the other stores' HttpClients.
    single { PsnClient(buildPsnHttpClient(psnEngine()), get()) }
    // The auth client keeps redirects unfollowed: the authorize `code` is read off the 302 Location.
    single { PsnAuth(buildPsnHttpClient(psnEngine(), followRedirects = false), get()) }
}
