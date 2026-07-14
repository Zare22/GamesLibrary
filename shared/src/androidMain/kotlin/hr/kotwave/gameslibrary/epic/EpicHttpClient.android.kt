package hr.kotwave.gameslibrary.epic

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun epicEngine(): HttpClientEngine = OkHttp.create()
