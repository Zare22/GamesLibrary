package hr.kotwave.gameslibrary.importer

/**
 * Parses pasted Nintendo "Virtual Game Cards" text from a Nintendo Account. The list is one game per
 * line, bounded above by a `<N> results` count line (after the account/profile chrome) and below by
 * the first blank line (before the `Can't find specific software?` footer).
 *
 * Titles are the contiguous run of non-blank lines after that count; `®`/`™`/`©` are stripped and
 * case-insensitive duplicates collapse to their first occurrence. Edition and collection suffixes are
 * kept whole.
 *
 * Falls back to [GenericLineParser] when no count line is found (e.g. only the game lines were
 * copied), never crashing or dropping lines.
 */
object NintendoParser : StoreParser {

    private val resultsCount = Regex("^\\d+\\s+results?$", RegexOption.IGNORE_CASE)
    private val trademarks = Regex("[®™©]")
    private val whitespace = Regex("\\s+")

    override fun parse(raw: String): List<ParsedLine> {
        val lines = raw.lineSequence().map { it.trim() }.toList()

        val start = lines.indexOfFirst { resultsCount.matches(it) }
        if (start < 0) return GenericLineParser.parse(raw)

        var i = start + 1
        while (i < lines.size && lines[i].isEmpty()) i++

        val seen = HashSet<String>()
        val parsed = ArrayList<ParsedLine>()
        while (i < lines.size && lines[i].isNotEmpty()) {
            val line = lines[i]
            val title = clean(line)
            if (title.isNotEmpty() && seen.add(normalizeTitle(title))) {
                parsed.add(ParsedLine(title = title, raw = line))
            }
            i++
        }

        return if (parsed.isEmpty()) GenericLineParser.parse(raw) else parsed
    }

    private fun clean(title: String): String =
        title.replace(trademarks, "").replace(whitespace, " ").trim()
}
