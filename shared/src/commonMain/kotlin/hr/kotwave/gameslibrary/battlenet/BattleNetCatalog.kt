package hr.kotwave.gameslibrary.battlenet

/** A franchise grouping of catalog [titles], for the picker's section headers. */
data class BattleNetCatalogSection(val label: String, val titles: List<String>)

/**
 * The fixed catalog the Battle.net picker shows. Battle.net has no pasteable or syncable owned-games
 * source, so the user ticks owned titles from this curated list; each ticked title flows through the
 * same IGDB match → Review → confirm path as a paste Import (Store.BATTLE_NET, additive). Plain title
 * strings — IGDB resolution happens at confirm, and Review disambiguates anything inexact.
 */
object BattleNetCatalog {
    val sections: List<BattleNetCatalogSection> = listOf(
        BattleNetCatalogSection(
            "Diablo",
            listOf("Diablo II: Resurrected", "Diablo III", "Diablo IV", "Diablo Immortal"),
        ),
        BattleNetCatalogSection(
            "Warcraft",
            listOf(
                "World of Warcraft",
                "Warcraft I: Remastered",
                "Warcraft II: Remastered",
                "Warcraft III: Reforged",
            ),
        ),
        BattleNetCatalogSection(
            "StarCraft",
            listOf("StarCraft: Remastered", "StarCraft II"),
        ),
        BattleNetCatalogSection(
            "Overwatch",
            listOf("Overwatch 2"),
        ),
        BattleNetCatalogSection(
            "Other Blizzard",
            listOf("Hearthstone", "Heroes of the Storm"),
        ),
        BattleNetCatalogSection(
            "Call of Duty",
            listOf(
                "Call of Duty: Modern Warfare",
                "Call of Duty: Black Ops Cold War",
                "Call of Duty: Vanguard",
                "Call of Duty: Modern Warfare II",
                "Call of Duty: Modern Warfare III",
                "Call of Duty: Black Ops 6",
                "Call of Duty: Warzone",
            ),
        ),
        BattleNetCatalogSection(
            "Crash Bandicoot",
            listOf("Crash Bandicoot 4: It's About Time"),
        ),
    )

    /** Every catalog title, in section order. */
    val titles: List<String> = sections.flatMap { it.titles }
}
