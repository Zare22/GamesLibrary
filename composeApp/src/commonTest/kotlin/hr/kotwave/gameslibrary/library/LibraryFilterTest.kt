package hr.kotwave.gameslibrary.library

import hr.kotwave.gameslibrary.data.Game
import hr.kotwave.gameslibrary.data.GameWithOwnerships
import hr.kotwave.gameslibrary.data.Ownership
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryFilterTest {

    private fun owned(
        id: Long,
        name: String,
        status: Status? = null,
        rating: Double? = null,
        addedAt: Long? = null,
        stores: List<Store> = emptyList(),
    ) = GameWithOwnerships(
        game = Game(id = id, name = name, status = status, userRating = rating, addedAt = addedAt),
        ownerships = stores.map { Ownership(gameId = id, store = it) },
    )

    private val library = listOf(
        owned(1, "Celeste", Status.COMPLETED, rating = 9.0, addedAt = 100, stores = listOf(Store.STEAM)),
        owned(2, "Hades", Status.PLAYING, rating = 8.0, addedAt = 300, stores = listOf(Store.STEAM, Store.GOG)),
        owned(3, "Baldur's Gate 3", Status.BACKLOG, rating = null, addedAt = 200, stores = listOf(Store.GOG)),
        owned(4, "Doom", status = null, rating = 7.0, addedAt = null, stores = listOf(Store.PSN)),
    )

    private fun names(filter: LibraryFilter) = library.filteredSorted(filter).map { it.game.name }

    @Test
    fun searchIsCaseInsensitiveSubstring() {
        assertEquals(listOf("Hades"), names(LibraryFilter(query = "ade")))
        assertEquals(listOf("Hades"), names(LibraryFilter(query = "HADES")))
    }

    @Test
    fun storeFilterIsOrAcrossSelectedStores() {
        // GOG selected -> Hades (Steam+GOG) and Baldur's Gate 3 (GOG); default title sort.
        assertEquals(listOf("Baldur's Gate 3", "Hades"), names(LibraryFilter(stores = setOf(Store.GOG))))
        assertEquals(
            listOf("Baldur's Gate 3", "Doom", "Hades"),
            names(LibraryFilter(stores = setOf(Store.GOG, Store.PSN))),
        )
    }

    @Test
    fun statusFilterMatchesTheNoStatusChip() {
        assertEquals(listOf("Doom"), names(LibraryFilter(statuses = setOf(null))))
        assertEquals(
            listOf("Doom", "Hades"),
            names(LibraryFilter(statuses = setOf(null, Status.PLAYING))),
        )
    }

    @Test
    fun sortByTitleIsCaseInsensitiveAlphabetical() {
        assertEquals(
            listOf("Baldur's Gate 3", "Celeste", "Doom", "Hades"),
            names(LibraryFilter(sort = LibrarySort.TITLE)),
        )
    }

    @Test
    fun sortByRecentlyAddedIsNewestFirstNullsLast() {
        // addedAt: Hades 300, Baldur's 200, Celeste 100, Doom null (sorts last).
        assertEquals(
            listOf("Hades", "Baldur's Gate 3", "Celeste", "Doom"),
            names(LibraryFilter(sort = LibrarySort.RECENTLY_ADDED)),
        )
    }

    @Test
    fun recentlyAddedTiesBreakByIdDescendingAmongNulls() {
        val a = owned(5, "Alpha", addedAt = null)
        val b = owned(6, "Bravo", addedAt = null)
        val ordered = listOf(a, b).filteredSorted(LibraryFilter(sort = LibrarySort.RECENTLY_ADDED))
        assertEquals(listOf("Bravo", "Alpha"), ordered.map { it.game.name })
    }

    @Test
    fun sortByRatingIsHighestFirstNullsLast() {
        // rating: Celeste 9.0, Hades 8.0, Doom 7.0, Baldur's null (last).
        assertEquals(
            listOf("Celeste", "Hades", "Doom", "Baldur's Gate 3"),
            names(LibraryFilter(sort = LibrarySort.RATING)),
        )
    }

    @Test
    fun searchAndFiltersComposeTogether() {
        // Steam-owned AND title contains "e": Celeste and Hades.
        assertEquals(
            listOf("Celeste", "Hades"),
            names(LibraryFilter(query = "e", stores = setOf(Store.STEAM))),
        )
    }

    @Test
    fun activeFacetCountCountsStoresAndStatusesNotSort() {
        assertEquals(0, LibraryFilter(sort = LibrarySort.RATING).activeFacetCount)
        assertEquals(
            3,
            LibraryFilter(stores = setOf(Store.STEAM, Store.GOG), statuses = setOf(null)).activeFacetCount,
        )
    }
}
