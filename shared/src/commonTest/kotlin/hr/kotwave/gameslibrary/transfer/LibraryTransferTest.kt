package hr.kotwave.gameslibrary.transfer

import hr.kotwave.gameslibrary.data.ExternalGame
import hr.kotwave.gameslibrary.data.Game
import hr.kotwave.gameslibrary.data.GameWithOwnerships
import hr.kotwave.gameslibrary.data.Ownership
import hr.kotwave.gameslibrary.data.Platform
import hr.kotwave.gameslibrary.data.Source
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryTransferTest {

    @Test
    fun encodeThenDecodeIsLossless() {
        val owned = GameWithOwnerships(
            game = Game(
                id = 7,
                name = "The Witcher 3",
                igdbId = 1942,
                wishlist = false,
                status = Status.PLAYING,
                userRating = 9.5,
                slug = "the-witcher-3",
                firstReleaseDate = 1431993600,
                coverImageId = "co1wyy",
                developer = "CD Projekt Red",
                totalRating = 93.0,
                totalRatingCount = 4200,
                platforms = listOf(Platform("PC (Microsoft Windows)", "PC"), Platform("PlayStation 4", "PS4")),
                alternativeNames = listOf("Wiedźmin 3"),
                orphaned = false,
            ),
            ownerships = listOf(
                Ownership(id = 1, gameId = 7, store = Store.GOG, source = Source.MANUAL),
                Ownership(id = 2, gameId = 7, store = Store.STEAM, source = Source.STEAM_SYNC),
            ),
        )
        val wished = GameWithOwnerships(
            game = Game(id = 8, name = "Hollow Knight: Silksong", igdbId = 200, wishlist = true, status = null),
            ownerships = emptyList(),
        )
        val externals = mapOf(7L to listOf(ExternalGame(id = 1, gameId = 7, category = 1, uid = "292030")))

        val decoded = LibraryTransfer.decode(LibraryTransfer.encode(listOf(owned, wished), externals))

        assertEquals(LIBRARY_EXPORT_VERSION, decoded.schemaVersion)
        assertEquals(2, decoded.games.size)
        val w3 = decoded.games.single { it.igdbId == 1942L }
        assertEquals("The Witcher 3", w3.name)
        assertEquals("PLAYING", w3.status)
        assertEquals(9.5, w3.userRating)
        assertEquals(listOf(Platform("PC (Microsoft Windows)", "PC"), Platform("PlayStation 4", "PS4")), w3.platforms)
        assertEquals(listOf("Wiedźmin 3"), w3.alternativeNames)
        assertEquals(
            setOf(ExportedOwnership("GOG", "MANUAL"), ExportedOwnership("STEAM", "STEAM_SYNC")),
            w3.ownerships.toSet(),
        )
        assertEquals(listOf(ExportedExternal(1, "292030")), w3.externals)
        val silksong = decoded.games.single { it.igdbId == 200L }
        assertTrue(silksong.wishlist)
        assertNull(silksong.status)
    }

    @Test
    fun decodeToleratesUnknownKeysAndUnknownEnumValues() {
        // A file written by a newer app: an extra top-level key and a Store this build doesn't know.
        val json = """
            {
              "schemaVersion": 2,
              "exportedAt": "2030-01-01",
              "games": [
                { "name": "From The Future", "ownerships": [ { "store": "FUTURE_STORE", "source": "MANUAL" } ] }
              ]
            }
        """.trimIndent()

        val decoded = LibraryTransfer.decode(json)

        assertEquals(2, decoded.schemaVersion)
        val game = decoded.games.single()
        assertEquals("FUTURE_STORE", game.ownerships.single().store)
        // The unknown Store is dropped when mapped to entities — never fatal.
        assertEquals(emptyList(), game.ownershipEntities(gameId = 1))
        assertNull(storeOrNull("FUTURE_STORE"))
        assertNull(statusOrNull("WHATEVER"))
        assertEquals(Source.MANUAL, sourceOrDefault("WHATEVER"))
    }

    @Test
    fun classifyAssignsEachRowItsKind() {
        val export = LibraryExport(
            games = listOf(
                ExportedGame(name = "Has Id, Present", igdbId = 1),
                ExportedGame(name = "Has Id, New", igdbId = 2),
                ExportedGame(name = "Indie", igdbId = null),
                ExportedGame(name = "Brand New Manual", igdbId = null),
            ),
        )

        val rows = classifyLibraryImport(
            export,
            existingIgdbIds = setOf(1L),
            existingTitlesLower = setOf("indie"),
        )

        assertEquals(
            listOf(
                ImportRowKind.AlreadyById,
                ImportRowKind.NewMatched,
                ImportRowKind.TitleCollision,
                ImportRowKind.NewManual,
            ),
            rows.map { it.kind },
        )
    }

    @Test
    fun addedAtRoundTripsThroughTheExportFormat() {
        val stamped = GameWithOwnerships(
            game = Game(id = 1, name = "Stamped", igdbId = 10, status = Status.BACKLOG, addedAt = 1_700_000_000_000),
            ownerships = emptyList(),
        )

        val decoded = LibraryTransfer.decode(LibraryTransfer.encode(listOf(stamped), emptyMap()))

        assertEquals(1_700_000_000_000, decoded.games.single().addedAt)
        assertEquals(1_700_000_000_000, decoded.games.single().toGame().addedAt)
    }

    @Test
    fun aFileWrittenBeforeAddedAtLoadsWithANullStamp() {
        val json = """{ "schemaVersion": 1, "games": [ { "name": "Old Row" } ] }"""

        val decoded = LibraryTransfer.decode(json)

        assertNull(decoded.games.single().addedAt)
        assertNull(decoded.games.single().toGame().addedAt)
    }

    @Test
    fun ownedGameWithNoKnownStatusDefaultsToBacklog() {
        assertEquals(Status.BACKLOG, ExportedGame(name = "X", igdbId = 1, wishlist = false, status = null).toGame().status)
        assertNull(ExportedGame(name = "Y", igdbId = 2, wishlist = true, status = null).toGame().status)
    }
}
