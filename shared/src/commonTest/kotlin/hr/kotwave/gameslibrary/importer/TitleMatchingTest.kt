package hr.kotwave.gameslibrary.importer

import hr.kotwave.gameslibrary.data.IgdbSearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TitleMatchingTest {

    private fun result(id: Long, name: String) = IgdbSearchResult(igdbId = id, name = name)

    @Test
    fun normalizeIgnoresCasePunctuationAndWhitespace() {
        assertEquals("marvelsspiderman", normalizeTitle("Marvel's Spider-Man"))
        assertEquals("thewitcher3wildhunt", normalizeTitle("  The   Witcher 3: Wild Hunt  "))
        assertEquals(normalizeTitle("Marvels Spider Man"), normalizeTitle("Marvel's Spider-Man"))
        assertEquals(normalizeTitle("BALDURS GATE 3"), normalizeTitle("Baldur's Gate 3"))
    }

    @Test
    fun noResultsIsUnmatched() {
        assertEquals(MatchClassification.Unmatched, classifyMatch("Some Obscure Game", emptyList()))
    }

    @Test
    fun topResultEqualByNormalizationIsMatched() {
        val results = listOf(result(1, "Marvel's Spider-Man"), result(2, "Marvel's Spider-Man 2"))
        val match = classifyMatch("marvels spider man", results)
        assertIs<MatchClassification.Matched>(match)
        assertEquals(1L, match.result.igdbId)
    }

    @Test
    fun topResultNotEqualIsAmbiguous() {
        val results = listOf(result(1, "The Witcher 3: Wild Hunt"), result(2, "The Witcher"))
        val match = classifyMatch("Witcher 3", results)
        assertIs<MatchClassification.Ambiguous>(match)
        assertEquals(2, match.results.size)
    }

    @Test
    fun exactNameRankedBelowTopStaysAmbiguous() {
        // Conservative: only the top result counts; an exact match further down does not auto-check.
        val results = listOf(result(1, "Resident Evil 4 (2023)"), result(2, "Resident Evil 4"))
        assertIs<MatchClassification.Ambiguous>(classifyMatch("Resident Evil 4", results))
    }
}
