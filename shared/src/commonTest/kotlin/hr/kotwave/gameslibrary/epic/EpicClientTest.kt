package hr.kotwave.gameslibrary.epic

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EpicClientTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun clientWith(engine: MockEngine) = EpicClient(
        buildEpicHttpClient(
            engine,
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS,
            socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS,
            connectTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS,
        ),
        EpicConfig(),
    )

    private fun record(namespace: String, catalogItemId: String, recordType: String = "APPLICATION") =
        """{"namespace":"$namespace","catalogItemId":"$catalogItemId","appName":"app_$catalogItemId","recordType":"$recordType"}"""

    private fun libraryBody(records: String, nextCursor: String? = null) =
        """{"records":[$records],"responseMetadata":{${if (nextCursor != null) """"nextCursor":"$nextCursor"""" else ""}}}"""

    private fun catalogItem(title: String, dlc: Boolean = false) =
        """{"title":"$title","mainGameItemList":[${if (dlc) """{"id":"main1"}""" else ""}]}"""

    private fun offer(id: String, title: String, offerType: String) =
        """{"id":"$id","title":"$title","offerType":"$offerType"}"""

    private fun offersBody(offers: String, start: Int = 0, count: Int = 100, total: Int = 1) =
        """{"elements":[$offers],"paging":{"start":$start,"count":$count,"total":$total}}"""

    @Test
    fun libraryFollowsCursorAndCarriesBearer() = runTest {
        val auths = mutableListOf<String?>()
        val client = clientWith(
            MockEngine { request ->
                auths += request.headers[HttpHeaders.Authorization]
                when (request.url.parameters["cursor"]) {
                    null -> respond(libraryBody(record("ns1", "item1"), nextCursor = "abc"), HttpStatusCode.OK, jsonHeaders)
                    "abc" -> respond(libraryBody(record("ns2", "item2")), HttpStatusCode.OK, jsonHeaders)
                    else -> error("unexpected cursor")
                }
            },
        )

        val items = client.getLibraryItems("tok123")

        assertEquals(
            listOf(EpicLibraryItem("ns1", "item1"), EpicLibraryItem("ns2", "item2")),
            items,
        )
        assertTrue(auths.all { it == "bearer tok123" })
    }

    @Test
    fun libraryDropsNonApplicationRecordsAndUnrealNamespace() = runTest {
        val client = clientWith(
            MockEngine {
                respond(
                    libraryBody(
                        listOf(
                            record("ns1", "game1"),
                            record("ue", "asset1"),
                            record("ns2", "sub1", recordType = "SUBSCRIPTION"),
                        ).joinToString(","),
                    ),
                    HttpStatusCode.OK,
                    jsonHeaders,
                )
            },
        )

        assertEquals(listOf(EpicLibraryItem("ns1", "game1")), client.getLibraryItems("tok"))
    }

    @Test
    fun resolveBridgesOfferPrefersBaseGameAndCleansTitle() = runTest {
        val client = clientWith(
            MockEngine { request ->
                when {
                    "/bulk/items" in request.url.encodedPath ->
                        respond("""{"game1":${catalogItem("World War Z™")}}""", HttpStatusCode.OK, jsonHeaders)
                    "/offers" in request.url.encodedPath -> respond(
                        offersBody(
                            listOf(
                                offer("editionOffer", "World War Z", "EDITION"),
                                offer("baseOffer", "WORLD WAR Z!", "BASE_GAME"),
                                offer("dlcOffer", "World War Z", "DLC"),
                            ).joinToString(","),
                            total = 3,
                        ),
                        HttpStatusCode.OK,
                        jsonHeaders,
                    )
                    else -> error("unexpected ${request.url}")
                }
            },
        )

        val owned = client.resolveOwnedGames("tok", listOf(EpicLibraryItem("wombat", "game1")))

        assertEquals(1, owned.size)
        assertEquals("World War Z", owned.single().title)
        assertEquals("baseOffer", owned.single().offerId)
    }

    @Test
    fun resolveLeavesDelistedTitlesUnbridged() = runTest {
        val client = clientWith(
            MockEngine { request ->
                when {
                    "/bulk/items" in request.url.encodedPath ->
                        respond("""{"fm24":${catalogItem("Football Manager 2024")}}""", HttpStatusCode.OK, jsonHeaders)
                    "/offers" in request.url.encodedPath -> respond(
                        offersBody(offer("editorOffer", "Football Manager 2024 In-Game Editor", "DLC")),
                        HttpStatusCode.OK,
                        jsonHeaders,
                    )
                    else -> error("unexpected ${request.url}")
                }
            },
        )

        val owned = client.resolveOwnedGames("tok", listOf(EpicLibraryItem("fmns", "fm24")))

        assertEquals(1, owned.size)
        assertNull(owned.single().offerId)
        assertEquals("Football Manager 2024", owned.single().title)
    }

    @Test
    fun resolveDropsDlcItemsAndTitlelessItems() = runTest {
        val client = clientWith(
            MockEngine { request ->
                when {
                    "/bulk/items" in request.url.encodedPath -> respond(
                        """{"game1":${catalogItem("Mortal Shell")},"dlc1":${catalogItem("The Virtuous Cycle", dlc = true)},"ghost1":{"mainGameItemList":[]}}""",
                        HttpStatusCode.OK,
                        jsonHeaders,
                    )
                    "/offers" in request.url.encodedPath -> respond(
                        offersBody(offer("mortalOffer", "Mortal Shell", "BASE_GAME")),
                        HttpStatusCode.OK,
                        jsonHeaders,
                    )
                    else -> error("unexpected ${request.url}")
                }
            },
        )

        val owned = client.resolveOwnedGames(
            "tok",
            listOf(
                EpicLibraryItem("ns", "game1"),
                EpicLibraryItem("ns", "dlc1"),
                EpicLibraryItem("ns", "ghost1"),
            ),
        )

        assertEquals(listOf("Mortal Shell"), owned.map { it.title })
    }

    @Test
    fun resolveDropsCompanionAppsButKeepsTheirGame() = runTest {
        val client = clientWith(
            MockEngine { request ->
                when {
                    "/bulk/items" in request.url.encodedPath -> respond(
                        """{"fm":${catalogItem("Football Manager 2024")},""" +
                            """"editor":${catalogItem("Football Manager 2024 Pre-game editor")},""" +
                            """"archiver":${catalogItem("Football Manager 2024 Resource archiver")}}""",
                        HttpStatusCode.OK,
                        jsonHeaders,
                    )
                    "/offers" in request.url.encodedPath ->
                        respond(offersBody("", total = 0), HttpStatusCode.OK, jsonHeaders)
                    else -> error("unexpected ${request.url}")
                }
            },
        )

        val owned = client.resolveOwnedGames(
            "tok",
            listOf(
                EpicLibraryItem("fmns", "fm"),
                EpicLibraryItem("fmns", "editor"),
                EpicLibraryItem("fmns", "archiver"),
            ),
        )

        // The delisted game survives unbridged; its title-extending companions don't.
        assertEquals(listOf("Football Manager 2024"), owned.map { it.title })
        assertNull(owned.single().offerId)
    }

    @Test
    fun companionRuleNeverDropsBridgedItems() = runTest {
        val client = clientWith(
            MockEngine { request ->
                when {
                    "/bulk/items" in request.url.encodedPath -> respond(
                        """{"chiv":${catalogItem("Chivalry")},"chiv2":${catalogItem("Chivalry 2")}}""",
                        HttpStatusCode.OK,
                        jsonHeaders,
                    )
                    "/offers" in request.url.encodedPath -> respond(
                        offersBody(
                            listOf(
                                offer("chivOffer", "Chivalry", "BASE_GAME"),
                                offer("chiv2Offer", "Chivalry 2", "BASE_GAME"),
                            ).joinToString(","),
                            total = 2,
                        ),
                        HttpStatusCode.OK,
                        jsonHeaders,
                    )
                    else -> error("unexpected ${request.url}")
                }
            },
        )

        val owned = client.resolveOwnedGames(
            "tok",
            listOf(EpicLibraryItem("ns", "chiv"), EpicLibraryItem("ns", "chiv2")),
        )

        // "Chivalry 2" extends "Chivalry" but both bridged to offers — both stay.
        assertEquals(setOf("Chivalry", "Chivalry 2"), owned.map { it.title }.toSet())
    }

    @Test
    fun resolveDedupsBundleDuplicatesOntoBridgedCopy() = runTest {
        val client = clientWith(
            MockEngine { request ->
                val path = request.url.encodedPath
                when {
                    "/namespace/nsA/bulk/items" in path ->
                        respond("""{"copyA":${catalogItem("Levelhead")}}""", HttpStatusCode.OK, jsonHeaders)
                    "/namespace/nsB/bulk/items" in path ->
                        respond("""{"copyB":${catalogItem("Levelhead")}}""", HttpStatusCode.OK, jsonHeaders)
                    "/namespace/nsA/offers" in path ->
                        respond(offersBody("", total = 0), HttpStatusCode.OK, jsonHeaders)
                    "/namespace/nsB/offers" in path -> respond(
                        offersBody(offer("levelOffer", "Levelhead", "BASE_GAME")),
                        HttpStatusCode.OK,
                        jsonHeaders,
                    )
                    else -> error("unexpected ${request.url}")
                }
            },
        )

        val owned = client.resolveOwnedGames(
            "tok",
            listOf(EpicLibraryItem("nsA", "copyA"), EpicLibraryItem("nsB", "copyB")),
        )

        assertEquals(1, owned.size)
        assertEquals("levelOffer", owned.single().offerId)
    }

    @Test
    fun resolvePagesOffersByTotal() = runTest {
        var offersCalls = 0
        val client = clientWith(
            MockEngine { request ->
                when {
                    "/bulk/items" in request.url.encodedPath ->
                        respond("""{"game1":${catalogItem("Deep Game")}}""", HttpStatusCode.OK, jsonHeaders)
                    "/offers" in request.url.encodedPath -> {
                        offersCalls++
                        when (request.url.parameters["start"]!!.toInt()) {
                            0 -> respond(
                                offersBody(offer("filler", "Filler", "DLC"), start = 0, total = 101),
                                HttpStatusCode.OK,
                                jsonHeaders,
                            )
                            else -> respond(
                                offersBody(offer("deepOffer", "Deep Game", "BASE_GAME"), start = 100, total = 101),
                                HttpStatusCode.OK,
                                jsonHeaders,
                            )
                        }
                    }
                    else -> error("unexpected ${request.url}")
                }
            },
        )

        val owned = client.resolveOwnedGames("tok", listOf(EpicLibraryItem("bigns", "game1")))

        assertEquals(2, offersCalls)
        assertEquals("deepOffer", owned.single().offerId)
    }

    @Test
    fun resolveSkipsOffersForFullyMatchedNamespaces() = runTest {
        var offersCalls = 0
        val client = clientWith(
            MockEngine { request ->
                when {
                    "/bulk/items" in request.url.encodedPath ->
                        respond("""{"game1":${catalogItem("Known Game")}}""", HttpStatusCode.OK, jsonHeaders)
                    "/offers" in request.url.encodedPath -> {
                        offersCalls++
                        respond(offersBody("", total = 0), HttpStatusCode.OK, jsonHeaders)
                    }
                    else -> error("unexpected ${request.url}")
                }
            },
        )

        val owned = client.resolveOwnedGames(
            "tok",
            listOf(EpicLibraryItem("ns", "game1")),
            skipOffersForCatalogItemIds = setOf("game1"),
        )

        assertEquals(0, offersCalls)
        assertEquals("Known Game", owned.single().title)
        assertNull(owned.single().offerId)
    }

    @Test
    fun transientFailureRetriesThenSucceeds() = runTest {
        var calls = 0
        val client = clientWith(
            MockEngine {
                if (calls++ == 0) {
                    respond("busy", HttpStatusCode.ServiceUnavailable)
                } else {
                    respond(libraryBody(record("ns1", "item1")), HttpStatusCode.OK, jsonHeaders)
                }
            },
        )

        assertEquals(1, client.getLibraryItems("tok").size)
        assertEquals(2, calls)
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
            client.getLibraryItems("tok")
            error("expected EpicException")
        } catch (e: EpicException) {
            assertTrue(e.message!!.contains("401"))
        }
        assertEquals(1, calls)
    }
}
