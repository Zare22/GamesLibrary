package hr.kotwave.gameslibrary.data

/**
 * One owned GOG game resolved for the additive sync: either [Matched] to an IGDB Game, or [Unmatched]
 * (added as an `igdbId`-null Game keyed by its GOG product id). [Matched.gogIds] carries the owned
 * product ids behind the entry when IGDB may lack the GOG reference (a Review-resolved pick), so the
 * merge records them for re-sync dedup. The ViewModel does the networking (GOG + IGDB) and hands
 * these to [GameRepository.syncGogGames].
 */
sealed interface GogSyncEntry {
    data class Matched(val igdb: IgdbGame, val gogIds: List<String> = emptyList()) : GogSyncEntry
    data class Unmatched(val gogId: String, val name: String) : GogSyncEntry
}
