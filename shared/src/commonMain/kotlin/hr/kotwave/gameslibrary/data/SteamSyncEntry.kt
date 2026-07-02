package hr.kotwave.gameslibrary.data

/**
 * One owned Steam game resolved for the additive sync: either [Matched] to an IGDB Game, or
 * [Unmatched] (added as an `igdbId`-null Game keyed by its Steam appid). The ViewModel does the
 * networking (Steam + IGDB) and hands these to [GameRepository.syncSteamGames].
 */
sealed interface SteamSyncEntry {
    data class Matched(val igdb: IgdbGame) : SteamSyncEntry
    data class Unmatched(val appid: String, val name: String) : SteamSyncEntry
}
