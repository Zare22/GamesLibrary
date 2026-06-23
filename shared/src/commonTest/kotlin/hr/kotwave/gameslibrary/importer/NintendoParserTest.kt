package hr.kotwave.gameslibrary.importer

import hr.kotwave.gameslibrary.data.Store
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NintendoParserTest {

    @Test
    fun parsesRealPasteToUniqueCleanedTitlesInOrder() {
        val titles = NintendoParser.parse(NINTENDO_SAMPLE).map { it.title }
        assertEquals(101, titles.size, "102 listed games minus one duplicate")
        assertEquals("FINAL FANTASY VII REBIRTH", titles.first())
        assertEquals("Hogwarts Legacy", titles[1])
        assertEquals("Shovel Knight: Treasure Trove", titles.last())
    }

    @Test
    fun rejectsHeaderAndFooterChrome() {
        val titles = NintendoParser.parse(NINTENDO_SAMPLE).map { it.title }
        val chrome = listOf(
            "Nintendo Account", "Nucl3r127", "User profile image", "Virtual Game Cards", "102 results",
            "Can't find specific software?", "View Hidden Virtual Game Cards", "Support",
            "Nintendo Account Agreement", "Nintendo website", "© Nintendo",
            "If you have the physical game card for a game, that game won't be displayed here.",
        )
        for (line in chrome) {
            assertFalse(line in titles, "chrome leaked as a title: $line")
        }
    }

    @Test
    fun collapsesDuplicateTitles() {
        val titles = NintendoParser.parse(NINTENDO_SAMPLE).map { it.title }
        assertEquals(1, titles.count { it == "Hogwarts Legacy" })
    }

    @Test
    fun stripsTrademarkSymbolsButKeepsRawLine() {
        val parsed = NintendoParser.parse(NINTENDO_SAMPLE)
        val lego = parsed.single { it.title == "LEGO Harry Potter Collection" }
        assertEquals("LEGO® Harry Potter™ Collection", lego.raw)
        assertTrue(parsed.none { "®" in it.title || "™" in it.title || "©" in it.title })
    }

    @Test
    fun keepsEditionAndCollectionSuffixes() {
        val titles = NintendoParser.parse(NINTENDO_SAMPLE).map { it.title }
        assertTrue("Xenoblade Chronicles: Definitive Edition" in titles)
        assertTrue("SteamWorld Heist: Ultimate Edition" in titles)
        assertTrue("Street Fighter 30th Anniversary Collection" in titles)
        assertTrue("Asterix & Obelix XXXL - The Ram From Hibernia" in titles)
    }

    @Test
    fun stopsAtBlankLineBeforeFooter() {
        val sample = """
            12 results

                Hades
                Celeste

            Can't find specific software?

                Support
        """.trimIndent()
        assertEquals(listOf("Hades", "Celeste"), NintendoParser.parse(sample).map { it.title })
    }

    @Test
    fun fallsBackToGenericLineModeWhenNoResultsCount() {
        val titles = NintendoParser.parse("Hades\nCeleste\nHollow Knight").map { it.title }
        assertEquals(listOf("Hades", "Celeste", "Hollow Knight"), titles)
    }

    @Test
    fun parserForRoutesNintendoToThisParserAndOthersToGeneric() {
        assertSame(NintendoParser, parserFor(Store.NINTENDO))
        assertSame(GenericLineParser, parserFor(Store.STEAM))
    }
}

private val NINTENDO_SAMPLE = """


Nintendo Account

Nucl3r127
User profile image
Virtual Game Cards
102 results

    FINAL FANTASY VII REBIRTH
    Hogwarts Legacy
    FINAL FANTASY VII REMAKE INTERGRADE
    Mario Kart World
    LEGO® Harry Potter™ Collection
    Hogwarts Legacy
    Paper Mario: The Thousand-Year Door
    Super Mario RPG
    The Legend of Zelda: Echoes of Wisdom
    LEGO® The Incredibles
    Asterix & Obelix XXXL - The Ram From Hibernia
    Asterix & Obelix XXL3: The Crystal Menhir
    Asterix & Obelix XXL
    Storyteller
    Tony Hawk’s™ Pro Skater™ 1 + 2
    Kirby and the Forgotten Land
    Metroid Prime Remastered
    Super Mario Bros. Wonder
    Mario + Rabbids® Sparks of Hope
    Xenoblade Chronicles 3
    The Legend of Zelda: Tears of the Kingdom
    The LEGO® NINJAGO® Movie Video Game
    Super Mario 3D World + Bowser's Fury
    LAYTON’S MYSTERY JOURNEY™: Katrielle and the Millionaires’ Conspiracy – Deluxe Edition
    Pikuniku
    LEGO® Star Wars™: The Skywalker Saga
    CHRONO CROSS: THE RADICAL DREAMERS EDITION
    Mario Golf: Super Rush
    Agent A: A puzzle in disguise
    LEGO® CITY UNDERCOVER
    Xenoblade Chronicles: Definitive Edition
    Ori and the Will of the Wisps
    Syberia 1 & 2
    American Fugitive
    Speed Limit
    Among Us
    Hades
    The Legend of Zelda: Link's Awakening
    UNO
    Super Mario 3D All-Stars
    Asphalt Legends
    Luigi's Mansion 3
    STAR WARS Episode I: Racer
    Trine 2
    DRAGON QUEST XI S: Echoes of an Elusive Age – Definitive Edition
    LEGO® DC Super-Villains
    Divinity Original Sin 2
    Ori and the Blind Forest: Definitive Edition
    Pokémon Sword
    LOST SPHEAR
    COLLECTION of MANA
    Super Smash Bros. Ultimate
    Street Fighter® 30th Anniversary Collection
    Mega Man X Legacy Collection 2
    Rayman Legends: Definitive Edition
    Mega Man X Legacy Collection
    Asterix & Obelix XXL 2
    Yooka-Laylee
    Castlevania Anniversary Collection
    SteamWorld Heist: Ultimate Edition
    Yoshi's Crafted World
    Crash™ Team Racing Nitro-Fueled
    Geki Yaba Runner Anniversary Edition
    Hyrule Warriors: Definitive Edition
    Sudoku Relax
    The Elder Scrolls V: Skyrim
    L.A. Noire
    I am Setsuna.
    Unravel TWO
    JUST DANCE® 2019
    The Way Remastered
    New Super Mario Bros. U Deluxe
    Hollow Knight
    Pokémon: Let's Go, Pikachu!
    Super Mario Party
    FINAL FANTASY XV POCKET EDITION HD
    Captain Toad: Treasure Tracker
    OCTOPATH TRAVELER
    Mario Tennis Aces
    Xenoblade Chronicles 2
    Splatoon 2
    Celeste
    Astro Bears Party
    FIFA 18
    OCTOPATH TRAVELER Prologue Demo
    Fortnite for Nintendo Switch
    Donkey Kong Country: Tropical Freeze
    Snake Pass
    Metropolis: Lux Obscura
    Kirby Star Allies
    Arcade Archives VS. SUPER MARIO BROS.
    30-in-1 Game Collection: Volume 1
    Scribblenauts Showdown
    Puyo Puyo™ Tetris®
    Golf Story
    Oxenfree
    Mario + Rabbids Kingdom Battle
    NBA Playgrounds
    Stardew Valley
    ARMS
    The Legend of Zelda: Breath of the Wild
    Shovel Knight: Treasure Trove

Can't find specific software?

    If you have the physical game card for a game, that game won't be displayed here.
    You won't get a virtual game card for some games, including Game Trials software, so those games won't be displayed.
    These games can be downloaded from Nintendo eShop and played without a virtual game card.
    Games purchased by other users won't be displayed.
    If you choose to hide a virtual game card, it will no longer be displayed.

View Hidden Virtual Game Cards

    Support
    Nintendo Account Agreement
    Nintendo Account Privacy Policy
    Contact and Website Privacy Policy
    Nintendo website

© Nintendo
"""
