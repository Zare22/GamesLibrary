package hr.kotwave.gameslibrary.steam

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SteamClientTest {

    private val config = SteamConfig(apiKey = "key")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun clientWith(engine: MockEngine) = SteamClient(buildSteamHttpClient(engine), config)

    @Test
    fun parsesOwnedGames() = runTest {
        val json = """{"response":{"game_count":2,"games":[
            {"appid":292030,"name":"The Witcher 3","playtime_forever":1200},
            {"appid":620,"name":"Portal 2"}
        ]}}"""
        val client = clientWith(MockEngine { respond(json, HttpStatusCode.OK, jsonHeaders) })

        val games = client.getOwnedGames("76561198000000000")

        assertEquals(
            listOf(SteamOwnedGame(292030, "The Witcher 3"), SteamOwnedGame(620, "Portal 2")),
            games,
        )
    }

    @Test
    fun privateProfileYieldsEmptyList() = runTest {
        val client = clientWith(MockEngine { respond("""{"response":{}}""", HttpStatusCode.OK, jsonHeaders) })

        assertTrue(client.getOwnedGames("76561198000000000").isEmpty())
    }

    @Test
    fun gamesWithoutNamesAreSkipped() = runTest {
        val json = """{"response":{"game_count":2,"games":[
            {"appid":1,"name":""},
            {"appid":2,"name":"Real Game"}
        ]}}"""
        val client = clientWith(MockEngine { respond(json, HttpStatusCode.OK, jsonHeaders) })

        assertEquals(listOf(SteamOwnedGame(2, "Real Game")), client.getOwnedGames("76561198000000000"))
    }

    @Test
    fun requestCarriesKeyAndSteamId() = runTest {
        var url: String? = null
        val client = clientWith(
            MockEngine { request ->
                url = request.url.toString()
                respond("""{"response":{}}""", HttpStatusCode.OK, jsonHeaders)
            },
        )

        client.getOwnedGames("76561198000000000")

        val sent = url!!
        assertTrue(sent.contains("key=key"))
        assertTrue(sent.contains("steamid=76561198000000000"))
        assertTrue(sent.contains("include_appinfo=1"))
    }

    @Test
    fun failureThrows() = runTest {
        val client = clientWith(MockEngine { respond("nope", HttpStatusCode.Forbidden, jsonHeaders) })

        try {
            client.getOwnedGames("76561198000000000")
            error("expected SteamException")
        } catch (e: SteamException) {
            assertTrue(e.message!!.contains("403"))
        }
    }
}
