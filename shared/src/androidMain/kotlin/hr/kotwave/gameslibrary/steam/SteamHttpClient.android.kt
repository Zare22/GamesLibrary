package hr.kotwave.gameslibrary.steam

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun steamEngine(): HttpClientEngine = OkHttp.create()
