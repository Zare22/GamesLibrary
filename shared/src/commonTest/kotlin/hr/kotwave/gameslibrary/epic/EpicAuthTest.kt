package hr.kotwave.gameslibrary.epic

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

class EpicAuthTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.fromEpochSeconds(1_000L)
    }

    private fun tokenJson(accessToken: String = "acc", refreshToken: String = "ref") =
        """{"access_token":"$accessToken","refresh_token":"$refreshToken","expires_in":129600,""" +
            """"refresh_expires":31540000,"token_type":"bearer","account_id":"abc","displayName":"leo"}"""

    private fun authWith(engine: MockEngine) = EpicAuth(
        buildEpicHttpClient(
            engine,
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS,
            socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS,
            connectTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS,
        ),
        EpicConfig(),
        fixedClock,
    )

    private fun HttpRequestData.formBody(): String = when (val content = body) {
        is TextContent -> content.text
        is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
        else -> error("unexpected body type: $content")
    }

    private val code = "0123456789abcdef0123456789abcdef"

    @Test
    fun extractCodeFromPageJson() {
        val auth = authWith(MockEngine { respond("", HttpStatusCode.OK) })

        assertEquals(
            code,
            auth.extractAuthorizationCode("""{"redirectUrl":"…","authorizationCode":"$code","exchangeCode":null}"""),
        )
        assertEquals(code, auth.extractAuthorizationCode("""{ "authorizationCode" : "$code" }"""))
    }

    @Test
    fun extractCodeFromBareValue() {
        val auth = authWith(MockEngine { respond("", HttpStatusCode.OK) })

        assertEquals(code, auth.extractAuthorizationCode("  $code "))
    }

    @Test
    fun extractCodeNullOnGarbage() {
        val auth = authWith(MockEngine { respond("", HttpStatusCode.OK) })

        assertNull(auth.extractAuthorizationCode(""))
        assertNull(auth.extractAuthorizationCode("short"))
        assertNull(auth.extractAuthorizationCode("""{"authorizationCode":null,"error":"login_required"}"""))
        // 64 hex chars are a different secret, not a code — no 32-hex word inside.
        assertNull(auth.extractAuthorizationCode("a".repeat(64)))
    }

    @Test
    fun exchangeCarriesBasicAuthAndReturnsToken() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val auth = authWith(
            MockEngine { request ->
                requests += request
                respond(tokenJson(), HttpStatusCode.OK, jsonHeaders)
            },
        )

        val token = auth.exchangeCode(code)

        assertEquals("acc", token.accessToken)
        assertEquals("ref", token.refreshToken)
        assertEquals(1_000L + 129_600L, token.expiresAt)

        val request = requests.single()
        assertTrue(request.headers[HttpHeaders.Authorization]!!.startsWith("Basic MzRhMDJjZjhmNDQxNGUyOWIx"))
        val form = request.formBody()
        assertTrue(form.contains("grant_type=authorization_code"))
        assertTrue(form.contains("code=$code"))
        assertTrue(form.contains("token_type=eg1"))
    }

    @Test
    fun refreshUsesRefreshGrantAndReturnsRotatedToken() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val auth = authWith(
            MockEngine { request ->
                requests += request
                respond(tokenJson(accessToken = "acc2", refreshToken = "rotated_ref"), HttpStatusCode.OK, jsonHeaders)
            },
        )

        val token = auth.refresh("old_ref")

        // The rotation contract: the result carries the response's NEW refresh token, never the old one.
        assertEquals("rotated_ref", token.refreshToken)
        assertEquals("acc2", token.accessToken)
        val form = requests.single().formBody()
        assertTrue(form.contains("grant_type=refresh_token"))
        assertTrue(form.contains("refresh_token=old_ref"))
        assertTrue(form.contains("token_type=eg1"))
    }

    @Test
    fun exchangeRejectedOnBadRequest() = runTest {
        val auth = authWith(
            MockEngine {
                respond(
                    """{"errorCode":"errors.com.epicgames.account.oauth.authorization_code_not_found"}""",
                    HttpStatusCode.BadRequest,
                    jsonHeaders,
                )
            },
        )

        try {
            auth.exchangeCode(code)
            error("expected EpicCodeRejectedException")
        } catch (expected: EpicCodeRejectedException) {
        }
    }

    @Test
    fun refreshFailureThrowsPlainEpicException() = runTest {
        val auth = authWith(
            MockEngine {
                respond("""{"errorCode":"errors.com.epicgames.account.auth_token.invalid_refresh_token"}""", HttpStatusCode.BadRequest, jsonHeaders)
            },
        )

        try {
            auth.refresh("dead_ref")
            error("expected EpicException")
        } catch (e: EpicException) {
            assertTrue(e !is EpicCodeRejectedException)
            assertTrue(e.message!!.contains("400"))
        }
    }

    @Test
    fun tokenFailureThrows() = runTest {
        val auth = authWith(MockEngine { respond("down", HttpStatusCode.InternalServerError) })

        try {
            auth.exchangeCode(code)
            error("expected EpicException")
        } catch (e: EpicException) {
            assertTrue(e.message!!.contains("500"))
        }
    }
}
