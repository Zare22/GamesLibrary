package hr.kotwave.gameslibrary.gog

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class GogAuthTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val tokenJson =
        """{"access_token":"acc","refresh_token":"ref","expires_in":3600,"user_id":"42","token_type":"bearer"}"""
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.fromEpochSeconds(1_000L)
    }

    private fun authWith(engine: MockEngine) = GogAuth(buildGogHttpClient(engine), GogConfig(), fixedClock)

    @Test
    fun authUrlCarriesClientAndRedirect() {
        val url = authWith(MockEngine { respond("", HttpStatusCode.OK) }).authUrl()

        assertTrue(url.startsWith("https://auth.gog.com/auth"))
        assertTrue(url.contains("client_id=46899977096215655"))
        assertTrue(url.contains("response_type=code"))
        assertFalse(url.contains("layout=client2"))
        assertTrue(url.contains("redirect_uri="))
    }

    @Test
    fun exchangeCodeParsesTokenAndComputesAbsoluteExpiry() = runTest {
        var url: String? = null
        val auth = authWith(
            MockEngine { request ->
                url = request.url.toString()
                respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
            },
        )

        val token = auth.exchangeCode("the_code")

        assertEquals("acc", token.accessToken)
        assertEquals("ref", token.refreshToken)
        assertEquals(1_000L + 3600L, token.expiresAt)
        val sent = url!!
        assertTrue(sent.contains("grant_type=authorization_code"))
        assertTrue(sent.contains("code=the_code"))
        assertTrue(sent.contains("client_id=46899977096215655"))
        assertTrue(sent.contains("client_secret="))
        assertTrue(sent.contains("redirect_uri="))
    }

    @Test
    fun refreshUsesRefreshGrant() = runTest {
        var url: String? = null
        val auth = authWith(
            MockEngine { request ->
                url = request.url.toString()
                respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
            },
        )

        val token = auth.refresh("old_ref")

        assertEquals("acc", token.accessToken)
        val sent = url!!
        assertTrue(sent.contains("grant_type=refresh_token"))
        assertTrue(sent.contains("refresh_token=old_ref"))
    }

    @Test
    fun tokenFailureThrows() = runTest {
        val auth = authWith(MockEngine { respond("nope", HttpStatusCode.BadRequest) })

        try {
            auth.exchangeCode("x")
            error("expected GogException")
        } catch (e: GogException) {
            assertTrue(e.message!!.contains("400"))
        }
    }

    @Test
    fun extractCodeFromFullRedirectUrl() {
        val auth = authWith(MockEngine { respond("", HttpStatusCode.OK) })

        assertEquals(
            "ABC123",
            auth.extractCode("https://embed.gog.com/on_login_success?origin=client&code=ABC123"),
        )
    }

    @Test
    fun extractCodeFromBareCode() {
        val auth = authWith(MockEngine { respond("", HttpStatusCode.OK) })

        assertEquals("ABC123", auth.extractCode("  ABC123 "))
    }

    @Test
    fun extractCodeNullWhenNoCodePresent() {
        val auth = authWith(MockEngine { respond("", HttpStatusCode.OK) })

        assertNull(auth.extractCode("https://embed.gog.com/on_login_success?origin=client&error=denied"))
        assertNull(auth.extractCode(""))
    }
}
