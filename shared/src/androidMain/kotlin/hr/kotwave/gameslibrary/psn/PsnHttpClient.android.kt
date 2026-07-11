package hr.kotwave.gameslibrary.psn

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun psnEngine(): HttpClientEngine = OkHttp.create()
