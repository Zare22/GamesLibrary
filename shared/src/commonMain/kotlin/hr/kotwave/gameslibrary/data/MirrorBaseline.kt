package hr.kotwave.gameslibrary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The converged library snapshot both devices persisted after the last Mirror with this pairing,
 * in the export file format. Diffing current-vs-Baseline attributes every later change (ADR 0038).
 */
@Entity(tableName = "mirror_baseline")
data class MirrorBaseline(
    @PrimaryKey val pairingId: String,
    val snapshot: String,
)

/** One converged Mirror row ready to write: the Game plus its full Ownership and external sets. */
data class MirrorGameWrite(
    val game: Game,
    val ownerships: List<Ownership>,
    val externals: List<ExternalGame>,
)
