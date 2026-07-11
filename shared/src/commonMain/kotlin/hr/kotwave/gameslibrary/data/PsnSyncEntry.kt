package hr.kotwave.gameslibrary.data

/**
 * One owned PSN game resolved for the additive sync: either [Matched] to an IGDB Game, or [Unmatched]
 * (added as an `igdbId`-null Game). [psnUids] carries every PSN uid of the owned rows behind the entry
 * — titleIds plus the conceptId when known — so the merge can dedup against (and upgrade) Games from
 * earlier syncs regardless of which uid keyed them. The ViewModel does the networking (PSN + IGDB)
 * and hands these to [GameRepository.syncPsnGames].
 */
sealed interface PsnSyncEntry {
    val psnUids: List<String>

    data class Matched(val igdb: IgdbGame, override val psnUids: List<String>) : PsnSyncEntry
    data class Unmatched(override val psnUids: List<String>, val name: String) : PsnSyncEntry
}
