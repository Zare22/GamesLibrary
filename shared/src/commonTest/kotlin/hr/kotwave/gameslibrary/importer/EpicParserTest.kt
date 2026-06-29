package hr.kotwave.gameslibrary.importer

import hr.kotwave.gameslibrary.data.Store
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EpicParserTest {

    @Test
    fun parsesRealPasteToCleanedTitlesInOrder() {
        val titles = EpicParser.parse(EPIC_SAMPLE).map { it.title }
        assertEquals(
            listOf(
                "The Operator",
                "Cat Quest II",
                "World of Warships",
                "Mortal Shell",
                "STAR WARS Knights of the Old Republic II - The Sith Lords",
                "STAR WARS Knights of the Old Republic",
                "World War Z Aftermath",
                "Garden Story",
                "Bear and Breakfast",
                "Sniper Ghost Warrior Contracts",
            ),
            titles,
        )
    }

    @Test
    fun rejectsNavAndFooterChrome() {
        val titles = EpicParser.parse(EPIC_SAMPLE).map { it.title }
        val chrome = listOf(
            "Store", "Account", "Payment and rewards", "Parental controls",
            "Creator & developer tools", "Purchases", "Date", "Description", "Price",
            "Marketplace", "Epic Games Store", "Store logo", "Games", "Marketplaces", "Tools",
            "Online Services", "Company", "Resources", "Close", "Epic Games Logo",
        )
        for (line in chrome) {
            assertFalse(line in titles, "chrome leaked as a title: $line")
        }
        assertTrue(
            titles.none { it.contains("€") || it.contains("2025") || it.contains("reserved") },
            "a date/price/footer column leaked into a title",
        )
    }

    @Test
    fun stripsGluedPurchasedLabelFromTitleAndRaw() {
        val parsed = EpicParser.parse(EPIC_SAMPLE)
        assertTrue(parsed.none { it.title.startsWith("Purchased") }, "Purchased label leaked into a title")
        assertTrue(parsed.none { it.raw.startsWith("Purchased") }, "Purchased label leaked into raw")
    }

    @Test
    fun stripsTrademarksFromTitleButKeepsThemInRaw() {
        val parsed = EpicParser.parse(EPIC_SAMPLE)
        val kotor = parsed.single { it.title == "STAR WARS Knights of the Old Republic II - The Sith Lords" }
        assertEquals("STAR WARS™ Knights of the Old Republic™ II - The Sith Lords™", kotor.raw)
        assertTrue(parsed.none { "®" in it.title || "™" in it.title || "©" in it.title })
    }

    @Test
    fun keepsSubtitlesAndNumeralsWhole() {
        val titles = EpicParser.parse(EPIC_SAMPLE).map { it.title }
        assertTrue("Cat Quest II" in titles)
        assertTrue("STAR WARS Knights of the Old Republic II - The Sith Lords" in titles)
    }

    @Test
    fun dedupesAcrossARepeatedHeaderFromAPaginatedPaste() {
        val sample = buildString {
            appendLine("\tDate\tDescription\tPrice\tMarketplace")
            appendLine("\tJun 23, 2025\tPurchasedThe Operator \t- €0.00\tEpic Games Store")
            appendLine("\tApr 3, 2025\tPurchasedCat Quest II \t- €0.00\tEpic Games Store")
            appendLine("Store logo")
            appendLine("\tDate\tDescription\tPrice\tMarketplace")
            appendLine("\tFeb 1, 2025\tPurchasedCat Quest II \t- €0.00\tEpic Games Store")
            appendLine("\tFeb 2, 2025\tPurchasedMortal Shell \t- €0.00\tEpic Games Store")
        }
        assertEquals(
            listOf("The Operator", "Cat Quest II", "Mortal Shell"),
            EpicParser.parse(sample).map { it.title },
        )
    }

    @Test
    fun fallsBackToGenericLineModeWhenNoHeader() {
        val titles = EpicParser.parse("The Operator\nCat Quest II\nMortal Shell").map { it.title }
        assertEquals(listOf("The Operator", "Cat Quest II", "Mortal Shell"), titles)
    }

    @Test
    fun parserForRoutesEpicToThisParserAndOthersToGeneric() {
        assertSame(EpicParser, parserFor(Store.EPIC))
        assertSame(GenericLineParser, parserFor(Store.ITCH))
    }
}

// Real paste from epicgames.com → Account → Purchases. Tabs are written as `→` and restored below,
// because triple-quoted Kotlin doesn't process `\t` and literal tabs don't survive reformatting.
private val EPIC_SAMPLE = """

Store

Account
Payment and rewards
Parental controls
Creator & developer tools
Purchases

Your account payment details, transactions and earned Epic Rewards.
Any other real-money purchases associated with this purchase will be refunded. View the Epic Games Store Refund Policy. Epic Rewards FAQ
→Date→Description→Price→Marketplace
→Jun 23, 2025→PurchasedThe Operator →- €0.00→Epic Games Store
→Apr 3, 2025→PurchasedCat Quest II →- €0.00→Epic Games Store
→Mar 15, 2025→PurchasedWorld of Warships →- €0.00→Epic Games Store
→Mar 14, 2025→PurchasedMortal Shell →- €0.00→Epic Games Store
→Feb 28, 2025→PurchasedSTAR WARS™ Knights of the Old Republic™ II - The Sith Lords™ →- €0.00→Epic Games Store
→Feb 28, 2025→PurchasedSTAR WARS™ Knights of the Old Republic™ →- €0.00→Epic Games Store
→Feb 22, 2025→PurchasedWorld War Z Aftermath →- €0.00→Epic Games Store
→Feb 22, 2025→PurchasedGarden Story →- €0.00→Epic Games Store
→Oct 4, 2024→PurchasedBear and Breakfast →- €0.00→Epic Games Store
→Sep 8, 2024→PurchasedSniper Ghost Warrior Contracts →- €0.00→Epic Games Store
Store logo

Games

Marketplaces

Tools

Online Services

Company

Resources

© 2026 Epic Games, Inc. All rights reserved. Epic, Epic Games, the Epic Games logo, Fortnite, the Fortnite logo, Unreal, Unreal Engine, the Unreal Engine logo, Unreal Tournament, and the Unreal Tournament logo are trademarks or registered trademarks of Epic Games, Inc. in the United States of America and elsewhere. Other brands or product names are the trademarks of their respective owners. Our websites may contain links to other sites and resources provided by third parties. These links are provided for your convenience only. Epic Games has no control over the contents of those sites or resources, and accepts no responsibility for them or for any loss or damage that may arise from your use of them.

Close
Epic Games Logo

""".replace('→', '\t')
