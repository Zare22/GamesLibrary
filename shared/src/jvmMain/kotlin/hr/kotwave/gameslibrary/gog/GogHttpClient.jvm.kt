package hr.kotwave.gameslibrary.gog

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun gogEngine(): HttpClientEngine = OkHttp.create()
