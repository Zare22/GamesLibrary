package hr.kotwave.gameslibrary.steam

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SteamBounceTest {

    private fun bounceWith(engine: MockEngine) = SteamBounce(buildSteamHttpClient(engine))

    /** The pre-flight timer must run on real time, not `runTest`'s virtual clock, to race MockEngine fairly. */
    private fun realTimeTest(block: suspend () -> Unit) = runTest { withContext(Dispatchers.Default) { block() } }

    @Test
    fun returnToViaBounceCarriesPortAsQueryParam() {
        val bounce = bounceWith(MockEngine { respond("", HttpStatusCode.OK) })

        assertEquals("https://gameslibrary.kotwave.hr/callback?port=54321", bounce.returnTo(54321, viaBounce = true))
    }

    @Test
    fun returnToWithoutBounceIsTheLoopbackCallback() {
        val bounce = bounceWith(MockEngine { respond("", HttpStatusCode.OK) })

        assertEquals("http://127.0.0.1:54321/callback", bounce.returnTo(54321, viaBounce = false))
    }

    @Test
    fun reachableWhenThePageAnswersOk() = realTimeTest {
        var requested: String? = null
        val bounce = bounceWith(
            MockEngine { request ->
                requested = request.url.toString()
                respond("", HttpStatusCode.OK)
            },
        )

        assertTrue(bounce.reachable())
        assertEquals("https://gameslibrary.kotwave.hr/callback", requested)
    }

    @Test
    fun notReachableWhenThePageAnswersNotFound() = realTimeTest {
        val bounce = bounceWith(MockEngine { respond("", HttpStatusCode.NotFound) })

        assertFalse(bounce.reachable())
    }

    @Test
    fun notReachableWhenTheRequestThrows() = realTimeTest {
        val bounce = bounceWith(MockEngine { error("connection refused") })

        assertFalse(bounce.reachable())
    }

    @Test
    fun notReachableWhenTheAnswerOutlivesTheTimeout() = realTimeTest {
        val engine = MockEngine {
            delay(1.seconds)
            respond("", HttpStatusCode.OK)
        }
        val bounce = SteamBounce(buildSteamHttpClient(engine), timeout = 100.milliseconds)

        assertFalse(bounce.reachable())
    }
}
