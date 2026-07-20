package hr.kotwave.gameslibrary.epic

import hr.kotwave.gameslibrary.data.IgdbGame
import hr.kotwave.gameslibrary.data.sync.SyncEntry
import hr.kotwave.gameslibrary.data.sync.SyncTailRow

/** IGDB's external-game category for the Epic Games Store; the Epic store offerId is its `uid`. */
private const val EPIC_EXTERNAL_CATEGORY = 26

/** The offerIds IGDB knows for this game on the Epic Games Store. */
private fun IgdbGame.epicOfferIds(): Set<String> =
    externalGames.filter { it.category == EPIC_EXTERNAL_CATEGORY }.map { it.uid }.toSet()

/** The entitlements no IGDB offerId claimed, each carrying its catalogItemId + offerId. */
internal fun epicTailRows(owned: List<EpicOwnedGame>, matched: List<IgdbGame>): List<SyncTailRow> {
    val matchedIds = matched.flatMap { it.epicOfferIds() }.toSet()
    return owned.filter { it.offerId !in matchedIds }
        .map { SyncTailRow(name = it.title, uids = it.uids) }
}

/** The merge input: one Matched entry per IGDB game carrying its entitlements' uids, then the known tail. */
internal fun epicEntries(
    matched: List<IgdbGame>,
    owned: List<EpicOwnedGame>,
    known: List<SyncTailRow>,
): List<SyncEntry> = buildList {
    matched.forEach { game ->
        val offerIds = game.epicOfferIds()
        val rows = owned.filter { it.offerId in offerIds }
        add(SyncEntry.Matched(game, rows.flatMap { it.uids }.distinct()))
    }
    known.forEach { add(SyncEntry.Unmatched(uids = it.uids, name = it.name)) }
}
