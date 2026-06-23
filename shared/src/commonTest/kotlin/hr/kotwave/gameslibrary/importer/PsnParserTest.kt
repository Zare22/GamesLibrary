package hr.kotwave.gameslibrary.importer

import hr.kotwave.gameslibrary.data.Store
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PsnParserTest {

    @Test
    fun parsesRealPasteToUniqueCleanedTitles() {
        val titles = PsnParser.parse(PSN_SAMPLE).map { it.title }
        assertEquals(
            listOf(
                "My friend Peppa Pig",
                "Nine Sols",
                "WUCHANG: Fallen Feathers",
                "EA SPORTS FC 26",
                "SWORD ART ONLINE Fractured Daydream",
                "Tomb Raider I-III Remastered Starring Lara Croft",
                "Lords of the Fallen",
                "The Crew 2",
                "PAW Patrol: Grand Prix",
                "Test Drive Unlimited - Solar Crown",
                "Overpass 2",
                "Assetto Corsa Competizione",
                "LEGO Horizon Adventures",
                "Ghost of Tsushima",
                "Indiana Jones and the Staff of Kings",
                "CAPTAIN TSUBASA: RISE OF NEW CHAMPIONS",
            ),
            titles,
        )
    }

    @Test
    fun rejectsHeaderAndFooterChrome() {
        val titles = PsnParser.parse(PSN_SAMPLE).map { it.title }
        for (chrome in listOf("Sony", "Game Library", "Purchased", "Filter", "All", "PS4", "PS5", "PS Plus")) {
            assertFalse(chrome in titles, "chrome leaked as a title: $chrome")
        }
    }

    @Test
    fun collapsesCrossPlatformDuplicates() {
        val titles = PsnParser.parse(PSN_SAMPLE).map { it.title }
        assertEquals(16, titles.size)
        assertEquals(1, titles.count { it == "Nine Sols" })
    }

    @Test
    fun stripsTrademarkSymbolsButKeepsRawLine() {
        val lego = PsnParser.parse(PSN_SAMPLE).single { it.title == "LEGO Horizon Adventures" }
        assertEquals("LEGO® Horizon Adventures™", lego.raw)
        assertTrue(PsnParser.parse(PSN_SAMPLE).none { "®" in it.title || "™" in it.title })
    }

    @Test
    fun parsesEveryPageOfAConcatenatedMultiPagePaste() {
        val twoPages = """
Sony
Game Library
    Purchased
    Played
    Downloads
    PlayStation Plus
Purchased (1 - 2 of 4)
    Hades
    PS5
    PS Plus
    Celeste
    PS4
    PS Plus
Filter
All
PS5
PS4
Country/Region: Croatia
Sony
Game Library
    Purchased
Purchased (3 - 4 of 4)
    Celeste
    PS5
    PS Plus
    Hollow Knight
    PS5
    PS Plus
Filter
All
PS5
PS4
"""
        assertEquals(listOf("Hades", "Celeste", "Hollow Knight"), PsnParser.parse(twoPages).map { it.title })
    }

    @Test
    fun keepsLastGameWhenBlockEndsAtPlatformTag() {
        val titles = PsnParser.parse("Hollow Knight\nPS5").map { it.title }
        assertEquals(listOf("Hollow Knight"), titles)
    }

    @Test
    fun fallsBackToGenericLineModeForNonPsnText() {
        val titles = PsnParser.parse("Hades\nCeleste\nHollow Knight").map { it.title }
        assertEquals(listOf("Hades", "Celeste", "Hollow Knight"), titles)
    }

    @Test
    fun parserForRoutesPsnToThisParserAndOthersToGeneric() {
        assertSame(PsnParser, parserFor(Store.PSN))
        assertSame(GenericLineParser, parserFor(Store.STEAM))
    }
}

private val PSN_SAMPLE = """
Sony

Game Library

    Purchased
    Played
    Downloads
    PlayStation Plus

Purchased (1 - 24 of 175)

    My friend Peppa Pig
    PS4
    PS Plus
    My Friend Peppa Pig
    PS5
    PS Plus
    Nine Sols
    PS4
    PS Plus
    Nine Sols
    PS5
    PS Plus
    WUCHANG: Fallen Feathers
    PS5
    PS Plus
    EA SPORTS FC 26
    PS4
    PS Plus
    EA SPORTS FC 26
    PS5
    PS Plus
    SWORD ART ONLINE Fractured Daydream
    PS5
    PS Plus
    Tomb Raider I-III Remastered Starring Lara Croft
    PS4
    PS Plus
    Tomb Raider I-III Remastered Starring Lara Croft
    PS5
    PS Plus
    Lords of the Fallen
    PS5
    PS Plus
    The Crew® 2
    PS4
    PS Plus
    PAW Patrol: Grand Prix
    PS4
    PS Plus
    PAW Patrol: Grand Prix
    PS5
    PS Plus
    Test Drive Unlimited - Solar Crown
    PS5
    PS Plus
    Overpass 2
    PS5
    PS Plus
    Assetto Corsa Competizione
    PS4
    PS Plus
    Assetto Corsa Competizione
    PS5
    PS Plus
    LEGO® Horizon Adventures™
    PS5
    PS Plus
    Ghost of Tsushima
    PS4
    PS Plus
    Ghost of Tsushima
    PS5
    PS Plus
    Indiana Jones and the Staff of Kings
    PS4
    PS Plus
    Indiana Jones and the Staff of Kings
    PS5
    PS Plus
    CAPTAIN TSUBASA: RISE OF NEW CHAMPIONS
    PS4
    PS Plus

Filter
All
PS5
PS4
Country/Region: Croatia

    Support
    Privacy and Cookies
    Website Terms of Use
    Sitemap
    PlayStation Studios
    Legal
    About SIE

    PlayStation Terms of Service
    Software Usage Terms
    PS Store Cancellation Policy
    Health Warnings
    About Ratings

    Facebook
    X
    YouTube
    Instagram
    Android App
    iOS App

© 2026 Sony Interactive Entertainment Europe Ltd. All rights reserved.
"""
