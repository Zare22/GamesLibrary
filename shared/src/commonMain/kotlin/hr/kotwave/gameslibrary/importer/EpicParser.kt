package hr.kotwave.gameslibrary.importer

/**
 * Parses pasted Epic text from the epicgames.com account "Purchases" page (Account → Purchases): a
 * tab-delimited transactions table with a `Date / Description / Price / Marketplace` header row above
 * one row per transaction, wrapped in store-nav and footer chrome that carries no tabs.
 *
 * The header row is the anchor and fixes the column layout; every later line with the same column
 * count is a transaction, its title taken from the `Description` column with the glued `Purchased`
 * action label removed (`PurchasedThe Operator` → `The Operator`). Tab-free chrome can't match the
 * layout, so the table parses wherever it sits and across a repeated header (paginated paste).
 * `®`/`™`/`©` are stripped from the title and case-insensitive duplicates collapse to their first
 * occurrence; edition suffixes and non-base-game rows (refunds, DLC) are kept whole for the import
 * Review to sort out.
 *
 * Falls back to [GenericLineParser] when no header row is found, never crashing or dropping lines.
 */
object EpicParser : StoreParser {

    private val headerLabels = setOf("Date", "Description", "Price", "Marketplace")
    private const val DESCRIPTION = "Description"
    private const val actionLabel = "Purchased"
    private val trademarks = Regex("[®™©]")
    private val whitespace = Regex("\\s+")

    override fun parse(raw: String): List<ParsedLine> {
        val lines = raw.lineSequence().toList()

        val headerIndex =
            lines.indexOfFirst { columns(it).mapTo(HashSet()) { c -> c.trim() }.containsAll(headerLabels) }
        if (headerIndex < 0) return GenericLineParser.parse(raw)

        val header = columns(lines[headerIndex])
        val columnCount = header.size
        val descriptionColumn = header.indexOfFirst { it.trim() == DESCRIPTION }

        val seen = HashSet<String>()
        val parsed = ArrayList<ParsedLine>()
        for (i in (headerIndex + 1) until lines.size) {
            val cols = columns(lines[i])
            if (cols.size != columnCount) continue
            val description = cols[descriptionColumn].trim()
            if (description.isEmpty() || description == DESCRIPTION) continue
            val name = description.removePrefix(actionLabel).trim()
            val title = clean(name)
            if (title.isNotEmpty() && seen.add(normalizeTitle(title))) {
                parsed.add(ParsedLine(title = title, raw = name))
            }
        }

        return if (parsed.isEmpty()) GenericLineParser.parse(raw) else parsed
    }

    private fun columns(line: String): List<String> = line.split('\t')

    private fun clean(title: String): String =
        title.replace(trademarks, "").replace(whitespace, " ").trim()
}
