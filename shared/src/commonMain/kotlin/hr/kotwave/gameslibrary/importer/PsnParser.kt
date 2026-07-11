package hr.kotwave.gameslibrary.importer

/**
 * Parses pasted PSN "Game Library" text from the official PlayStation website. Each game is a block
 * of `<Title>` / platform tag (`PS4`/`PS5`/…) / optional `PS Plus` badge, wrapped in page chrome and
 * often repeated once per owned platform.
 *
 * A title is a line directly above a platform tag whose own following line is not another platform
 * tag. That selects every game block and rejects the footer's `All` / `PS5` / `PS4` filter — the one
 * piece of chrome shaped like a game — without matching any localized header text, so a whole library
 * pasted page-by-page parses in one go. Cross-platform duplicates collapse to one title (matched
 * case-insensitively); `®`/`™`/`©` are stripped.
 *
 * Falls back to [GenericLineParser] when no such block is found, never crashing or dropping lines.
 */
object PsnParser : StoreParser {

    private val platformTag = Regex("^PS\\s?(VR2?|Vita|[1-5])$|^PSP$", RegexOption.IGNORE_CASE)
    private val trademarks = Regex("[®™©]")
    private val whitespace = Regex("\\s+")

    /** Strips `®`/`™`/`©` and collapses whitespace runs. */
    internal fun cleanTitle(title: String): String =
        title.replace(trademarks, "").replace(whitespace, " ").trim()

    override fun parse(raw: String): List<ParsedLine> {
        val lines = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

        val seen = HashSet<String>()
        val parsed = ArrayList<ParsedLine>()
        for (i in lines.indices) {
            val line = lines[i]
            if (isPlatformTag(line)) continue
            if (!isPlatformTag(lines.getOrNull(i + 1))) continue
            if (isPlatformTag(lines.getOrNull(i + 2))) continue
            val title = cleanTitle(line)
            if (title.isNotEmpty() && seen.add(normalizeTitle(title))) {
                parsed.add(ParsedLine(title = title, raw = line))
            }
        }

        return if (parsed.isEmpty()) GenericLineParser.parse(raw) else parsed
    }

    private fun isPlatformTag(line: String?): Boolean = line != null && platformTag.matches(line)
}
