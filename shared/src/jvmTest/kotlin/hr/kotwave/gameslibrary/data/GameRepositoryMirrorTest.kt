package hr.kotwave.gameslibrary.data

import androidx.room.Room
import hr.kotwave.gameslibrary.data.db.GameDao
import hr.kotwave.gameslibrary.data.db.GamesLibraryDatabase
import hr.kotwave.gameslibrary.data.db.buildGamesLibraryDatabase
import hr.kotwave.gameslibrary.mirror.mirrorMerge
import hr.kotwave.gameslibrary.transfer.ExportedGame
import hr.kotwave.gameslibrary.transfer.ExportedOwnership
import hr.kotwave.gameslibrary.transfer.LibraryExport
import hr.kotwave.gameslibrary.transfer.LibraryTransfer
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameRepositoryMirrorTest {

    private lateinit var dbFile: File
    private lateinit var database: GamesLibraryDatabase
    private lateinit var dao: GameDao
    private lateinit var repository: GameRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("games_library_mirror_test", ".db").apply { delete() }
        database = Room.databaseBuilder<GamesLibraryDatabase>(name = dbFile.absolutePath).buildGamesLibraryDatabase()
        dao = database.gameDao()
        repository = GameRepository(dao)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun applyMirrorMergeAppliesEveryChangeAndPersistsTheBaselineAtomically() = runTest {
        repository.addOwnedGame("Updated", igdbId = 1L)
        repository.addOwnedGame("Deleted", igdbId = 2L)
        repository.addOwnedGame("Kept", igdbId = 3L)

        // Last Mirror converged at exactly the current library; the peer edited, deleted, and added since.
        val mine = LibraryTransfer.decode(repository.exportLibrary())
        val theirs = LibraryExport(
            games = mine.games
                .filter { it.igdbId != 2L }
                .map { if (it.igdbId == 1L) it.copy(status = "COMPLETED") else it } +
                ExportedGame(
                    name = "Added",
                    igdbId = 4L,
                    status = "BACKLOG",
                    addedAt = 123L,
                    ownerships = listOf(ExportedOwnership("EPIC", "MANUAL")),
                ),
        )
        val merge = mirrorMerge(
            baseline = mine,
            mine = mine,
            theirs = theirs,
            theirsDismissals = setOf(SyncDismissal(1, "777")),
        )
        assertTrue(merge.conflicts.isEmpty() && merge.collisions.isEmpty())
        val outcome = merge.resolve()
        assertEquals(listOf("Added"), outcome.mineChanges.adds.map { it.name })
        assertEquals(listOf("COMPLETED"), outcome.mineChanges.updates.map { it.status })
        assertEquals(listOf("Deleted"), outcome.mineChanges.deletes.map { it.name })

        repository.applyMirrorMerge("desktop", outcome)

        assertNull(dao.getGameByIgdbId(2L))
        assertEquals(Status.COMPLETED, dao.getGameByIgdbId(1L)?.status)
        assertNotNull(dao.getGameByIgdbId(3L))
        val added = dao.getGameByIgdbId(4L)!!
        assertEquals(123L, added.addedAt) // the incoming stamp travels; apply never re-stamps
        assertEquals(listOf(Store.EPIC), dao.ownershipsFor(added.id).map { it.store })
        assertEquals(listOf("777"), dao.dismissedSyncUids(1, listOf("777")))
        assertEquals(outcome.converged, repository.mirrorBaseline("desktop")?.games)
    }

    @Test
    fun theNextMirrorAgainstThePersistedBaselineIsSilent() = runTest {
        repository.addOwnedGame("Alpha", igdbId = 1L)
        val mine = LibraryTransfer.decode(repository.exportLibrary())
        repository.applyMirrorMerge("desktop", mirrorMerge(null, mine, mine).resolve())

        val next = mirrorMerge(
            baseline = repository.mirrorBaseline("desktop"),
            mine = LibraryTransfer.decode(repository.exportLibrary()),
            theirs = repository.mirrorBaseline("desktop")!!,
        )

        assertTrue(next.conflicts.isEmpty() && next.collisions.isEmpty())
        val outcome = next.resolve()
        assertTrue(outcome.mineChanges.adds.isEmpty() && outcome.mineChanges.updates.isEmpty())
        assertTrue(outcome.theirsChanges.adds.isEmpty() && outcome.theirsChanges.updates.isEmpty())
    }
}
