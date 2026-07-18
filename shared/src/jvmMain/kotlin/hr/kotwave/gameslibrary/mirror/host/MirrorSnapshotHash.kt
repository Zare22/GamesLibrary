package hr.kotwave.gameslibrary.mirror.host

import hr.kotwave.gameslibrary.transfer.ExportedGame
import hr.kotwave.gameslibrary.transfer.LibraryExport
import kotlinx.serialization.json.Json

private val HashJson = Json { encodeDefaults = true }

/**
 * Order-independent content hash of a snapshot: every row compact-encoded with its Ownership and
 * external sets sorted, rows sorted, SHA-256 over the whole. The push's hash guard compares this
 * against the pull-time value.
 */
internal fun snapshotContentHash(export: LibraryExport): String {
    val rows = export.games
        .map { row ->
            HashJson.encodeToString(
                ExportedGame.serializer(),
                row.copy(
                    ownerships = row.ownerships.sortedWith(compareBy({ it.store }, { it.source })),
                    externals = row.externals.sortedWith(compareBy({ it.category }, { it.uid })),
                ),
            )
        }
        .sorted()
    return sha256Hex(rows.joinToString("\n"))
}
