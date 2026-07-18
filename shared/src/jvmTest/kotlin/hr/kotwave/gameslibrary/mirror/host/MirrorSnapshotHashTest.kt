package hr.kotwave.gameslibrary.mirror.host

import hr.kotwave.gameslibrary.transfer.ExportedExternal
import hr.kotwave.gameslibrary.transfer.ExportedGame
import hr.kotwave.gameslibrary.transfer.ExportedOwnership
import hr.kotwave.gameslibrary.transfer.LibraryExport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MirrorSnapshotHashTest {

    private val gameA = ExportedGame(
        name = "Game A",
        igdbId = 1,
        status = "BACKLOG",
        ownerships = listOf(ExportedOwnership("STEAM", "STEAM_SYNC"), ExportedOwnership("GOG", "MANUAL")),
        externals = listOf(ExportedExternal(1, "570"), ExportedExternal(5, "9")),
    )
    private val gameB = ExportedGame(name = "Game B", igdbId = 2, status = "PLAYING")

    @Test
    fun hashIsIndependentOfRowAndSetOrder() {
        val ordered = LibraryExport(games = listOf(gameA, gameB))
        val shuffled = LibraryExport(
            games = listOf(
                gameB,
                gameA.copy(
                    ownerships = gameA.ownerships.reversed(),
                    externals = gameA.externals.reversed(),
                ),
            ),
        )

        assertEquals(snapshotContentHash(ordered), snapshotContentHash(shuffled))
    }

    @Test
    fun contentChangeChangesTheHash() {
        val before = LibraryExport(games = listOf(gameA, gameB))
        val after = LibraryExport(games = listOf(gameA, gameB.copy(status = "COMPLETED")))

        assertNotEquals(snapshotContentHash(before), snapshotContentHash(after))
    }

    @Test
    fun emptySnapshotsHashEqually() {
        assertEquals(snapshotContentHash(LibraryExport()), snapshotContentHash(LibraryExport()))
    }
}
