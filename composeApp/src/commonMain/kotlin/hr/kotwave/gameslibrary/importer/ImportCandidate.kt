package hr.kotwave.gameslibrary.importer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hr.kotwave.gameslibrary.data.IgdbSearchResult

/**
 * One parsed line on its way through Review: its raw text, how IGDB [classification] resolved it, a
 * user-controlled [checked] state, and — for an [MatchClassification.Ambiguous] line — which option
 * the user [pickedIndex] picked. A [MatchClassification.Matched] line starts checked; the rest do not.
 */
@Stable
class ImportCandidate(
    val rawTitle: String,
    val classification: MatchClassification,
    val alreadyInLibrary: Boolean,
) {
    var checked by mutableStateOf(classification is MatchClassification.Matched)

    /** The chosen option for an Ambiguous line; -1 until the user picks. Unused otherwise. */
    var pickedIndex by mutableIntStateOf(-1)
        private set

    /** Picks an Ambiguous option (and checks the row); picking again toggles the check off. */
    fun pick(index: Int) {
        if (pickedIndex == index && checked) {
            checked = false
        } else {
            pickedIndex = index
            checked = true
        }
    }

    /** The IGDB result this row will import as, or null for an Unmatched (or unpicked Ambiguous) row. */
    val resolved: IgdbSearchResult?
        get() = when (classification) {
            is MatchClassification.Matched -> classification.result
            is MatchClassification.Ambiguous -> classification.results.getOrNull(pickedIndex)
            MatchClassification.Unmatched -> null
        }
}
