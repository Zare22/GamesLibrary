package hr.kotwave.gameslibrary.epic

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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration.Companion.milliseconds

/** One owned library entitlement, ids only; titles and offers resolve per namespace afterwards. */
data class EpicLibraryItem(
    val namespace: String,
    val catalogItemId: String,
)

/**
 * The Epic library APIs, authorized by the user's bearer token: the entitlement list (ids only),
 * then a per-namespace resolve — bulk catalog for titles and the DLC drop, store offers for the
 * offerId IGDB keys Epic games by (the title-bridge: an offer titled like the owned item).
 */
class EpicClient internal constructor(
    private val httpClient: HttpClient,
    private val config: EpicConfig,
) {
    /**
     * Every owned entitlement, cursor-paginated. Only `APPLICATION` records count (subscriptions
     * and audience entitlements don't), and the Unreal marketplace namespace is dropped whole.
     */
    suspend fun getLibraryItems(accessToken: String): List<EpicLibraryItem> {
        val records = mutableListOf<LibraryRecordDto>()
        var cursor: String? = null
        do {
            val page = fetchLibraryPage(accessToken, cursor)
            records += page.records
            cursor = page.responseMetadata.nextCursor
        } while (cursor != null)
        return records
            .filter { it.recordType == APPLICATION_RECORD_TYPE && it.namespace != UNREAL_NAMESPACE }
            .mapNotNull { record ->
                val namespace = record.namespace ?: return@mapNotNull null
                val catalogItemId = record.catalogItemId ?: return@mapNotNull null
                EpicLibraryItem(namespace = namespace, catalogItemId = catalogItemId)
            }
    }

    private suspend fun fetchLibraryPage(accessToken: String, cursor: String?): LibraryItemsResponse {
        val body = request("library items") {
            httpClient.get(config.libraryItemsUrl) {
                header(HttpHeaders.Authorization, "bearer $accessToken")
                parameter("includeMetadata", "true")
                cursor?.let { parameter("cursor", it) }
            }
        }
        return EpicJson.decodeFromString(body)
    }

    /**
     * Resolves library [items] into owned games, one namespace at a time (a few in flight at once):
     * bulk catalog gives titles and drops DLC (`mainGameItemList` non-empty) and title-less rows;
     * the namespace's offers give each item its offerId — the offer whose normalized title equals
     * the item's, `BASE_GAME` preferred over `EDITION` (delisted titles have no offer and stay
     * unbridged). Namespaces whose items all sit in [skipOffersForCatalogItemIds] (already
     * IGDB-matched from an earlier sync) skip the offers leg.
     *
     * Two account-level filters follow: an unbridged item whose title extends another owned item's
     * title in the same namespace is a companion app (editors, test builds), dropped; equal titles
     * across namespaces (bundle duplicates) collapse onto the bridged copy.
     */
    suspend fun resolveOwnedGames(
        accessToken: String,
        items: List<EpicLibraryItem>,
        skipOffersForCatalogItemIds: Set<String> = emptySet(),
    ): List<EpicOwnedGame> = coroutineScope {
        val semaphore = Semaphore(NAMESPACE_RESOLVE_CONCURRENCY)
        val resolved = items.groupBy { it.namespace }.map { (namespace, nsItems) ->
            async {
                semaphore.withPermit {
                    resolveNamespace(accessToken, namespace, nsItems, skipOffersForCatalogItemIds)
                }
            }
        }.awaitAll().flatten()
        dedupAcrossNamespaces(resolved.flatMap { dropCompanions(it) })
    }

    private suspend fun resolveNamespace(
        accessToken: String,
        namespace: String,
        items: List<EpicLibraryItem>,
        skipOffersFor: Set<String>,
    ): List<List<EpicOwnedGame>> {
        val catalog = fetchCatalogItems(accessToken, namespace, items.map { it.catalogItemId })
        val games = items.mapNotNull { item ->
            val entry = catalog[item.catalogItemId] ?: return@mapNotNull null
            if (entry.mainGameItemList.isNotEmpty()) return@mapNotNull null
            val title = entry.title?.let(::cleanTitle)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            EpicOwnedGame(catalogItemId = item.catalogItemId, namespace = namespace, title = title, offerId = null)
        }
        if (games.isEmpty() || games.all { it.catalogItemId in skipOffersFor }) return listOf(games)
        val offers = fetchOffers(accessToken, namespace)
        return listOf(
            games.map { game ->
                game.copy(offerId = bridgeOffer(game.title, offers))
            },
        )
    }

    /** The offer whose normalized title equals the item's, `BASE_GAME` before `EDITION`. */
    private fun bridgeOffer(title: String, offers: List<OfferDto>): String? {
        val wanted = normalizeForBridge(title)
        val candidates = offers.filter {
            it.offerType in BRIDGEABLE_OFFER_TYPES && normalizeForBridge(it.title.orEmpty()) == wanted
        }
        return (candidates.firstOrNull { it.offerType == "BASE_GAME" } ?: candidates.firstOrNull())?.id
    }

    /**
     * Drops companion apps: an unbridged item whose normalized title strictly extends another owned
     * item's title in the same namespace (`Football Manager 2024 Pre-game editor`, `Mortal Shell
     * Tech Beta`). Bridged items are store games by definition and never dropped.
     */
    private fun dropCompanions(games: List<EpicOwnedGame>): List<EpicOwnedGame> {
        val normalized = games.associateWith { normalizeForBridge(it.title) }
        return games.filter { game ->
            val title = normalized.getValue(game)
            game.offerId != null || games.none { other ->
                other !== game && title != normalized.getValue(other) && title.startsWith(normalized.getValue(other))
            }
        }
    }

    /** Bundle purchases duplicate a game across namespaces; equal titles collapse onto the bridged copy. */
    private fun dedupAcrossNamespaces(games: List<EpicOwnedGame>): List<EpicOwnedGame> =
        games.sortedBy { it.offerId == null }.distinctBy { normalizeForBridge(it.title) }

    private suspend fun fetchCatalogItems(
        accessToken: String,
        namespace: String,
        catalogItemIds: List<String>,
    ): Map<String, CatalogItemDto> {
        val resolved = mutableMapOf<String, CatalogItemDto>()
        catalogItemIds.chunked(CATALOG_IDS_PER_REQUEST).forEach { chunk ->
            val body = request("bulk catalog") {
                httpClient.get("${config.catalogNamespaceUrl}/$namespace/bulk/items") {
                    header(HttpHeaders.Authorization, "bearer $accessToken")
                    chunk.forEach { parameter("id", it) }
                    parameter("includeDLCDetails", "true")
                    parameter("includeMainGameDetails", "true")
                    parameter("country", "US")
                    parameter("locale", "en-US")
                }
            }
            resolved += EpicJson.decodeFromString<Map<String, CatalogItemDto>>(body)
        }
        return resolved
    }

    private suspend fun fetchOffers(accessToken: String, namespace: String): List<OfferDto> {
        val offers = mutableListOf<OfferDto>()
        var start = 0
        var total: Int
        do {
            val body = request("namespace offers") {
                httpClient.get("${config.catalogNamespaceUrl}/$namespace/offers") {
                    header(HttpHeaders.Authorization, "bearer $accessToken")
                    parameter("start", start)
                    parameter("count", OFFERS_PAGE_SIZE)
                    parameter("country", "US")
                    parameter("locale", "en-US")
                }
            }
            val page = EpicJson.decodeFromString<OffersResponse>(body)
            offers += page.elements
            total = page.paging.total
            start += OFFERS_PAGE_SIZE
        } while (start < total)
        return offers
    }

    /**
     * Runs one request, retrying transient failures — request timeouts and 429/5xx responses — with
     * a short backoff; other failures fail fast.
     */
    private suspend fun request(description: String, block: suspend () -> HttpResponse): String {
        var lastError: Exception = EpicException("Epic $description request did not complete")
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
                    throw EpicException("Epic $description request failed: ${response.status}")
                }
                lastError = EpicException("Epic $description request failed: ${response.status}")
            }
            if (attemptIndex < RETRY_BACKOFFS.size) delay(RETRY_BACKOFFS[attemptIndex])
        }
        throw lastError
    }
}

/** Titles shed `®`/`™`/`©` and collapse whitespace, as in the paste parsers. */
private fun cleanTitle(title: String): String =
    title.replace(TRADEMARKS, "").replace(WHITESPACE, " ").trim()

/** The bridge compares titles Epic-side only: lowercase, alphanumerics — punctuation variants collapse. */
private fun normalizeForBridge(title: String): String =
    title.lowercase().filter { it.isLetterOrDigit() }

private val TRADEMARKS = Regex("[®™©]")
private val WHITESPACE = Regex("\\s+")

/** Epic responses worth retrying: rate-limit (429) and server errors (5xx). */
private fun HttpStatusCode.isTransient(): Boolean =
    this == HttpStatusCode.TooManyRequests || value in 500..599

/** Backoff before each retry; index i is the wait after attempt i failed. Its length sets the retry count. */
private val RETRY_BACKOFFS = listOf(500.milliseconds, 1_500.milliseconds)

/** Epic request attempts before giving up: 1 initial + one per backoff entry. */
private val MAX_ATTEMPTS = RETRY_BACKOFFS.size + 1

/** Games and their companion apps; subscriptions and audience entitlements carry other record types. */
private const val APPLICATION_RECORD_TYPE = "APPLICATION"

/** Unreal marketplace assets live in the `ue` namespace — never games. */
private const val UNREAL_NAMESPACE = "ue"

/** Offer types that sell the game itself; DLC/ADD_ON/UNLOCKABLE/OTHERS never key an owned game. */
private val BRIDGEABLE_OFFER_TYPES = setOf("BASE_GAME", "EDITION")

/** Namespace resolves in flight at once — two requests each (catalog + offers), easy on Epic's edge. */
private const val NAMESPACE_RESOLVE_CONCURRENCY = 4

/** Catalog item ids per bulk request, bounding the query-string length. */
private const val CATALOG_IDS_PER_REQUEST = 40

private const val OFFERS_PAGE_SIZE = 100
