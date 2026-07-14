package hr.kotwave.gameslibrary.data

/**
 * One owned Epic game resolved for the additive sync: either [Matched] to an IGDB Game, or
 * [Unmatched] (added as an `igdbId`-null Game). [epicUids] carries every Epic uid of the owned rows
 * behind the entry — catalogItemIds plus the offerId when bridged — so the merge can dedup against
 * (and upgrade) Games from earlier syncs regardless of which uid keyed them. The ViewModel does the
 * networking (Epic + IGDB) and hands these to [GameRepository.syncEpicGames].
 */
sealed interface EpicSyncEntry {
    val epicUids: List<String>

    data class Matched(val igdb: IgdbGame, override val epicUids: List<String>) : EpicSyncEntry
    data class Unmatched(override val epicUids: List<String>, val name: String) : EpicSyncEntry
}
