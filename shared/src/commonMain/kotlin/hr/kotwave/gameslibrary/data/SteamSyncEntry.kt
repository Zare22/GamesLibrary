package hr.kotwave.gameslibrary.data

/**
 * One owned Steam game resolved for the additive sync: either [Matched] to an IGDB Game, or
 * [Unmatched] (added as an `igdbId`-null Game keyed by its Steam appid). [Matched.appids] carries the
 * owned appids behind the entry when IGDB may lack the Steam reference (a Review-resolved pick), so
 * the merge records them for re-sync dedup. The ViewModel does the networking (Steam + IGDB) and
 * hands these to [GameRepository.syncSteamGames].
 */
sealed interface SteamSyncEntry {
    data class Matched(val igdb: IgdbGame, val appids: List<String> = emptyList()) : SteamSyncEntry
    data class Unmatched(val appid: String, val name: String) : SteamSyncEntry
}
