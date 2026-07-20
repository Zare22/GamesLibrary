package hr.kotwave.gameslibrary.psn

import hr.kotwave.gameslibrary.data.IgdbGame
import hr.kotwave.gameslibrary.data.sync.SyncEntry
import hr.kotwave.gameslibrary.data.sync.SyncTailRow

/** IGDB's external-game category for the Playstation Store; the PSN conceptId is its `uid`. */
private const val PSN_EXTERNAL_CATEGORY = 36

/** The conceptIds IGDB knows for this game on the PlayStation Store. */
private fun IgdbGame.psnConceptIds(): Set<String> =
    externalGames.filter { it.category == PSN_EXTERNAL_CATEGORY }.map { it.uid }.toSet()

/** The titleIds Sony returned without a conceptId — the gap the per-title resolve fills. */
internal fun conceptlessTitleIds(owned: List<PsnOwnedGame>): List<String> =
    owned.filter { it.conceptId == null }.map { it.titleId }

/** Folds resolved conceptIds into the rows that lacked one; a title left unresolved keeps its null. */
internal fun withResolvedConceptIds(owned: List<PsnOwnedGame>, resolved: Map<String, String>): List<PsnOwnedGame> =
    owned.map { row -> if (row.conceptId == null) row.copy(conceptId = resolved[row.titleId]) else row }

/** The rows no IGDB conceptId claimed, each carrying both of its uids. */
internal fun psnTailRows(library: List<PsnOwnedGame>, matched: List<IgdbGame>): List<SyncTailRow> {
    val matchedIds = matched.flatMap { it.psnConceptIds() }.toSet()
    return library.filter { it.conceptId !in matchedIds }
        .map { SyncTailRow(name = it.name, uids = listOfNotNull(it.titleId, it.conceptId)) }
}

/** The merge input: one Matched entry per IGDB game carrying its rows' titleIds + conceptIds, then the known tail. */
internal fun psnEntries(
    matched: List<IgdbGame>,
    library: List<PsnOwnedGame>,
    known: List<SyncTailRow>,
): List<SyncEntry> = buildList {
    matched.forEach { game ->
        val conceptIds = game.psnConceptIds()
        val rows = library.filter { it.conceptId in conceptIds }
        add(SyncEntry.Matched(game, rows.flatMap { listOfNotNull(it.titleId, it.conceptId) }.distinct()))
    }
    known.forEach { add(SyncEntry.Unmatched(uids = it.uids, name = it.name)) }
}
