package hr.kotwave.gameslibrary.epic

import hr.kotwave.gameslibrary.data.ExternalRef
import hr.kotwave.gameslibrary.data.IgdbGame
import hr.kotwave.gameslibrary.data.sync.SyncEntry
import hr.kotwave.gameslibrary.data.sync.SyncTailRow
import kotlin.test.Test
import kotlin.test.assertEquals

private const val EPIC_CATEGORY = 26

private fun epicGame(name: String, igdbId: Long, vararg offerIds: String) = IgdbGame(
    igdbId = igdbId,
    name = name,
    externalGames = offerIds.map { ExternalRef(category = EPIC_CATEGORY, uid = it) },
)

class EpicSyncPlanTest {

    @Test
    fun tailRowsCarryEveryUidOfEntitlementsNoIgdbOfferIdClaimed() {
        val owned = listOf(
            EpicOwnedGame(catalogItemId = "c1", namespace = "fn", title = "Control", offerId = "o1"),
            EpicOwnedGame(catalogItemId = "c2", namespace = "fn", title = "Alan Wake", offerId = "o2"),
            EpicOwnedGame(catalogItemId = "c3", namespace = "misc", title = "Bundle Extra", offerId = null),
        )

        val rows = epicTailRows(owned, listOf(epicGame("Control", 1, "o1")))

        assertEquals(
            listOf(
                SyncTailRow(name = "Alan Wake", uids = listOf("c2", "o2")),
                SyncTailRow(name = "Bundle Extra", uids = listOf("c3")),
            ),
            rows,
        )
    }

    @Test
    fun entriesGiveEachMatchedGameEveryUidOfItsEntitlements() {
        val owned = listOf(
            EpicOwnedGame(catalogItemId = "c1", namespace = "fn", title = "Control", offerId = "o1"),
            EpicOwnedGame(catalogItemId = "c1b", namespace = "fn", title = "Control Ultimate", offerId = "o1b"),
            EpicOwnedGame(catalogItemId = "c2", namespace = "fn", title = "Alan Wake", offerId = "o2"),
        )
        val known = listOf(SyncTailRow(name = "Alan Wake", uids = listOf("c2", "o2")))

        val entries = epicEntries(listOf(epicGame("Control", 1, "o1", "o1b")), owned, known)

        assertEquals(
            listOf(
                SyncEntry.Matched(epicGame("Control", 1, "o1", "o1b"), listOf("c1", "o1", "c1b", "o1b")),
                SyncEntry.Unmatched(uids = listOf("c2", "o2"), name = "Alan Wake"),
            ),
            entries,
        )
    }

    @Test
    fun anEntitlementWithoutAnOfferIdNeverJoinsAMatchedGame() {
        val owned = listOf(
            EpicOwnedGame(catalogItemId = "c1", namespace = "fn", title = "Control", offerId = "o1"),
            EpicOwnedGame(catalogItemId = "c3", namespace = "misc", title = "Bundle Extra", offerId = null),
        )

        val entries = epicEntries(listOf(epicGame("Control", 1, "o1")), owned, emptyList())

        assertEquals(listOf(SyncEntry.Matched(epicGame("Control", 1, "o1"), listOf("c1", "o1"))), entries)
    }
}
