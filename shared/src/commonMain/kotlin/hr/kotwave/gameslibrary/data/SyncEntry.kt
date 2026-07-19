package hr.kotwave.gameslibrary.data

/**
 * One owned game resolved for a store's additive sync: either [Matched] to an IGDB Game, or
 * [Unmatched] (added as an `igdbId`-null Game keyed by its store [uids]). [uids] carries every store
 * uid behind the entry so the merge can dedup against (and upgrade) Games from earlier syncs
 * regardless of which uid keyed them — one appid/product id for Steam/GOG; titleIds plus the
 * conceptId for PSN, catalogItemIds plus the offerId for Epic. The ViewModel does the networking
 * (store + IGDB) and hands these to [GameRepository.syncStore].
 */
sealed interface SyncEntry {
    val uids: List<String>

    data class Matched(val igdb: IgdbGame, override val uids: List<String> = emptyList()) : SyncEntry
    data class Unmatched(override val uids: List<String>, val name: String) : SyncEntry
}
