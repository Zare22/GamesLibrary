package hr.kotwave.gameslibrary.igdb

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun igdbEngine(): HttpClientEngine = OkHttp.create()
