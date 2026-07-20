package hr.kotwave.gameslibrary.psn

import hr.kotwave.gameslibrary.data.ExternalRef
import hr.kotwave.gameslibrary.data.IgdbGame
import hr.kotwave.gameslibrary.data.sync.SyncEntry
import hr.kotwave.gameslibrary.data.sync.SyncTailRow
import kotlin.test.Test
import kotlin.test.assertEquals

private const val PSN_CATEGORY = 36

private fun psnGame(name: String, igdbId: Long, vararg conceptIds: String) = IgdbGame(
    igdbId = igdbId,
    name = name,
    externalGames = conceptIds.map { ExternalRef(category = PSN_CATEGORY, uid = it) },
)

class PsnSyncPlanTest {

    @Test
    fun conceptlessTitleIdsPicksOnlyTheRowsSonyLeftWithoutOne() {
        val owned = listOf(
            PsnOwnedGame(conceptId = "10", titleId = "CUSA100", name = "Bloodborne"),
            PsnOwnedGame(conceptId = null, titleId = "CUSA200", name = "Returnal"),
            PsnOwnedGame(conceptId = null, titleId = "CUSA300", name = "Astro Bot"),
        )

        assertEquals(listOf("CUSA200", "CUSA300"), conceptlessTitleIds(owned))
    }

    @Test
    fun withResolvedConceptIdsFillsTheGapAndLeavesUnresolvedRowsNull() {
        val owned = listOf(
            PsnOwnedGame(conceptId = "10", titleId = "CUSA100", name = "Bloodborne"),
            PsnOwnedGame(conceptId = null, titleId = "CUSA200", name = "Returnal"),
            PsnOwnedGame(conceptId = null, titleId = "CUSA300", name = "Astro Bot"),
        )

        val library = withResolvedConceptIds(owned, mapOf("CUSA200" to "20"))

        assertEquals(listOf("10", "20", null), library.map { it.conceptId })
        assertEquals("10", library[0].conceptId)
    }

    @Test
    fun tailRowsCarryBothUidsForEveryRowNoIgdbConceptIdClaimed() {
        val library = listOf(
            PsnOwnedGame(conceptId = "10", titleId = "CUSA100", name = "Bloodborne"),
            PsnOwnedGame(conceptId = "20", titleId = "CUSA200", name = "Returnal"),
            PsnOwnedGame(conceptId = null, titleId = "CUSA300", name = "Astro Bot"),
        )

        val rows = psnTailRows(library, listOf(psnGame("Bloodborne", 1, "10")))

        assertEquals(
            listOf(
                SyncTailRow(name = "Returnal", uids = listOf("CUSA200", "20")),
                SyncTailRow(name = "Astro Bot", uids = listOf("CUSA300")),
            ),
            rows,
        )
    }

    @Test
    fun entriesGiveEachMatchedGameEveryUidOfItsRows() {
        val library = listOf(
            PsnOwnedGame(conceptId = "10", titleId = "CUSA100", name = "Bloodborne"),
            PsnOwnedGame(conceptId = "11", titleId = "CUSA101", name = "Bloodborne (PS5)"),
            PsnOwnedGame(conceptId = "20", titleId = "CUSA200", name = "Returnal"),
        )
        val known = listOf(SyncTailRow(name = "Returnal", uids = listOf("CUSA200", "20")))

        val entries = psnEntries(listOf(psnGame("Bloodborne", 1, "10", "11")), library, known)

        assertEquals(
            listOf(
                SyncEntry.Matched(psnGame("Bloodborne", 1, "10", "11"), listOf("CUSA100", "10", "CUSA101", "11")),
                SyncEntry.Unmatched(uids = listOf("CUSA200", "20"), name = "Returnal"),
            ),
            entries,
        )
    }
}
