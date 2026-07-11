package hr.kotwave.gameslibrary.psn

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class PsnAuthTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val tokenJson =
        """{"access_token":"acc","refresh_token":"ref","expires_in":3600,"token_type":"bearer","scope":"psn:mobile.v2.core psn:clientapp"}"""
    private val redirectLocation = "com.scee.psxandroid.scecompcall://redirect/?code=v3.ABC123&cid=xyz"
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.fromEpochSeconds(1_000L)
    }

    private fun authWith(engine: MockEngine) = PsnAuth(
        buildPsnHttpClient(
            engine,
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS,
            socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS,
            connectTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS,
            followRedirects = false,
        ),
        PsnConfig(),
        fixedClock,
    )

    private fun HttpRequestData.formBody(): String = when (val content = body) {
        is TextContent -> content.text
        is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
        else -> error("unexpected body type: $content")
    }

    private fun MockEngine.Companion.authFlow(
        location: String? = null,
        tokenStatus: HttpStatusCode = HttpStatusCode.OK,
        record: MutableList<HttpRequestData> = mutableListOf(),
    ) = MockEngine { request ->
        record += request
        if (request.url.encodedPath.endsWith("/authorize")) {
            respond(
                "",
                HttpStatusCode.Found,
                if (location != null) headersOf(HttpHeaders.Location, location) else headersOf(),
            )
        } else {
            respond(tokenJson, tokenStatus, jsonHeaders)
        }
    }

    @Test
    fun extractNpssoFromBareValue() {
        val auth = authWith(MockEngine { respond("", HttpStatusCode.OK) })
        val npsso = "a".repeat(64)

        assertEquals(npsso, auth.extractNpsso("  $npsso "))
    }

    @Test
    fun extractNpssoFromPageJson() {
        val auth = authWith(MockEngine { respond("", HttpStatusCode.OK) })
        val npsso = "Hb9".repeat(22)

        assertEquals(npsso, auth.extractNpsso("""{"npsso":"$npsso"}"""))
        assertEquals(npsso, auth.extractNpsso("""{ "npsso" : "$npsso" }"""))
    }

    @Test
    fun extractNpssoNullOnGarbage() {
        val auth = authWith(MockEngine { respond("", HttpStatusCode.OK) })

        assertNull(auth.extractNpsso(""))
        assertNull(auth.extractNpsso("short"))
        assertNull(auth.extractNpsso("""{"error":"invalid_grant"}"""))
        assertNull(auth.extractNpsso("https://ca.account.sony.com/api/v1/ssocookie"))
    }

    @Test
    fun exchangeNpssoCarriesCookieAndReturnsToken() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val auth = authWith(MockEngine.authFlow(location = redirectLocation, record = requests))

        val token = auth.exchangeNpsso("npsso_value")

        assertEquals("acc", token.accessToken)
        assertEquals("ref", token.refreshToken)
        assertEquals(1_000L + 3600L, token.expiresAt)

        val authorize = requests[0]
        assertEquals("npsso=npsso_value", authorize.headers[HttpHeaders.Cookie])
        val authorizeUrl = authorize.url.toString()
        assertTrue(authorizeUrl.contains("client_id=09515159-7237-4370-9b40-3806e67c0891"))
        assertTrue(authorizeUrl.contains("response_type=code"))
        assertTrue(authorizeUrl.contains("access_type=offline"))

        val tokenRequest = requests[1]
        assertTrue(tokenRequest.headers[HttpHeaders.Authorization]!!.startsWith("Basic "))
        val form = tokenRequest.formBody()
        assertTrue(form.contains("code=v3.ABC123"))
        assertTrue(form.contains("grant_type=authorization_code"))
        assertTrue(form.contains("token_format=jwt"))
    }

    @Test
    fun exchangeNpssoRejectedWhenLocationHasNoCode() = runTest {
        val auth = authWith(
            MockEngine.authFlow(location = "https://web.np.playstation.com/signin?error=login_required"),
        )

        try {
            auth.exchangeNpsso("dead_npsso")
            error("expected PsnNpssoRejectedException")
        } catch (expected: PsnNpssoRejectedException) {
        }
    }

    @Test
    fun exchangeNpssoRejectedWhenLocationMissing() = runTest {
        val auth = authWith(MockEngine.authFlow(location = null))

        try {
            auth.exchangeNpsso("dead_npsso")
            error("expected PsnNpssoRejectedException")
        } catch (expected: PsnNpssoRejectedException) {
        }
    }

    @Test
    fun refreshUsesRefreshGrant() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val auth = authWith(MockEngine.authFlow(record = requests))

        val token = auth.refresh("old_ref")

        assertEquals("acc", token.accessToken)
        val form = requests.single().formBody()
        assertTrue(form.contains("grant_type=refresh_token"))
        assertTrue(form.contains("refresh_token=old_ref"))
        assertTrue(form.contains("token_format=jwt"))
    }

    @Test
    fun tokenFailureThrows() = runTest {
        val auth = authWith(
            MockEngine.authFlow(location = redirectLocation, tokenStatus = HttpStatusCode.BadRequest),
        )

        try {
            auth.exchangeNpsso("npsso_value")
            error("expected PsnException")
        } catch (e: PsnException) {
            assertTrue(e.message!!.contains("400"))
        }
    }
}
