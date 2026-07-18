package hr.kotwave.gameslibrary.mirror

import hr.kotwave.gameslibrary.data.GameRepository
import hr.kotwave.gameslibrary.data.SyncDismissal
import hr.kotwave.gameslibrary.transfer.LibraryExport

/** The local-library seams a Mirror end (host or client) reads and applies through. */
interface MirrorLocalStore {
    suspend fun snapshot(): LibraryExport
    suspend fun dismissals(): List<SyncDismissal>
    suspend fun baseline(pairingId: String): LibraryExport?
    suspend fun apply(pairingId: String, outcome: MirrorOutcome)
}

/** [MirrorLocalStore] over the real [GameRepository]. */
class RepositoryMirrorStore(private val repository: GameRepository) : MirrorLocalStore {
    override suspend fun snapshot(): LibraryExport = repository.exportSnapshot()
    override suspend fun dismissals(): List<SyncDismissal> = repository.syncDismissals()
    override suspend fun baseline(pairingId: String): LibraryExport? = repository.mirrorBaseline(pairingId)
    override suspend fun apply(pairingId: String, outcome: MirrorOutcome) =
        repository.applyMirrorMerge(pairingId, outcome)
}
