package hr.kotwave.gameslibrary.mirror

import hr.kotwave.gameslibrary.data.Source
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.SyncDismissal
import hr.kotwave.gameslibrary.transfer.ExportedExternal
import hr.kotwave.gameslibrary.transfer.ExportedGame
import hr.kotwave.gameslibrary.transfer.ExportedOwnership
import hr.kotwave.gameslibrary.transfer.LibraryExport

/**
 * Three-way merges the two devices' libraries over the pairing's stored [baseline] (ADR 0038).
 * [theirs] is the host's snapshot — cached metadata resolves host-wins on a both-changed tie.
 * A null [baseline] is the first Mirror: everything unions, and a row pair whose user state
 * differs is unattributable and surfaces as a Conflict once.
 */
fun mirrorMerge(
    baseline: LibraryExport?,
    mine: LibraryExport,
    theirs: LibraryExport,
    mineDismissals: Set<SyncDismissal> = emptySet(),
    theirsDismissals: Set<SyncDismissal> = emptySet(),
): MirrorMerge {
    val rows = pairSnapshots(baseline?.games ?: emptyList(), mine.games, theirs.games)
    val settled = mutableListOf<ExportedGame>()
    val conflicts = mutableListOf<MirrorConflict>()
    val collisions = mutableListOf<MirrorCollision>()

    rows.forEach { row ->
        val b = row.baseline
        val m = row.mine
        val t = row.theirs
        when {
            m != null && t != null -> {
                val (merged, fields) = mergePair(b, m, t)
                if (fields.isEmpty()) {
                    settled += merged
                } else {
                    conflicts += MirrorConflict(MirrorConflictKind.UserState, m, t, fields, merged)
                }
            }

            b == null -> {
                val single = (m ?: t)!!
                val matched = if (single.igdbId == null) {
                    titleMatchedIn(single, if (m != null) theirs.games else mine.games)
                } else {
                    null
                }
                if (matched != null) {
                    collisions += MirrorCollision(single, matched, manualIsMine = m != null)
                } else {
                    settled += single
                }
            }

            else -> {
                val keeper = (m ?: t)!!
                if (userEdited(b, keeper)) {
                    conflicts += MirrorConflict(MirrorConflictKind.DeleteVsEdit, m, t)
                }
            }
        }
    }

    return MirrorMerge(
        settled = settled,
        conflicts = conflicts,
        collisions = collisions,
        dismissals = (mineDismissals + theirsDismissals).toList(),
        mineGames = mine.games,
        theirsGames = theirs.games,
    )
}

/**
 * The attributed merge, pending its Review decisions. [conflicts] and [collisions] are the rows
 * Review must decide; [resolve] folds the decisions in and yields the converged [MirrorOutcome].
 */
class MirrorMerge internal constructor(
    private val settled: List<ExportedGame>,
    val conflicts: List<MirrorConflict>,
    val collisions: List<MirrorCollision>,
    val dismissals: List<SyncDismissal>,
    private val mineGames: List<ExportedGame>,
    private val theirsGames: List<ExportedGame>,
) {

    /** Requires one decision per Conflict and per collision; throws [IllegalArgumentException] on a gap. */
    fun resolve(
        conflictDecisions: List<MirrorConflictDecision> = emptyList(),
        collisionDecisions: List<MirrorCollisionDecision> = emptyList(),
    ): MirrorOutcome {
        val rows = settled.toMutableList()

        conflicts.forEach { conflict ->
            val decision = requireNotNull(conflictDecisions.firstOrNull { it.conflict == conflict }) {
                "Missing decision for conflict on \"${(conflict.mine ?: conflict.theirs)?.name}\""
            }
            when (conflict.kind) {
                MirrorConflictKind.UserState -> {
                    val winner = (if (decision.keepMine) conflict.mine else conflict.theirs)!!
                    var row = conflict.merged!!
                    conflict.fields.forEach { field ->
                        row = when (field) {
                            MirrorField.STATUS -> row.copy(status = winner.status)
                            MirrorField.USER_RATING -> row.copy(userRating = winner.userRating)
                            MirrorField.WISHLIST -> row.copy(wishlist = winner.wishlist)
                        }
                    }
                    rows += row.normalized()
                }

                MirrorConflictKind.DeleteVsEdit -> {
                    (if (decision.keepMine) conflict.mine else conflict.theirs)?.let { rows += it }
                }
            }
        }

        collisions.forEach { collision ->
            val decision = requireNotNull(collisionDecisions.firstOrNull { it.collision == collision }) {
                "Missing decision for collision on \"${collision.manual.name}\""
            }
            val matchedAt = rows.indexOfFirst { it.igdbId == collision.matched.igdbId }
            if (decision.merge && matchedAt >= 0) {
                rows[matchedAt] = rows[matchedAt].folding(collision.manual)
            } else {
                rows += collision.manual
            }
        }

        val converged = rows.sortedWith(compareBy({ it.name.lowercase() }, { it.igdbId ?: Long.MAX_VALUE }))
        return MirrorOutcome(
            converged = converged,
            mineChanges = sideChanges(mineGames, converged),
            theirsChanges = sideChanges(theirsGames, converged),
            dismissals = dismissals,
        )
    }
}

private class PairedRow(val baseline: ExportedGame?, val mine: ExportedGame?, val theirs: ExportedGame?)

/** Pairs mine↔theirs by identity, then attaches each pair's Baseline row by the same identity. */
private fun pairSnapshots(
    baseline: List<ExportedGame>,
    mine: List<ExportedGame>,
    theirs: List<ExportedGame>,
): List<PairedRow> {
    val sideMatches = matchRows(mine, theirs)
    val pairedMine = BooleanArray(mine.size)
    val pairedTheirs = BooleanArray(theirs.size)
    val pairs = mutableListOf<Pair<ExportedGame?, ExportedGame?>>()
    sideMatches.forEach { (mi, ti) ->
        pairs += mine[mi] to theirs[ti]
        pairedMine[mi] = true
        pairedTheirs[ti] = true
    }
    mine.forEachIndexed { i, row -> if (!pairedMine[i]) pairs += row to null }
    theirs.forEachIndexed { i, row -> if (!pairedTheirs[i]) pairs += null to row }

    val reps = pairs.map { (m, t) -> (m ?: t)!! }
    val baseAt = matchRows(reps, baseline).associate { it }
    return pairs.mapIndexed { i, (m, t) -> PairedRow(baseAt[i]?.let(baseline::get), m, t) }
}

/**
 * Identity matching between two row lists, by tier: equal `igdbId`, then a shared external
 * `(category, uid)` (never across two different `igdbId`s), then normalized title between two
 * manual rows. Cross-key title equality (manual vs matched) never matches — that is a
 * [MirrorCollision] or a delete+add, not the same row.
 */
private fun matchRows(left: List<ExportedGame>, right: List<ExportedGame>): List<Pair<Int, Int>> {
    val matches = mutableListOf<Pair<Int, Int>>()
    val leftMatched = BooleanArray(left.size)
    val rightMatched = BooleanArray(right.size)

    val rightByIgdb = mutableMapOf<Long, Int>()
    right.forEachIndexed { i, row -> row.igdbId?.let { rightByIgdb.putIfAbsent(it, i) } }
    left.forEachIndexed { li, row ->
        val ri = row.igdbId?.let(rightByIgdb::get) ?: return@forEachIndexed
        if (!rightMatched[ri]) {
            matches += li to ri
            leftMatched[li] = true
            rightMatched[ri] = true
        }
    }

    val rightByUid = mutableMapOf<Pair<Int, String>, Int>()
    right.forEachIndexed { i, row ->
        if (!rightMatched[i]) row.externals.forEach { rightByUid.putIfAbsent(it.category to it.uid, i) }
    }
    left.forEachIndexed { li, row ->
        if (leftMatched[li]) return@forEachIndexed
        val ri = row.externals.firstNotNullOfOrNull { ext ->
            rightByUid[ext.category to ext.uid]?.takeIf { candidate ->
                !rightMatched[candidate] &&
                    (row.igdbId == null || right[candidate].igdbId == null || row.igdbId == right[candidate].igdbId)
            }
        } ?: return@forEachIndexed
        matches += li to ri
        leftMatched[li] = true
        rightMatched[ri] = true
    }

    val rightByTitle = mutableMapOf<String, Int>()
    right.forEachIndexed { i, row ->
        if (!rightMatched[i] && row.igdbId == null) rightByTitle.putIfAbsent(row.normTitle(), i)
    }
    left.forEachIndexed { li, row ->
        if (leftMatched[li] || row.igdbId != null) return@forEachIndexed
        val ri = rightByTitle[row.normTitle()]?.takeIf { !rightMatched[it] } ?: return@forEachIndexed
        matches += li to ri
        leftMatched[li] = true
        rightMatched[ri] = true
    }
    return matches
}

/** Merges one paired row field by field; returns the merged row and its conflicting user-state fields. */
private fun mergePair(b: ExportedGame?, m: ExportedGame, t: ExportedGame): Pair<ExportedGame, Set<MirrorField>> {
    val conflictFields = mutableSetOf<MirrorField>()
    val hasBase = b != null

    val ownerships = mergeOwnerships(b, m, t)
    val externals = mergeExternals(b, m, t)

    val wishlist = if (ownerships.isNotEmpty()) {
        false
    } else {
        merge3(b?.wishlist, m.wishlist, t.wishlist, hasBase) { conflictFields += MirrorField.WISHLIST }
    }
    val status = merge3(b?.status, m.status, t.status, hasBase) { conflictFields += MirrorField.STATUS }
    val rating = merge3(b?.userRating, m.userRating, t.userRating, hasBase) { conflictFields += MirrorField.USER_RATING }

    val merged = ExportedGame(
        name = mergeMeta(b?.name, m.name, t.name, hasBase),
        igdbId = m.igdbId ?: t.igdbId,
        wishlist = wishlist,
        status = status,
        userRating = rating,
        slug = mergeMeta(b?.slug, m.slug, t.slug, hasBase),
        firstReleaseDate = mergeMeta(b?.firstReleaseDate, m.firstReleaseDate, t.firstReleaseDate, hasBase),
        coverImageId = mergeMeta(b?.coverImageId, m.coverImageId, t.coverImageId, hasBase),
        developer = mergeMeta(b?.developer, m.developer, t.developer, hasBase),
        totalRating = mergeMeta(b?.totalRating, m.totalRating, t.totalRating, hasBase),
        totalRatingCount = mergeMeta(b?.totalRatingCount, m.totalRatingCount, t.totalRatingCount, hasBase),
        platforms = mergeMeta(b?.platforms, m.platforms, t.platforms, hasBase),
        alternativeNames = mergeMeta(b?.alternativeNames, m.alternativeNames, t.alternativeNames, hasBase),
        orphaned = mergeMeta(b?.orphaned, m.orphaned, t.orphaned, hasBase),
        addedAt = listOfNotNull(m.addedAt, t.addedAt).minOrNull(),
        ownerships = ownerships,
        externals = externals,
    )
    // A conflict-pending row stays raw so resolution can overlay without losing non-conflicting fields.
    return (if (conflictFields.isEmpty()) merged.normalized() else merged) to conflictFields
}

/**
 * One user-state field, three ways: equal converges; one side changed since Baseline wins; changed on
 * both to different values conflicts — mine stands in until resolved. With no Baseline (first Mirror)
 * a null yields to the other side's value — union semantics; only value-vs-value differences conflict.
 */
private fun <T> merge3(b: T?, m: T, t: T, hasBase: Boolean, onConflict: () -> Unit): T = when {
    m == t -> m
    !hasBase -> when {
        m == null -> t
        t == null -> m
        else -> {
            onConflict()
            m
        }
    }

    m == b -> t
    t == b -> m
    else -> {
        onConflict()
        m
    }
}

/** One cached-metadata field: never conflicts — one side changed wins, any tie goes to the host ([t]). */
private fun <T> mergeMeta(b: T?, m: T, t: T, hasBase: Boolean): T = when {
    m == t -> t
    hasBase && t == b -> m
    else -> t
}

/**
 * Set-merges Ownerships by Store: additions union in, a side's own removals since Baseline propagate,
 * and a same-Store pair keeps the store-confirmed (non-MANUAL) source, host's on a tie.
 */
private fun mergeOwnerships(b: ExportedGame?, m: ExportedGame, t: ExportedGame): List<ExportedOwnership> {
    val mByStore = m.ownerships.associateBy { it.store }
    val tByStore = t.ownerships.associateBy { it.store }
    val bStores = b?.ownerships?.map { it.store }?.toSet() ?: emptySet()
    return (mByStore.keys + tByStore.keys).mapNotNull { store ->
        val mo = mByStore[store]
        val to = tByStore[store]
        val present = if (store in bStores) mo != null && to != null else mo != null || to != null
        when {
            !present -> null
            mo != null && to != null ->
                if (to.source != Source.MANUAL.name || mo.source == Source.MANUAL.name) to else mo

            else -> mo ?: to
        }
    }.sortedBy { it.store }
}

/** Set-merges external refs by `(category, uid)` with removal propagation; a pair prefers a non-null url. */
private fun mergeExternals(b: ExportedGame?, m: ExportedGame, t: ExportedGame): List<ExportedExternal> {
    val mByUid = m.externals.associateBy { it.category to it.uid }
    val tByUid = t.externals.associateBy { it.category to it.uid }
    val bUids = b?.externals?.map { it.category to it.uid }?.toSet() ?: emptySet()
    return (mByUid.keys + tByUid.keys).mapNotNull { uid ->
        val me = mByUid[uid]
        val te = tByUid[uid]
        val present = if (uid in bUids) me != null && te != null else me != null || te != null
        when {
            !present -> null
            me != null && te != null -> if (te.url != null) te else me
            else -> me ?: te
        }
    }.sortedWith(compareBy({ it.category }, { it.uid }))
}

/** Whether a side changed the row's user state since Baseline — what blocks a peer's delete. */
private fun userEdited(b: ExportedGame, side: ExportedGame): Boolean =
    side.status != b.status || side.userRating != b.userRating || side.wishlist != b.wishlist ||
        side.ownerships.map { it.store }.toSet() != b.ownerships.map { it.store }.toSet()

/** An IGDB-matched row in [rows] titled like [manual] — the cross-key collision counterpart. */
private fun titleMatchedIn(manual: ExportedGame, rows: List<ExportedGame>): ExportedGame? =
    rows.firstOrNull { it.igdbId != null && it.normTitle() == manual.normTitle() }

/** Folds a manual row onto this matched row: sets union, earliest `addedAt`, this row's user state. */
private fun ExportedGame.folding(manual: ExportedGame): ExportedGame = copy(
    ownerships = (ownerships + manual.ownerships)
        .groupBy { it.store }
        .map { (_, rows) -> rows.firstOrNull { it.source != Source.MANUAL.name } ?: rows.first() }
        .sortedBy { it.store },
    externals = (externals + manual.externals)
        .distinctBy { it.category to it.uid }
        .sortedWith(compareBy({ it.category }, { it.uid })),
    addedAt = listOfNotNull(addedAt, manual.addedAt).minOrNull(),
).normalized()

/** Re-asserts the ADR 0004 invariant: an owned row is never Wishlisted and always has a Status. */
private fun ExportedGame.normalized(): ExportedGame {
    val owned = ownerships.isNotEmpty() || !wishlist
    return copy(wishlist = !owned, status = if (owned) status ?: Status.BACKLOG.name else null)
}

/** What [current] must apply to become [converged]: adds, updates (converged form), and deletes. */
private fun sideChanges(current: List<ExportedGame>, converged: List<ExportedGame>): MirrorSideChanges {
    val matches = matchRows(current, converged)
    val currentMatched = BooleanArray(current.size)
    val convergedMatched = BooleanArray(converged.size)
    val updates = mutableListOf<ExportedGame>()
    matches.forEach { (ci, vi) ->
        currentMatched[ci] = true
        convergedMatched[vi] = true
        if (current[ci].canonical() != converged[vi].canonical()) updates += converged[vi]
    }
    return MirrorSideChanges(
        adds = converged.filterIndexed { i, _ -> !convergedMatched[i] },
        updates = updates,
        deletes = current.filterIndexed { i, _ -> !currentMatched[i] },
    )
}

/** Order-independent comparison form: Ownership and external sets sorted. */
private fun ExportedGame.canonical(): ExportedGame = copy(
    ownerships = ownerships.sortedWith(compareBy({ it.store }, { it.source })),
    externals = externals.sortedWith(compareBy({ it.category }, { it.uid })),
)

private fun ExportedGame.normTitle(): String = name.trim().lowercase()
