package hr.kotwave.gameslibrary.importer

import hr.kotwave.gameslibrary.data.Store

/** Turns one Store's pasted text into [ParsedLine]s. Pure text→lines: no IGDB, no DB. */
fun interface StoreParser {
    fun parse(raw: String): List<ParsedLine>
}

/**
 * The fallback parser: one line is one title. Trims each line, drops blanks, and collapses
 * case-insensitive duplicate titles to their first occurrence (a list with a title twice means one
 * Game). Used until a Store gets its own parser.
 */
object GenericLineParser : StoreParser {
    override fun parse(raw: String): List<ParsedLine> {
        val seen = HashSet<String>()
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { seen.add(normalizeTitle(it)) }
            .map { ParsedLine(title = it, raw = it) }
            .toList()
    }
}

/** The parser for a [store]. A Store falls back to [GenericLineParser] until it gets its own. */
fun parserFor(store: Store): StoreParser = when (store) {
    Store.PSN -> PsnParser
    Store.NINTENDO -> NintendoParser
    Store.EPIC -> EpicParser
    else -> GenericLineParser
}
