package hr.kotwave.gameslibrary.data

/**
 * One checked Candidate resolved for [GameRepository.confirmImport]: either [Matched] to an IGDB Game,
 * or [Unmatched] (added as an `igdbId`-null Game). The ViewModel does the IGDB networking and hands
 * these over, keeping the additive-merge logic pure and `:shared`-testable (mirrors [SteamSyncEntry]).
 */
sealed interface ImportEntry {
    val store: Store

    data class Matched(val igdb: IgdbGame, override val store: Store) : ImportEntry
    data class Unmatched(val name: String, override val store: Store) : ImportEntry
}
