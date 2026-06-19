package hr.kotwave.gameslibrary.importer

/** One line a [StoreParser] pulled from pasted text: the [title] to match, and the [raw] line it came from. */
data class ParsedLine(val title: String, val raw: String)
