package hr.kotwave.gameslibrary.mirror

import hr.kotwave.gameslibrary.data.SyncDismissal
import hr.kotwave.gameslibrary.transfer.ExportedExternal
import hr.kotwave.gameslibrary.transfer.ExportedGame
import hr.kotwave.gameslibrary.transfer.ExportedOwnership
import hr.kotwave.gameslibrary.transfer.LibraryExport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MirrorMergeTest {

    // --- Attribution: adds and deletes ---

    @Test
    fun addsOnEitherSideAttributeToTheOtherSidesApply() {
        val alpha = game("Alpha", igdbId = 1)
        val merge = mirrorMerge(
            baseline = lib(alpha),
            mine = lib(alpha, game("Mine New", igdbId = 2)),
            theirs = lib(alpha, game("Their New", igdbId = 3)),
        )

        assertTrue(merge.conflicts.isEmpty())
        val outcome = merge.resolve()
        assertEquals(listOf(1L, 2L, 3L), outcome.converged.map { it.igdbId }.sortedBy { it })
        assertEquals(listOf("Their New"), outcome.mineChanges.adds.map { it.name })
        assertEquals(listOf("Mine New"), outcome.theirsChanges.adds.map { it.name })
        assertTrue(outcome.mineChanges.deletes.isEmpty() && outcome.theirsChanges.deletes.isEmpty())
    }

    @Test
    fun deletionOnOneSidePropagatesWhenThePeerLeftTheRowUntouched() {
        val alpha = game("Alpha", igdbId = 1)
        val beta = game("Beta", igdbId = 2)
        val gamma = game("Gamma", igdbId = 3)
        val merge = mirrorMerge(
            baseline = lib(alpha, beta, gamma),
            mine = lib(alpha, gamma), // deleted Beta
            theirs = lib(alpha, beta), // deleted Gamma
        )

        assertTrue(merge.conflicts.isEmpty())
        val outcome = merge.resolve()
        assertEquals(listOf("Alpha"), outcome.converged.map { it.name })
        assertEquals(listOf("Gamma"), outcome.mineChanges.deletes.map { it.name })
        assertEquals(listOf("Beta"), outcome.theirsChanges.deletes.map { it.name })
    }

    @Test
    fun deletingOnBothSidesConvergesSilently() {
        val beta = game("Beta", igdbId = 2)
        val outcome = mirrorMerge(baseline = lib(beta), mine = lib(), theirs = lib()).resolve()

        assertTrue(outcome.converged.isEmpty())
        assertTrue(outcome.mineChanges.deletes.isEmpty() && outcome.theirsChanges.deletes.isEmpty())
    }

    @Test
    fun deleteVsEditConflictsAndResolvesBothWays() {
        val beta = game("Beta", igdbId = 2, status = "BACKLOG")
        val betaEdited = beta.copy(status = "COMPLETED")
        val merge = mirrorMerge(baseline = lib(beta), mine = lib(), theirs = lib(betaEdited))

        val conflict = merge.conflicts.single()
        assertEquals(MirrorConflictKind.DeleteVsEdit, conflict.kind)
        assertNull(conflict.mine)
        assertEquals("COMPLETED", conflict.theirs?.status)

        val keptDeleted = merge.resolve(listOf(MirrorConflictDecision(conflict, keepMine = true)))
        assertTrue(keptDeleted.converged.isEmpty())
        assertEquals(listOf("Beta"), keptDeleted.theirsChanges.deletes.map { it.name })

        val keptEdit = merge.resolve(listOf(MirrorConflictDecision(conflict, keepMine = false)))
        assertEquals("COMPLETED", keptEdit.converged.single().status)
        assertEquals(listOf("Beta"), keptEdit.mineChanges.adds.map { it.name })
    }

    // --- Field rules ---

    @Test
    fun statusChangedOnOneSideWinsAndLandsInThePeersUpdates() {
        val alpha = game("Alpha", igdbId = 1, status = "BACKLOG")
        val merge = mirrorMerge(
            baseline = lib(alpha),
            mine = lib(alpha.copy(status = "PLAYING")),
            theirs = lib(alpha),
        )

        assertTrue(merge.conflicts.isEmpty())
        val outcome = merge.resolve()
        assertEquals("PLAYING", outcome.converged.single().status)
        assertEquals(listOf("PLAYING"), outcome.theirsChanges.updates.map { it.status })
        assertTrue(outcome.mineChanges.updates.isEmpty())
    }

    @Test
    fun sameChangeOnBothSidesConvergesSilently() {
        val alpha = game("Alpha", igdbId = 1, status = "BACKLOG")
        val outcome = mirrorMerge(
            baseline = lib(alpha),
            mine = lib(alpha.copy(status = "PLAYING")),
            theirs = lib(alpha.copy(status = "PLAYING")),
        ).resolve()

        assertEquals("PLAYING", outcome.converged.single().status)
        assertTrue(outcome.mineChanges.updates.isEmpty() && outcome.theirsChanges.updates.isEmpty())
    }

    @Test
    fun differentChangesOnBothSidesConflictAsOneRowAndResolveEitherWay() {
        val alpha = game("Alpha", igdbId = 1, status = "BACKLOG", userRating = null)
        val merge = mirrorMerge(
            baseline = lib(alpha),
            mine = lib(alpha.copy(status = "PLAYING", userRating = 8.0)),
            theirs = lib(alpha.copy(status = "COMPLETED", userRating = 9.0)),
        )

        val conflict = merge.conflicts.single()
        assertEquals(MirrorConflictKind.UserState, conflict.kind)
        assertEquals(setOf(MirrorField.STATUS, MirrorField.USER_RATING), conflict.fields)

        val mineWins = merge.resolve(listOf(MirrorConflictDecision(conflict, keepMine = true))).converged.single()
        assertEquals("PLAYING", mineWins.status)
        assertEquals(8.0, mineWins.userRating)

        val theirsWins = merge.resolve(listOf(MirrorConflictDecision(conflict, keepMine = false))).converged.single()
        assertEquals("COMPLETED", theirsWins.status)
        assertEquals(9.0, theirsWins.userRating)
    }

    @Test
    fun clearingARatingSinceBaselineWinsOverAnUnchangedPeer() {
        val alpha = game("Alpha", igdbId = 1, userRating = 7.0)
        val outcome = mirrorMerge(
            baseline = lib(alpha),
            mine = lib(alpha.copy(userRating = null)),
            theirs = lib(alpha),
        ).resolve()

        assertNull(outcome.converged.single().userRating)
    }

    @Test
    fun metadataChangedOnOneSideWinsEvenAgainstTheHost() {
        val alpha = game("Alpha", igdbId = 1, developer = "Old Studio")
        val outcome = mirrorMerge(
            baseline = lib(alpha),
            mine = lib(alpha.copy(developer = "New Studio")),
            theirs = lib(alpha),
        ).resolve()

        assertEquals("New Studio", outcome.converged.single().developer)
    }

    @Test
    fun metadataChangedOnBothSidesGoesToTheHostWithoutConflict() {
        val alpha = game("Alpha", igdbId = 1, developer = "Old Studio")
        val merge = mirrorMerge(
            baseline = lib(alpha),
            mine = lib(alpha.copy(developer = "Mine Studio")),
            theirs = lib(alpha.copy(developer = "Host Studio")),
        )

        assertTrue(merge.conflicts.isEmpty())
        assertEquals("Host Studio", merge.resolve().converged.single().developer)
    }

    // --- First Mirror (no Baseline) ---

    @Test
    fun firstMirrorUnionsBothLibrariesAndNeverDeletes() {
        val shared = game("Shared", igdbId = 1, status = "PLAYING")
        val merge = mirrorMerge(
            baseline = null,
            mine = lib(shared, game("Mine Only", igdbId = 2)),
            theirs = lib(shared, game("Theirs Only", igdbId = 3)),
        )

        assertTrue(merge.conflicts.isEmpty())
        val outcome = merge.resolve()
        assertEquals(3, outcome.converged.size)
        assertEquals(listOf("Theirs Only"), outcome.mineChanges.adds.map { it.name })
        assertEquals(listOf("Mine Only"), outcome.theirsChanges.adds.map { it.name })
        assertTrue(outcome.mineChanges.deletes.isEmpty() && outcome.theirsChanges.deletes.isEmpty())
    }

    @Test
    fun firstMirrorUserStateDifferenceIsUnattributableAndConflictsOnce() {
        val merge = mirrorMerge(
            baseline = null,
            mine = lib(game("Alpha", igdbId = 1, status = "PLAYING")),
            theirs = lib(game("Alpha", igdbId = 1, status = "COMPLETED")),
        )

        val conflict = merge.conflicts.single()
        assertEquals(setOf(MirrorField.STATUS), conflict.fields)
        val outcome = merge.resolve(listOf(MirrorConflictDecision(conflict, keepMine = false)))
        assertEquals("COMPLETED", outcome.converged.single().status)
    }

    @Test
    fun firstMirrorNullUserStateYieldsToTheSideWithAValue() {
        val merge = mirrorMerge(
            baseline = null,
            mine = lib(game("Alpha", igdbId = 1, userRating = null)),
            theirs = lib(game("Alpha", igdbId = 1, userRating = 8.5)),
        )

        assertTrue(merge.conflicts.isEmpty())
        assertEquals(8.5, merge.resolve().converged.single().userRating)
    }

    @Test
    fun firstMirrorWishlistVsOwnedWithAStoreResolvesByOwnershipWithoutConflict() {
        val merge = mirrorMerge(
            baseline = null,
            mine = lib(game("Alpha", igdbId = 1, wishlist = true)),
            theirs = lib(game("Alpha", igdbId = 1, status = "BACKLOG", ownerships = listOf(steam()))),
        )

        assertTrue(merge.conflicts.isEmpty())
        val row = merge.resolve().converged.single()
        assertFalse(row.wishlist)
        assertEquals("BACKLOG", row.status)
        assertEquals(listOf(steam()), row.ownerships)
    }

    @Test
    fun firstMirrorWishlistVsUntrackedOwnedConflictsAndKeepsTheWinnersShape() {
        val merge = mirrorMerge(
            baseline = null,
            mine = lib(game("Alpha", igdbId = 1, wishlist = true)),
            theirs = lib(game("Alpha", igdbId = 1, status = "PLAYING")),
        )

        val conflict = merge.conflicts.single()
        assertEquals(setOf(MirrorField.WISHLIST), conflict.fields)

        val wishlisted = merge.resolve(listOf(MirrorConflictDecision(conflict, keepMine = true))).converged.single()
        assertTrue(wishlisted.wishlist)
        assertNull(wishlisted.status)

        val owned = merge.resolve(listOf(MirrorConflictDecision(conflict, keepMine = false))).converged.single()
        assertFalse(owned.wishlist)
        assertEquals("PLAYING", owned.status)
    }

    // --- Re-match (identity change) ---

    @Test
    fun rematchSinceBaselineReadsAsDeletePlusAddOnThePeer() {
        val old = game("Alpha", igdbId = 1)
        val rematched = game("Alpha", igdbId = 99)
        val merge = mirrorMerge(baseline = lib(old), mine = lib(rematched), theirs = lib(old))

        assertTrue(merge.conflicts.isEmpty())
        val outcome = merge.resolve()
        assertEquals(listOf(99L), outcome.converged.map { it.igdbId })
        assertEquals(listOf(1L), outcome.theirsChanges.deletes.map { it.igdbId })
        assertEquals(listOf(99L), outcome.theirsChanges.adds.map { it.igdbId })
    }

    @Test
    fun rematchAgainstAPeerEditSurfacesAsDeleteVsEdit() {
        val old = game("Alpha", igdbId = 1, status = "BACKLOG")
        val merge = mirrorMerge(
            baseline = lib(old),
            mine = lib(game("Alpha", igdbId = 99)),
            theirs = lib(old.copy(status = "COMPLETED")),
        )

        val conflict = merge.conflicts.single()
        assertEquals(MirrorConflictKind.DeleteVsEdit, conflict.kind)
        assertNull(conflict.mine)
        assertEquals(1L, conflict.theirs?.igdbId)
    }

    // --- Ownership and external set-merge ---

    @Test
    fun ownershipsUnionAdditionsAndPropagateOwnRemovals() {
        val baseline = game("Alpha", igdbId = 1, ownerships = listOf(steam()))
        val merge = mirrorMerge(
            baseline = lib(baseline),
            mine = lib(baseline.copy(ownerships = listOf(steam(), gog()))), // added GOG
            theirs = lib(baseline.copy(ownerships = emptyList(), wishlist = false)), // removed STEAM
        )

        assertTrue(merge.conflicts.isEmpty())
        assertEquals(listOf(gog()), merge.resolve().converged.single().ownerships)
    }

    @Test
    fun sameStoreOwnershipKeepsTheSyncConfirmedSourceOverManual() {
        val merge = mirrorMerge(
            baseline = null,
            mine = lib(
                game(
                    "Alpha",
                    igdbId = 1,
                    status = "BACKLOG",
                    ownerships = listOf(steam("MANUAL"), gog("GOG_SYNC")),
                ),
            ),
            theirs = lib(
                game(
                    "Alpha",
                    igdbId = 1,
                    status = "BACKLOG",
                    ownerships = listOf(steam("STEAM_SYNC"), gog("MANUAL")),
                ),
            ),
        )

        assertEquals(
            listOf(gog("GOG_SYNC"), steam("STEAM_SYNC")),
            merge.resolve().converged.single().ownerships,
        )
    }

    @Test
    fun externalsUnionAdditionsAndPropagateOwnRemovals() {
        val baseline = game("Alpha", igdbId = 1, externals = listOf(ExportedExternal(1, "100")))
        val merge = mirrorMerge(
            baseline = lib(baseline),
            mine = lib(baseline.copy(externals = listOf(ExportedExternal(1, "100"), ExportedExternal(5, "200")))),
            theirs = lib(baseline.copy(externals = emptyList())),
        )

        assertEquals(listOf(ExportedExternal(5, "200")), merge.resolve().converged.single().externals)
    }

    @Test
    fun anOwnershipArrivingOnAWishlistRowClearsWishlist() {
        val wished = game("Alpha", igdbId = 1, wishlist = true)
        val outcome = mirrorMerge(
            baseline = lib(wished),
            mine = lib(wished),
            theirs = lib(wished.copy(wishlist = false, status = "BACKLOG", ownerships = listOf(steam()))),
        ).resolve()

        val row = outcome.converged.single()
        assertFalse(row.wishlist)
        assertEquals("BACKLOG", row.status)
        assertEquals(listOf(steam()), row.ownerships)
        assertEquals(1, outcome.mineChanges.updates.size)
    }

    // --- addedAt ---

    @Test
    fun addedAtConvergesToTheEarliestStampAndNullNeverErasesOne() {
        val merge = mirrorMerge(
            baseline = null,
            mine = lib(game("Alpha", igdbId = 1, addedAt = 2_000L), game("Beta", igdbId = 2, addedAt = null)),
            theirs = lib(game("Alpha", igdbId = 1, addedAt = 1_000L), game("Beta", igdbId = 2, addedAt = 1_500L)),
        )

        val byId = merge.resolve().converged.associateBy { it.igdbId }
        assertEquals(1_000L, byId[1L]?.addedAt)
        assertEquals(1_500L, byId[2L]?.addedAt)
    }

    // --- Identity tiers ---

    @Test
    fun sharedExternalUidPairsAManualRowToItsMatchedCounterpart() {
        val merge = mirrorMerge(
            baseline = null,
            mine = lib(game("Elden Ring", status = "BACKLOG", externals = listOf(ExportedExternal(1, "123")))),
            theirs = lib(
                game("ELDEN RING", igdbId = 7, status = "BACKLOG", externals = listOf(ExportedExternal(1, "123"))),
            ),
        )

        assertTrue(merge.conflicts.isEmpty() && merge.collisions.isEmpty())
        val row = merge.resolve().converged.single()
        assertEquals(7L, row.igdbId)
        assertEquals("ELDEN RING", row.name) // metadata tie goes to the host
    }

    @Test
    fun normalizedTitlePairsManualRowsAcrossTrimAndCase() {
        val merge = mirrorMerge(
            baseline = null,
            mine = lib(game(" Hades ", status = "BACKLOG")),
            theirs = lib(game("hades", status = "BACKLOG")),
        )

        assertEquals(1, merge.resolve().converged.size)
    }

    // --- Cross-key title collision ---

    @Test
    fun crossKeyTitleCollisionGoesToReviewAndResolvesEitherWay() {
        val manual = game("Elden Ring", status = "BACKLOG", ownerships = listOf(gog()))
        val matched = game("elden ring", igdbId = 7, status = "PLAYING", ownerships = listOf(steam()))
        val merge = mirrorMerge(baseline = null, mine = lib(manual), theirs = lib(matched))

        val collision = merge.collisions.single()
        assertTrue(collision.manualIsMine)
        assertEquals(7L, collision.matched.igdbId)

        val folded = merge.resolve(
            collisionDecisions = listOf(MirrorCollisionDecision(collision, merge = true)),
        ).converged.single()
        assertEquals(7L, folded.igdbId)
        assertEquals("PLAYING", folded.status) // the matched side's user state wins a fold
        assertEquals(listOf(gog(), steam()), folded.ownerships)

        val keptApart = merge.resolve(
            collisionDecisions = listOf(MirrorCollisionDecision(collision, merge = false)),
        ).converged
        assertEquals(2, keptApart.size)
    }

    @Test
    fun foldingAnOwnedManualRowOntoAWishlistMatchClearsWishlist() {
        val manual = game("Elden Ring", status = "BACKLOG", ownerships = listOf(gog()))
        val matchedWish = game("Elden Ring", igdbId = 7, wishlist = true)
        val merge = mirrorMerge(baseline = null, mine = lib(manual), theirs = lib(matchedWish))

        val folded = merge.resolve(
            collisionDecisions = listOf(MirrorCollisionDecision(merge.collisions.single(), merge = true)),
        ).converged.single()
        assertFalse(folded.wishlist)
        assertEquals("BACKLOG", folded.status)
    }

    @Test
    fun aManualRowAlreadyInTheBaselineNeverReRaisesACollision() {
        val manual = game("Elden Ring", status = "BACKLOG")
        val matched = game("Elden Ring", igdbId = 7, status = "PLAYING")
        val merge = mirrorMerge(
            baseline = lib(manual, matched),
            mine = lib(manual, matched),
            theirs = lib(manual, matched),
        )

        assertTrue(merge.collisions.isEmpty())
        assertEquals(2, merge.resolve().converged.size)
    }

    // --- Dismissals and decision coverage ---

    @Test
    fun dismissalsUnionAcrossBothSides() {
        val merge = mirrorMerge(
            baseline = null,
            mine = lib(),
            theirs = lib(),
            mineDismissals = setOf(SyncDismissal(1, "a")),
            theirsDismissals = setOf(SyncDismissal(1, "a"), SyncDismissal(5, "b")),
        )

        assertEquals(setOf(SyncDismissal(1, "a"), SyncDismissal(5, "b")), merge.resolve().dismissals.toSet())
    }

    @Test
    fun resolveRequiresADecisionForEveryConflictAndCollision() {
        val merge = mirrorMerge(
            baseline = null,
            mine = lib(game("Alpha", igdbId = 1, status = "PLAYING")),
            theirs = lib(game("Alpha", igdbId = 1, status = "COMPLETED")),
        )

        assertFailsWith<IllegalArgumentException> { merge.resolve() }
    }

    // --- Helpers ---

    private fun lib(vararg games: ExportedGame) = LibraryExport(games = games.toList())

    private fun steam(source: String = "MANUAL") = ExportedOwnership("STEAM", source)

    private fun gog(source: String = "MANUAL") = ExportedOwnership("GOG", source)

    private fun game(
        name: String,
        igdbId: Long? = null,
        wishlist: Boolean = false,
        status: String? = if (wishlist) null else "BACKLOG",
        userRating: Double? = null,
        developer: String? = null,
        addedAt: Long? = null,
        ownerships: List<ExportedOwnership> = emptyList(),
        externals: List<ExportedExternal> = emptyList(),
    ): ExportedGame = ExportedGame(
        name = name,
        igdbId = igdbId,
        wishlist = wishlist,
        status = status,
        userRating = userRating,
        developer = developer,
        addedAt = addedAt,
        ownerships = ownerships,
        externals = externals,
    )
}
