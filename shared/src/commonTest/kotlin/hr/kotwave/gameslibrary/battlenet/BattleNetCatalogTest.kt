package hr.kotwave.gameslibrary.battlenet

import hr.kotwave.gameslibrary.importer.normalizeTitle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleNetCatalogTest {

    @Test
    fun catalogIsNonEmpty() {
        assertTrue(BattleNetCatalog.titles.isNotEmpty())
        assertTrue(BattleNetCatalog.sections.isNotEmpty())
    }

    @Test
    fun everySectionHasTitles() {
        assertTrue(BattleNetCatalog.sections.all { it.titles.isNotEmpty() })
    }

    @Test
    fun titlesIsTheSectionsFlattenedInOrder() {
        assertEquals(BattleNetCatalog.sections.flatMap { it.titles }, BattleNetCatalog.titles)
    }

    @Test
    fun noDuplicateTitles() {
        val titles = BattleNetCatalog.titles
        assertEquals(titles.size, titles.toSet().size)
    }

    @Test
    fun noTitlesCollideAfterNormalization() {
        val normalized = BattleNetCatalog.titles.map { normalizeTitle(it) }
        assertEquals(normalized.size, normalized.toSet().size)
    }

    @Test
    fun coversBlizzardCorePlusCallOfDutyAndCrash() {
        val titles = BattleNetCatalog.titles
        assertTrue("World of Warcraft" in titles)
        assertTrue("Diablo IV" in titles)
        assertTrue("Overwatch 2" in titles)
        assertTrue("StarCraft II" in titles)
        assertTrue(titles.any { it.startsWith("Call of Duty") })
        assertTrue("Crash Bandicoot 4: It's About Time" in titles)
    }
}
