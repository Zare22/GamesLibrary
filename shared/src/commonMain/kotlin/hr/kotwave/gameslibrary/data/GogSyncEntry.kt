package hr.kotwave.gameslibrary.data

/**
 * One owned GOG game resolved for the additive sync: either [Matched] to an IGDB Game, or [Unmatched]
 * (added as an `igdbId`-null Game keyed by its GOG product id). The ViewModel does the networking (GOG +
 * IGDB) and hands these to [GameRepository.syncGogGames], keeping the additive-merge logic pure and
 * `:shared`-testable (mirrors the Steam sync split, ADR 0016).
 */
sealed interface GogSyncEntry {
    data class Matched(val igdb: IgdbGame) : GogSyncEntry
    data class Unmatched(val gogId: String, val name: String) : GogSyncEntry
}
