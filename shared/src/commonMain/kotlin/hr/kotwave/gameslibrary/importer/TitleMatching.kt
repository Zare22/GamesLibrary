package hr.kotwave.gameslibrary.importer

import hr.kotwave.gameslibrary.data.IgdbSearchResult

private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}]")

/**
 * Reduces a title to lowercase letters and digits only — dropping spacing and punctuation — so that
 * variants like "Marvel's Spider-Man" and "Marvels Spider Man" compare equal.
 */
fun normalizeTitle(title: String): String = title.lowercase().replace(nonAlphanumeric, "")

/** How an imported line's IGDB search resolved. */
sealed interface MatchClassification {
    /** A confident hit — the top result's name equals the line. Pre-checked on Review. */
    data class Matched(val result: IgdbSearchResult) : MatchClassification

    /** Plausible hits but no exact one — the user picks. Unchecked on Review. */
    data class Ambiguous(val results: List<IgdbSearchResult>) : MatchClassification

    /** No IGDB hit — addable as an `igdbId`-null Game. Unchecked on Review. */
    data object Unmatched : MatchClassification
}

/**
 * Classifies a line's IGDB search [results] conservatively: the top result is a [Matched] only when
 * its name normalizes equal to the [query]; any other non-empty result set is [Ambiguous]; none is
 * [Unmatched]. Nothing auto-checks unless it is an exact name match.
 */
fun classifyMatch(query: String, results: List<IgdbSearchResult>): MatchClassification {
    val top = results.firstOrNull() ?: return MatchClassification.Unmatched
    return if (normalizeTitle(top.name) == normalizeTitle(query)) {
        MatchClassification.Matched(top)
    } else {
        MatchClassification.Ambiguous(results)
    }
}
