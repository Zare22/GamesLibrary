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
 * A sync-tail line also carries its store [uids] and a [dismissed] state ("don't ask again"); checked
 * and dismissed are mutually exclusive.
 */
@Stable
class ImportCandidate(
    val rawTitle: String,
    val classification: MatchClassification,
    val alreadyInLibrary: Boolean,
    val uids: List<String> = emptyList(),
) {
    var checked by mutableStateOf(classification is MatchClassification.Matched)

    var dismissed by mutableStateOf(false)
        private set

    /** The chosen option for an Ambiguous line; -1 until the user picks. Unused otherwise. */
    var pickedIndex by mutableIntStateOf(-1)
        private set

    fun toggleChecked() {
        checked = !checked
        if (checked) dismissed = false
    }

    fun toggleDismissed() {
        dismissed = !dismissed
        if (dismissed) checked = false
    }

    /** Picks an Ambiguous option (and checks the row); picking again toggles the check off. */
    fun pick(index: Int) {
        if (pickedIndex == index && checked) {
            checked = false
        } else {
            pickedIndex = index
            checked = true
            dismissed = false
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
