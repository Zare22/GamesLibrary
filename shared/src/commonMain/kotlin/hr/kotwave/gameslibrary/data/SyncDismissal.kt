package hr.kotwave.gameslibrary.data

import androidx.room.Entity

/**
 * A store uid the user dismissed from the sync Review ("don't ask again"): rows carrying it are
 * dropped from every later sync's needs-review tail. Keyed by the IGDB external-game (category, uid)
 * pair the syncs already dedup by; no Game row is created.
 */
@Entity(tableName = "sync_dismissal", primaryKeys = ["category", "uid"])
data class SyncDismissal(
    val category: Int,
    val uid: String,
)
