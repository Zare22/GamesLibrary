package hr.kotwave.gameslibrary.igdb

import hr.kotwave.gameslibrary.data.ExternalRef
import hr.kotwave.gameslibrary.data.Platform
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IgdbClientTest {

    private val config = IgdbConfig(clientId = "cid", clientSecret = "secret")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val tokenJson = """{"access_token":"tok_abc","expires_in":5000000,"token_type":"bearer"}"""

    private fun clientWith(engine: MockEngine): IgdbClient {
        val http = buildIgdbHttpClient(engine)
        return IgdbClient(http, TwitchTokenProvider(http, config), config)
    }

    private fun isTwitch(host: String) = host == "id.twitch.tv"

    @Test
    fun searchReturnsLightweightResults() = runTest {
        val games = """[{
            "id":1942,"name":"The Witcher 3","cover":{"image_id":"co1wyy"},"first_release_date":1431993600,
            "involved_companies":[{"developer":true,"company":{"name":"CD Projekt RED"}}]
        }]"""
        val client = clientWith(
            MockEngine { request ->
                if (isTwitch(request.url.host)) respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                else respond(games, HttpStatusCode.OK, jsonHeaders)
            },
        )

        val result = client.searchGames("witcher").single()
        assertEquals(1942L, result.igdbId)
        assertEquals("The Witcher 3", result.name)
        assertEquals("co1wyy", result.coverImageId)
        assertEquals(1431993600L, result.firstReleaseDate)
        assertEquals("CD Projekt RED", result.developer)
    }

    @Test
    fun blankSearchSkipsTheNetwork() = runTest {
        var calls = 0
        val client = clientWith(MockEngine { calls++; respond("[]", HttpStatusCode.OK, jsonHeaders) })

        assertTrue(client.searchGames("   ").isEmpty())
        assertEquals(0, calls)
    }

    @Test
    fun fetchGameMapsFullMetadata() = runTest {
        val game = """[{
            "id":1942,"name":"The Witcher 3","slug":"the-witcher-3-wild-hunt",
            "first_release_date":1431993600,"cover":{"image_id":"co1wyy"},
            "total_rating":93.5,"total_rating_count":4000,
            "involved_companies":[
                {"developer":true,"company":{"name":"CD Projekt RED"}},
                {"developer":false,"company":{"name":"Bandai Namco"}}
            ],
            "platforms":[
                {"name":"PC (Microsoft Windows)","abbreviation":"PC"},
                {"name":"PlayStation 4","abbreviation":"PS4"}
            ],
            "external_games":[{"uid":"292030","external_game_source":1,"url":"https://store.steampowered.com/app/292030"}],
            "alternative_names":[{"name":"Wiedzmin 3"}]
        }]"""
        val client = clientWith(
            MockEngine { request ->
                if (isTwitch(request.url.host)) respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                else respond(game, HttpStatusCode.OK, jsonHeaders)
            },
        )

        val igdb = client.fetchGame(1942L)!!
        assertEquals("the-witcher-3-wild-hunt", igdb.slug)
        assertEquals("co1wyy", igdb.coverImageId)
        assertEquals("CD Projekt RED", igdb.developer)
        assertEquals(93.5, igdb.totalRating)
        assertEquals(4000, igdb.totalRatingCount)
        assertEquals(
            listOf(Platform("PC (Microsoft Windows)", "PC"), Platform("PlayStation 4", "PS4")),
            igdb.platforms,
        )
        assertEquals(
            listOf(ExternalRef(1, "292030", "https://store.steampowered.com/app/292030")),
            igdb.externalGames,
        )
        assertEquals(listOf("Wiedzmin 3"), igdb.alternativeNames)
    }

    @Test
    fun fetchGameReturnsNullWhenNoMatch() = runTest {
        val client = clientWith(
            MockEngine { request ->
                if (isTwitch(request.url.host)) respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                else respond("[]", HttpStatusCode.OK, jsonHeaders)
            },
        )
        assertNull(client.fetchGame(999L))
    }

    @Test
    fun tokenIsFetchedOnceAndReused() = runTest {
        var authCalls = 0
        val client = clientWith(
            MockEngine { request ->
                if (isTwitch(request.url.host)) {
                    authCalls++
                    respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                } else {
                    respond("[]", HttpStatusCode.OK, jsonHeaders)
                }
            },
        )

        client.searchGames("a")
        client.searchGames("b")

        assertEquals(1, authCalls)
    }

    @Test
    fun unauthorizedTriggersTokenRefreshAndRetry() = runTest {
        var authCalls = 0
        var gameCalls = 0
        val client = clientWith(
            MockEngine { request ->
                if (isTwitch(request.url.host)) {
                    authCalls++
                    respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                } else {
                    gameCalls++
                    if (gameCalls == 1) respond("", HttpStatusCode.Unauthorized, jsonHeaders)
                    else respond("[]", HttpStatusCode.OK, jsonHeaders)
                }
            },
        )

        client.searchGames("a")

        assertEquals(2, authCalls)
        assertEquals(2, gameCalls)
    }

    @Test
    fun matchBySteamAppidsMapsResultsAndQueriesByExternalGames() = runTest {
        var body: String? = null
        val games = """[{
            "id":1942,"name":"The Witcher 3",
            "external_games":[{"uid":"292030","external_game_source":1,"url":"https://store.steampowered.com/app/292030"}]
        }]"""
        val client = clientWith(
            MockEngine { request ->
                if (isTwitch(request.url.host)) {
                    respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                } else {
                    body = (request.body as TextContent).text
                    respond(games, HttpStatusCode.OK, jsonHeaders)
                }
            },
        )

        val result = client.matchBySteamAppids(listOf("292030")).single()

        assertEquals(1942L, result.igdbId)
        assertEquals("292030", result.externalGames.single { it.category == 1 }.uid)
        val sent = body!!
        assertTrue(sent.contains("external_games.external_game_source = 1"))
        assertTrue(sent.contains("""external_games.uid = ("292030")"""))
    }

    @Test
    fun matchBySteamAppidsChunksLargeRequests() = runTest {
        var gameCalls = 0
        val client = clientWith(
            MockEngine { request ->
                if (isTwitch(request.url.host)) respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                else { gameCalls++; respond("[]", HttpStatusCode.OK, jsonHeaders) }
            },
        )

        client.matchBySteamAppids((1..150).map { it.toString() })

        assertEquals(2, gameCalls)
    }

    @Test
    fun matchBySteamAppidsEmptySkipsTheNetwork() = runTest {
        var calls = 0
        val client = clientWith(MockEngine { calls++; respond("[]", HttpStatusCode.OK, jsonHeaders) })

        assertTrue(client.matchBySteamAppids(emptyList()).isEmpty())
        assertEquals(0, calls)
    }

    @Test
    fun matchByGogIdsMapsResultsAndQueriesByExternalGames() = runTest {
        var body: String? = null
        val games = """[{
            "id":1207658691,"name":"The Witcher 3",
            "external_games":[{"uid":"1207658691","external_game_source":5,"url":"https://www.gog.com/game/the_witcher_3"}]
        }]"""
        val client = clientWith(
            MockEngine { request ->
                if (isTwitch(request.url.host)) {
                    respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                } else {
                    body = (request.body as TextContent).text
                    respond(games, HttpStatusCode.OK, jsonHeaders)
                }
            },
        )

        val result = client.matchByGogIds(listOf("1207658691")).single()

        assertEquals(1207658691L, result.igdbId)
        assertEquals("1207658691", result.externalGames.single { it.category == 5 }.uid)
        val sent = body!!
        assertTrue(sent.contains("external_games.external_game_source = 5"))
        assertTrue(sent.contains("""external_games.uid = ("1207658691")"""))
    }

    @Test
    fun matchByGogIdsChunksLargeRequests() = runTest {
        var gameCalls = 0
        val client = clientWith(
            MockEngine { request ->
                if (isTwitch(request.url.host)) respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                else { gameCalls++; respond("[]", HttpStatusCode.OK, jsonHeaders) }
            },
        )

        client.matchByGogIds((1..150).map { it.toString() })

        assertEquals(2, gameCalls)
    }

    @Test
    fun matchByGogIdsEmptySkipsTheNetwork() = runTest {
        var calls = 0
        val client = clientWith(MockEngine { calls++; respond("[]", HttpStatusCode.OK, jsonHeaders) })

        assertTrue(client.matchByGogIds(emptyList()).isEmpty())
        assertEquals(0, calls)
    }

    @Test
    fun sentRequestCarriesClientIdAndBearer() = runTest {
        var clientIdHeader: String? = null
        var authHeader: String? = null
        val client = clientWith(
            MockEngine { request ->
                if (isTwitch(request.url.host)) {
                    respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                } else {
                    clientIdHeader = request.headers["Client-ID"]
                    authHeader = request.headers[HttpHeaders.Authorization]
                    respond("[]", HttpStatusCode.OK, jsonHeaders)
                }
            },
        )

        client.searchGames("a")

        assertEquals("cid", clientIdHeader)
        assertEquals("Bearer tok_abc", authHeader)
    }
}
