package hr.kotwave.gameslibrary.library

import hr.kotwave.gameslibrary.data.GameWithOwnerships
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store

/** Sort orders for the Library grid. */
enum class LibrarySort { TITLE, RECENTLY_ADDED, RATING }

/**
 * Library view-state: the search query plus the active filter/sort facets. Empty [stores]/[statuses]
 * mean "no filter" (everything passes). A null entry in [statuses] matches owned Games with no Status.
 */
data class LibraryFilter(
    val query: String = "",
    val stores: Set<Store> = emptySet(),
    val statuses: Set<Status?> = emptySet(),
    val sort: LibrarySort = LibrarySort.TITLE,
) {
    /** Count shown on the Sliders button; sort is not counted (it always has a value). */
    val activeFacetCount: Int get() = stores.size + statuses.size
}

/**
 * Applies [filter]'s title search and store/status filters, then its sort. Pure over the owned-games
 * list, so it drives the grid without rendering and is unit-testable on its own.
 */
fun List<GameWithOwnerships>.filteredSorted(filter: LibraryFilter): List<GameWithOwnerships> {
    val query = filter.query.trim()
    val matched = this.filter { owned ->
        (query.isEmpty() || owned.game.name.contains(query, ignoreCase = true)) &&
            (filter.stores.isEmpty() || owned.ownerships.any { it.store in filter.stores }) &&
            (filter.statuses.isEmpty() || owned.game.status in filter.statuses)
    }
    return when (filter.sort) {
        LibrarySort.TITLE ->
            matched.sortedBy { it.game.name.lowercase() }
        LibrarySort.RECENTLY_ADDED ->
            matched.sortedWith(
                compareByDescending<GameWithOwnerships> { it.game.addedAt ?: Long.MIN_VALUE }
                    .thenByDescending { it.game.id },
            )
        LibrarySort.RATING ->
            matched.sortedWith(
                compareByDescending<GameWithOwnerships> { it.game.userRating ?: Double.NEGATIVE_INFINITY }
                    .thenBy { it.game.name.lowercase() },
            )
    }
}
