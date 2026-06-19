package hr.kotwave.gameslibrary.importer

import kotlin.test.Test
import kotlin.test.assertEquals

class GenericLineParserTest {

    @Test
    fun oneTitlePerLine() {
        val lines = GenericLineParser.parse("Hades\nCeleste\nHollow Knight")
        assertEquals(listOf("Hades", "Celeste", "Hollow Knight"), lines.map { it.title })
    }

    @Test
    fun trimsAndDropsBlankLines() {
        val lines = GenericLineParser.parse("  Hades  \n\n\t\n  Celeste\n   \n")
        assertEquals(listOf("Hades", "Celeste"), lines.map { it.title })
    }

    @Test
    fun handlesCrlfLineEndings() {
        val lines = GenericLineParser.parse("Hades\r\nCeleste\r\n")
        assertEquals(listOf("Hades", "Celeste"), lines.map { it.title })
    }

    @Test
    fun collapsesCaseInsensitiveDuplicates() {
        val lines = GenericLineParser.parse("Hades\nHADES\n  hades  \nCeleste")
        assertEquals(listOf("Hades", "Celeste"), lines.map { it.title })
    }

    @Test
    fun keepsRawTitleVerbatimForGenericMode() {
        val lines = GenericLineParser.parse("Baldur's Gate 3 — 100% · Platinum")
        assertEquals("Baldur's Gate 3 — 100% · Platinum", lines.single().title)
        assertEquals("Baldur's Gate 3 — 100% · Platinum", lines.single().raw)
    }

    @Test
    fun emptyTextYieldsNothing() {
        assertEquals(emptyList(), GenericLineParser.parse("   \n\n\t"))
    }
}
