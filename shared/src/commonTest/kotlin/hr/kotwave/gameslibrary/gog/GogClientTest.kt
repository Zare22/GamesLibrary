package hr.kotwave.gameslibrary.gog

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GogClientTest {

    private val config = GogConfig()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun clientWith(engine: MockEngine) = GogClient(buildGogHttpClient(engine), config)

    @Test
    fun pullsAllPagesAndCarriesBearer() = runTest {
        val auths = mutableListOf<String?>()
        val client = clientWith(
            MockEngine { request ->
                auths += request.headers[HttpHeaders.Authorization]
                val page = request.url.parameters["page"]?.toInt() ?: 1
                val body = """{"page":$page,"totalPages":2,"products":[
                    {"id":${page}001,"title":"Game $page A"},
                    {"id":${page}002,"title":"Game $page B"}
                ]}"""
                respond(body, HttpStatusCode.OK, jsonHeaders)
            },
        )

        val games = client.getOwnedProducts("tok123")

        assertEquals(4, games.size)
        assertEquals(
            listOf(1001L, 1002L, 2001L, 2002L),
            games.map { it.id },
        )
        assertTrue(auths.all { it == "Bearer tok123" })
    }

    @Test
    fun skipsProductsWithoutTitle() = runTest {
        val body = """{"page":1,"totalPages":1,"products":[
            {"id":1,"title":""},
            {"id":2,"title":"Real Game"},
            {"id":3}
        ]}"""
        val client = clientWith(MockEngine { respond(body, HttpStatusCode.OK, jsonHeaders) })

        assertEquals(listOf(GogOwnedGame(2, "Real Game")), client.getOwnedProducts("tok"))
    }

    @Test
    fun emptyLibraryYieldsEmptyList() = runTest {
        val client = clientWith(
            MockEngine { respond("""{"page":1,"totalPages":1,"products":[]}""", HttpStatusCode.OK, jsonHeaders) },
        )

        assertTrue(client.getOwnedProducts("tok").isEmpty())
    }

    @Test
    fun failureThrows() = runTest {
        val client = clientWith(MockEngine { respond("nope", HttpStatusCode.Unauthorized, jsonHeaders) })

        try {
            client.getOwnedProducts("tok")
            error("expected GogException")
        } catch (e: GogException) {
            assertTrue(e.message!!.contains("401"))
        }
    }
}
