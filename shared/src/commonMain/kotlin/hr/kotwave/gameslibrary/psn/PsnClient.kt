package hr.kotwave.gameslibrary.psn

import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.time.Duration.Companion.milliseconds

/**
 * The PSN library APIs, authorized by the user's bearer token: the purchased list (a persisted
 * GraphQL query, digital purchases incl. never-played) unioned with the played list (gamelist v2,
 * which still carries disc games Sony hides from purchased).
 */
class PsnClient internal constructor(
    private val httpClient: HttpClient,
    private val config: PsnConfig,
) {
    /**
     * Every owned/played game across both lists, purchased first (its names are the cleanest),
     * deduped by conceptId and titleId. Purchased rows often carry a null conceptId (the played list
     * is the reliable source), so a concept-less purchased row adopts the conceptId of the played row
     * sharing its titleId before the dedup. Rows with no titleId or name are skipped.
     */
    suspend fun getOwnedGames(accessToken: String): List<PsnOwnedGame> {
        val purchased = getPurchasedGames(accessToken)
        val played = getPlayedGames(accessToken)
        val playedConceptByTitleId = played
            .mapNotNull { row -> row.conceptId?.let { row.titleId to it } }
            .toMap()
        val enriched = purchased.map { row ->
            if (row.conceptId == null) row.copy(conceptId = playedConceptByTitleId[row.titleId]) else row
        }
        val seenConceptIds = HashSet<String>()
        val seenTitleIds = HashSet<String>()
        return (enriched + played).filter { game ->
            val duplicate = game.conceptId in seenConceptIds || game.titleId in seenTitleIds
            game.conceptId?.let(seenConceptIds::add)
            seenTitleIds.add(game.titleId)
            !duplicate
        }
    }

    private suspend fun getPurchasedGames(accessToken: String): List<PsnOwnedGame> {
        val games = mutableListOf<PsnOwnedGame>()
        var start = 0
        do {
            val page = fetchPurchasedPage(accessToken, start)
            games += page.games.mapNotNull { it.toOwnedGame() }
            start += PURCHASED_PAGE_SIZE
        } while (start < page.pageInfo.totalCount)
        return games
    }

    private suspend fun fetchPurchasedPage(accessToken: String, start: Int): PurchasedTitles {
        val body = request("purchased list") {
            httpClient.get(config.graphqlUrl) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("x-apollo-operation-name", "getPurchasedGameList")
                parameter("operationName", "getPurchasedGameList")
                parameter("variables", purchasedVariables(start))
                parameter("extensions", PURCHASED_EXTENSIONS)
            }
        }
        val envelope = PsnJson.decodeFromString<PurchasedGamesEnvelope>(body)
        if (envelope.errors.any { it.isPersistedQueryNotFound() }) throw PsnQueryOutdatedException()
        return envelope.data?.purchasedTitlesRetrieve
            ?: throw PsnException("PSN purchased list returned no data: ${envelope.errors.firstOrNull()?.message}")
    }

    private fun purchasedVariables(start: Int): String = buildJsonObject {
        put("isActive", true)
        putJsonArray("platform") { PURCHASED_PLATFORMS.forEach { add(it) } }
        put("start", start)
        put("size", PURCHASED_PAGE_SIZE)
        put("sortBy", "ACTIVE_DATE")
        put("sortDirection", "desc")
        put("subscriptionService", "NONE")
    }.toString()

    /**
     * Resolves conceptIds for [titleIds] via the per-title catalog endpoint (the purchased list
     * rarely carries one), a few requests in flight at a time. Titles Sony can't resolve — and
     * individual request failures — are simply absent from the result: the row stays unmatched
     * rather than failing the sync.
     */
    suspend fun resolveConceptIds(accessToken: String, titleIds: List<String>): Map<String, String> = coroutineScope {
        val semaphore = Semaphore(CONCEPT_RESOLVE_CONCURRENCY)
        titleIds.distinct().map { titleId ->
            async {
                semaphore.withPermit {
                    val conceptId = try {
                        fetchConceptId(accessToken, titleId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    }
                    conceptId?.let { titleId to it }
                }
            }
        }.awaitAll().filterNotNull().toMap()
    }

    private suspend fun fetchConceptId(accessToken: String, titleId: String): String? {
        val body = request("title concepts") {
            httpClient.get("${config.titleCatalogUrl}/$titleId/concepts") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("age", 99)
                parameter("country", "US")
                parameter("language", "en-US")
            }
        }
        return PsnJson.decodeFromString<List<ConceptDto>>(body).firstOrNull()?.id?.contentOrNull
    }

    private suspend fun getPlayedGames(accessToken: String): List<PsnOwnedGame> {
        val games = mutableListOf<PsnOwnedGame>()
        var offset = 0
        while (true) {
            val page = fetchPlayedPage(accessToken, offset)
            games += page.titles.mapNotNull { it.toOwnedGame() }
            val next = page.nextOffset
            if (next == null || next <= offset) break
            offset = next
        }
        return games
    }

    private suspend fun fetchPlayedPage(accessToken: String, offset: Int): PlayedTitlesResponse {
        val body = request("played list") {
            httpClient.get(config.playedTitlesUrl) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("categories", PLAYED_CATEGORIES)
                parameter("limit", PLAYED_PAGE_SIZE)
                parameter("offset", offset)
            }
        }
        return PsnJson.decodeFromString(body)
    }

    /**
     * Runs one request, retrying transient failures — request timeouts and 429/5xx responses — with
     * a short backoff; other failures fail fast.
     */
    private suspend fun request(description: String, block: suspend () -> HttpResponse): String {
        var lastError: Exception = PsnException("PSN $description request did not complete")
        for (attemptIndex in 0 until MAX_ATTEMPTS) {
            val response = try {
                block()
            } catch (e: SocketTimeoutException) {
                lastError = e; null
            } catch (e: ConnectTimeoutException) {
                lastError = e; null
            } catch (e: HttpRequestTimeoutException) {
                lastError = e; null
            }
            if (response != null) {
                if (response.status.isSuccess()) return response.bodyAsText()
                if (!response.status.isTransient()) {
                    throw PsnException("PSN $description request failed: ${response.status}")
                }
                lastError = PsnException("PSN $description request failed: ${response.status}")
            }
            if (attemptIndex < RETRY_BACKOFFS.size) delay(RETRY_BACKOFFS[attemptIndex])
        }
        throw lastError
    }
}

/** PSN responses worth retrying: rate-limit (429) and server errors (5xx). */
private fun HttpStatusCode.isTransient(): Boolean =
    this == HttpStatusCode.TooManyRequests || value in 500..599

/** Backoff before each retry; index i is the wait after attempt i failed. Its length sets the retry count. */
private val RETRY_BACKOFFS = listOf(500.milliseconds, 1_500.milliseconds)

/** PSN request attempts before giving up: 1 initial + one per backoff entry. */
private val MAX_ATTEMPTS = RETRY_BACKOFFS.size + 1

/**
 * sha256 of the `getPurchasedGameList` persisted GraphQL query, reverse-engineered from Sony's
 * library.playstation.com JS bundle (this variant also accepts `ps3` and returns `pageInfo`).
 * Re-derive after a rotation: open library.playstation.com with DevTools → Network, filter
 * `graphql/v1/op`, and copy `extensions.persistedQuery.sha256Hash` off a `getPurchasedGameList`
 * request. A rotated hash surfaces as [PsnQueryOutdatedException].
 */
private const val PURCHASED_QUERY_HASH = "2c045408b0a4d0264bb5a3edfed4efd49fb4749cf8d216be9043768adff905e2"

private const val PURCHASED_EXTENSIONS =
    """{"persistedQuery":{"version":1,"sha256Hash":"$PURCHASED_QUERY_HASH"}}"""

private val PURCHASED_PLATFORMS = listOf("ps3", "ps4", "ps5")

private const val PURCHASED_PAGE_SIZE = 100

/** Concept resolutions in flight at once — far under the ~300 req/15 min budget even first-sync. */
private const val CONCEPT_RESOLVE_CONCURRENCY = 4

/** gamelist v2 has no PS3 category — played coverage is PS4/PS5; PS3 rides the purchased list only. */
private const val PLAYED_CATEGORIES = "ps4_game,ps5_native_game"

private const val PLAYED_PAGE_SIZE = 200
