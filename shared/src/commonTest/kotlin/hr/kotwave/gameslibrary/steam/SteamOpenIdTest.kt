package hr.kotwave.gameslibrary.steam

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SteamOpenIdTest {

    private val textHeaders = headersOf(HttpHeaders.ContentType, "text/plain")

    private fun openIdWith(engine: MockEngine) = SteamOpenId(buildSteamHttpClient(engine))

    /** The `openid.*` params Steam appends to a successful redirect. */
    private fun callbackParams(
        mode: String = "id_res",
        claimedId: String = "https://steamcommunity.com/openid/id/76561198000000001",
    ) = mapOf(
        "openid.ns" to "http://specs.openid.net/auth/2.0",
        "openid.mode" to mode,
        "openid.op_endpoint" to "https://steamcommunity.com/openid/login",
        "openid.claimed_id" to claimedId,
        "openid.identity" to claimedId,
        "openid.return_to" to "http://127.0.0.1:54321/callback",
        "openid.response_nonce" to "2026-06-19T00:00:00Zabc",
        "openid.assoc_handle" to "1234567890",
        "openid.signed" to "signed,op_endpoint,claimed_id,identity,return_to,response_nonce,assoc_handle",
        "openid.sig" to "deadbeefsignature==",
    )

    @Test
    fun authUrlCarriesCheckidSetupAndReturnTo() {
        val url = openIdWith(MockEngine { respond("", HttpStatusCode.OK) })
            .authUrl("http://127.0.0.1:54321/callback")

        assertTrue(url.startsWith("https://steamcommunity.com/openid/login"))
        val params = Url(url).parameters
        assertEquals("checkid_setup", params["openid.mode"])
        assertEquals("http://specs.openid.net/auth/2.0", params["openid.ns"])
        assertEquals("http://127.0.0.1:54321/callback", params["openid.return_to"])
        assertEquals("http://127.0.0.1:54321", params["openid.realm"])
        assertTrue(params["openid.identity"]!!.endsWith("identifier_select"))
    }

    @Test
    fun verifyReturnsSteamIdWhenSteamConfirms() = runTest {
        val openId = openIdWith(
            MockEngine { respond("ns:http://specs.openid.net/auth/2.0\nis_valid:true\n", HttpStatusCode.OK, textHeaders) },
        )

        assertEquals("76561198000000001", openId.verify(callbackParams()))
    }

    @Test
    fun verifyPostsCheckAuthenticationEchoingSignedFields() = runTest {
        var body: String? = null
        val openId = openIdWith(
            MockEngine { request ->
                body = (request.body as FormDataContent).bytes().decodeToString()
                respond("ns:http://specs.openid.net/auth/2.0\nis_valid:true\n", HttpStatusCode.OK, textHeaders)
            },
        )

        openId.verify(callbackParams())

        val sent = body!!
        assertTrue(sent.contains("openid.mode=check_authentication"))
        assertTrue("openid.mode=id_res" !in sent)
        assertTrue(sent.contains("openid.sig=deadbeefsignature"))
        assertTrue(sent.contains("openid.signed="))
    }

    @Test
    fun verifyReturnsNullWhenSteamRejects() = runTest {
        val openId = openIdWith(
            MockEngine { respond("ns:http://specs.openid.net/auth/2.0\nis_valid:false\n", HttpStatusCode.OK, textHeaders) },
        )

        assertNull(openId.verify(callbackParams()))
    }

    @Test
    fun verifyRejectsNonPositiveAssertionWithoutCallingSteam() = runTest {
        var called = false
        val openId = openIdWith(MockEngine { called = true; respond("", HttpStatusCode.OK) })

        assertNull(openId.verify(callbackParams(mode = "cancel")))
        assertTrue(!called)
    }

    @Test
    fun verifyRejectsMalformedClaimedId() = runTest {
        var called = false
        val openId = openIdWith(MockEngine { called = true; respond("", HttpStatusCode.OK) })

        assertNull(openId.verify(callbackParams(claimedId = "https://evil.example/openid/id/123")))
        assertTrue(!called)
    }
}
