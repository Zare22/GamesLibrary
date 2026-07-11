package hr.kotwave.gameslibrary.psn

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PsnClientTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun clientWith(engine: MockEngine) = PsnClient(
        buildPsnHttpClient(
            engine,
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS,
            socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS,
            connectTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS,
        ),
        PsnConfig(),
    )

    private fun purchasedBody(games: String, totalCount: Int, offset: Int = 0): String =
        """{"data":{"purchasedTitlesRetrieve":{"games":[$games],"pageInfo":{"isLast":false,"offset":$offset,"size":100,"totalCount":$totalCount}}}}"""

    private fun playedBody(titles: String, nextOffset: Int? = null): String =
        """{"titles":[$titles],"totalItemCount":0${if (nextOffset != null) ""","nextOffset":$nextOffset""" else ""}}"""

    private fun MockRequestHandleScope.respondFor(
        request: HttpRequestData,
        purchased: (start: Int) -> String,
        played: (offset: Int) -> String,
        concepts: (titleId: String) -> String = { "[]" },
    ): HttpResponseData = when {
        request.url.host == "web.np.playstation.com" -> {
            val start = request.url.parameters["variables"]!!.substringAfter("\"start\":").substringBefore(',').trim().toInt()
            respond(purchased(start), HttpStatusCode.OK, jsonHeaders)
        }
        "/catalog/v2/titles/" in request.url.encodedPath -> {
            val titleId = request.url.encodedPath.substringAfter("/titles/").substringBefore("/concepts")
            respond(concepts(titleId), HttpStatusCode.OK, jsonHeaders)
        }
        else -> {
            val offset = request.url.parameters["offset"]!!.toInt()
            respond(played(offset), HttpStatusCode.OK, jsonHeaders)
        }
    }

    @Test
    fun purchasedPaginatesByTotalCountAndCarriesBearer() = runTest {
        val auths = mutableListOf<String?>()
        val client = clientWith(
            MockEngine { request ->
                auths += request.headers[HttpHeaders.Authorization]
                respondFor(
                    request,
                    purchased = { start ->
                        purchasedBody(
                            """{"conceptId":"${start + 1}","titleId":"CUSA0${start + 1}_00","name":"Game ${start + 1}"}""",
                            totalCount = 150,
                            offset = start,
                        )
                    },
                    played = { playedBody("") },
                )
            },
        )

        val games = client.getOwnedGames("tok123")

        assertEquals(listOf("1", "101"), games.map { it.conceptId })
        assertTrue(auths.all { it == "Bearer tok123" })
    }

    @Test
    fun unionDedupsByConceptIdAndTitleId() = runTest {
        val client = clientWith(
            MockEngine { request ->
                respondFor(
                    request,
                    purchased = {
                        purchasedBody(
                            """{"conceptId":"100","titleId":"PPSA100_00","name":"Purchased Game"},
                               {"conceptId":null,"titleId":"CUSA200_00","name":"Old Digital"}""",
                            totalCount = 2,
                        )
                    },
                    played = {
                        playedBody(
                            """{"titleId":"PPSA101_00","name":"Purchased Game Played","concept":{"id":100}},
                               {"titleId":"CUSA200_00","name":"Old Digital Played","concept":{"id":300}},
                               {"titleId":"CUSA999_00","name":"Disc Only Game","concept":{"id":400}}""",
                        )
                    },
                )
            },
        )

        val games = client.getOwnedGames("tok")

        assertEquals(3, games.size)
        assertEquals("Purchased Game", games[0].name)
        assertEquals("Old Digital", games[1].name)
        assertEquals("Disc Only Game", games[2].name)
        assertEquals("400", games[2].conceptId)
    }

    @Test
    fun playedFollowsNextOffsetAndUidFallsBackToTitleId() = runTest {
        val client = clientWith(
            MockEngine { request ->
                respondFor(
                    request,
                    purchased = { purchasedBody("", totalCount = 0) },
                    played = { offset ->
                        when (offset) {
                            0 -> playedBody("""{"titleId":"CUSA001_00","name":"First","concept":{"id":11}}""", nextOffset = 200)
                            else -> playedBody("""{"titleId":"CUSA002_00","name":"Second"}""")
                        }
                    },
                )
            },
        )

        val games = client.getOwnedGames("tok")

        assertEquals(listOf("11", "CUSA002_00"), games.map { it.uid })
    }

    @Test
    fun unionAdoptsPlayedConceptIdForConceptlessPurchasedRow() = runTest {
        val client = clientWith(
            MockEngine { request ->
                respondFor(
                    request,
                    purchased = {
                        purchasedBody("""{"conceptId":null,"titleId":"CUSA00572_00","name":"SHAREfactory™"}""", totalCount = 1)
                    },
                    played = {
                        playedBody("""{"titleId":"CUSA00572_00","name":"SHAREfactory™","concept":{"id":201408}}""")
                    },
                )
            },
        )

        val games = client.getOwnedGames("tok")

        assertEquals(1, games.size)
        assertEquals("201408", games.single().conceptId)
        assertEquals("SHAREfactory", games.single().name)
    }

    @Test
    fun purchasedConceptIdToleratesNumberAndString() = runTest {
        val client = clientWith(
            MockEngine { request ->
                respondFor(
                    request,
                    purchased = {
                        purchasedBody(
                            """{"conceptId":10002856,"titleId":"PPSA100_00","name":"Numeric Concept"},
                               {"conceptId":"229182","titleId":"PPSA200_00","name":"String Concept"}""",
                            totalCount = 2,
                        )
                    },
                    played = { playedBody("") },
                )
            },
        )

        assertEquals(listOf("10002856", "229182"), client.getOwnedGames("tok").map { it.conceptId })
    }

    @Test
    fun namesShedTrademarkGlyphs() = runTest {
        val client = clientWith(
            MockEngine { request ->
                respondFor(
                    request,
                    purchased = {
                        purchasedBody("""{"conceptId":"1","titleId":"CUSA1_00","name":"Rocket League®"}""", totalCount = 1)
                    },
                    played = { playedBody("""{"titleId":"CUSA2_00","name":"EA SPORTS™ FC 25","concept":{"id":2}}""") },
                )
            },
        )

        assertEquals(listOf("Rocket League", "EA SPORTS FC 25"), client.getOwnedGames("tok").map { it.name })
    }

    @Test
    fun resolveConceptIdsMapsNumberAndStringIdsAndToleratesFailures() = runTest {
        val client = clientWith(
            MockEngine { request ->
                val path = request.url.encodedPath
                if ("/catalog/v2/titles/" in path) {
                    when (path.substringAfter("/titles/").substringBefore("/concepts")) {
                        "CUSA1_00" -> respond("""[{"id":201930,"name":"Grand Theft Auto V"}]""", HttpStatusCode.OK, jsonHeaders)
                        "CUSA2_00" -> respond("""[{"id":"229182"}]""", HttpStatusCode.OK, jsonHeaders)
                        "CUSA3_00" -> respond("[]", HttpStatusCode.OK, jsonHeaders)
                        else -> respond("not found", HttpStatusCode.NotFound)
                    }
                } else {
                    respond("[]", HttpStatusCode.OK, jsonHeaders)
                }
            },
        )

        val resolved = client.resolveConceptIds("tok", listOf("CUSA1_00", "CUSA2_00", "CUSA3_00", "CUSA404_00"))

        assertEquals(mapOf("CUSA1_00" to "201930", "CUSA2_00" to "229182"), resolved)
    }

    @Test
    fun persistedQueryNotFoundThrowsQueryOutdated() = runTest {
        val client = clientWith(
            MockEngine { request ->
                respondFor(
                    request,
                    purchased = {
                        """{"errors":[{"message":"PersistedQueryNotFound","extensions":{"code":"PERSISTED_QUERY_NOT_FOUND"}}],"data":{"purchasedTitlesRetrieve":null}}"""
                    },
                    played = { playedBody("") },
                )
            },
        )

        try {
            client.getOwnedGames("tok")
            error("expected PsnQueryOutdatedException")
        } catch (expected: PsnQueryOutdatedException) {
        }
    }

    @Test
    fun transientFailureRetriesThenSucceeds() = runTest {
        var purchasedCalls = 0
        val client = clientWith(
            MockEngine { request ->
                if (request.url.host == "web.np.playstation.com" && purchasedCalls++ == 0) {
                    respond("busy", HttpStatusCode.ServiceUnavailable)
                } else {
                    respondFor(
                        request,
                        purchased = {
                            purchasedBody("""{"conceptId":"1","titleId":"CUSA1_00","name":"Game"}""", totalCount = 1)
                        },
                        played = { playedBody("") },
                    )
                }
            },
        )

        assertEquals(1, client.getOwnedGames("tok").size)
        assertEquals(2, purchasedCalls)
    }

    @Test
    fun nonTransientFailureFailsFast() = runTest {
        var calls = 0
        val client = clientWith(
            MockEngine {
                calls++
                respond("nope", HttpStatusCode.Unauthorized)
            },
        )

        try {
            client.getOwnedGames("tok")
            error("expected PsnException")
        } catch (e: PsnException) {
            assertTrue(e.message!!.contains("401"))
        }
        assertEquals(1, calls)
    }
}
