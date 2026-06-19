package hr.kotwave.gameslibrary.data

import androidx.room.Room
import hr.kotwave.gameslibrary.transfer.ExportedGame
import hr.kotwave.gameslibrary.transfer.ExportedOwnership
import hr.kotwave.gameslibrary.transfer.LibraryImportDecision
import hr.kotwave.gameslibrary.transfer.LibraryTransfer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class GameRepositoryTransferTest {

    private lateinit var dbFile: File
    private lateinit var database: GamesLibraryDatabase
    private lateinit var dao: GameDao
    private lateinit var repository: GameRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("games_library_transfer_test", ".db").apply { delete() }
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
    fun exportThenImportIntoAFreshLibraryRoundTrips() = runTest {
        repository.addMatchedGame(sampleIgdb(1942L, "The Witcher 3"), wishlist = false, stores = setOf(Store.GOG))
        repository.addWishlistGame("Silksong", igdbId = 200L)
        repository.addOwnedGame("Manual Indie", stores = setOf(Store.PSN))

        val json = repository.exportLibrary()

        val targetFile = File.createTempFile("games_library_transfer_target", ".db").apply { delete() }
        val target = Room.databaseBuilder<GamesLibraryDatabase>(name = targetFile.absolutePath).buildGamesLibraryDatabase()
        try {
            val targetRepo = GameRepository(target.gameDao())
            val decoded = LibraryTransfer.decode(json)
            val summary = targetRepo.importLibrary(decoded.games.map { LibraryImportDecision(it) })

            assertEquals(3, summary.total)
            val witcher = target.gameDao().getGameByIgdbId(1942L)!!
            assertEquals("Some Studio", witcher.developer) // full metadata survived the round trip
            assertEquals(Store.GOG, target.gameDao().ownershipsFor(witcher.id).single().store)
            assertEquals(1, targetRepo.wishlistGames.first().count { it.name == "Silksong" })
            assertNotNull(targetRepo.ownedGames.first().singleOrNull { it.game.name == "Manual Indie" })
        } finally {
            target.close()
            targetFile.delete()
        }
    }

    @Test
    fun matchedRowDedupsByIgdbIdAndUnionsOwnership() = runTest {
        repository.addMatchedGame(sampleIgdb(5L), wishlist = false, stores = setOf(Store.STEAM))

        val summary = repository.importLibrary(
            listOf(LibraryImportDecision(exported(igdbId = 5L, stores = listOf("GOG")))),
        )

        assertEquals(ImportSummary(added = 0, attached = 1), summary)
        val game = dao.getGameByIgdbId(5L)!!
        assertEquals(setOf(Store.STEAM, Store.GOG), dao.ownershipsFor(game.id).map { it.store }.toSet())
    }

    @Test
    fun titleCollisionMergesWhenMergeByTitleSet() = runTest {
        repository.addOwnedGame("Indie", stores = setOf(Store.PSN)) // igdb-null

        val summary = repository.importLibrary(
            listOf(LibraryImportDecision(exported(name = "indie", stores = listOf("XBOX")), mergeByTitle = true)),
        )

        assertEquals(ImportSummary(added = 0, attached = 1), summary)
        val tile = repository.ownedGames.first().single()
        assertEquals(setOf(Store.PSN, Store.XBOX), tile.ownerships.map { it.store }.toSet())
    }

    @Test
    fun titleCollisionAddsSecondGameWhenMergeByTitleOff() = runTest {
        repository.addOwnedGame("Indie", stores = setOf(Store.PSN))

        val summary = repository.importLibrary(
            listOf(LibraryImportDecision(exported(name = "Indie", stores = listOf("XBOX")), mergeByTitle = false)),
        )

        assertEquals(ImportSummary(added = 1, attached = 0), summary)
        assertEquals(2, repository.ownedGames.first().count { it.game.name == "Indie" })
    }

    @Test
    fun importingAnOwnedCopyOfAWishlistGameClearsWishlist() = runTest {
        val wished = repository.addWishlistGame("Wished", igdbId = 8L)

        repository.importLibrary(
            listOf(LibraryImportDecision(exported(igdbId = 8L, wishlist = false, stores = listOf("GOG")))),
        )

        val game = dao.getGame(wished)!!
        assertFalse(game.wishlist)
        assertNotNull(game.status)
        assertEquals(Store.GOG, dao.ownershipsFor(game.id).single().store)
    }

    @Test
    fun newMatchedGameCarriesItsCachedMetadata() = runTest {
        repository.importLibrary(
            listOf(
                LibraryImportDecision(
                    ExportedGame(
                        name = "New",
                        igdbId = 99L,
                        developer = "Studio X",
                        coverImageId = "cov123",
                        platforms = listOf(Platform("PC (Microsoft Windows)", "PC")),
                        ownerships = listOf(ExportedOwnership("EPIC", "MANUAL")),
                    ),
                ),
            ),
        )

        val game = dao.getGameByIgdbId(99L)!!
        assertEquals("Studio X", game.developer)
        assertEquals("cov123", game.coverImageId)
        assertEquals(listOf(Platform("PC (Microsoft Windows)", "PC")), game.platforms)
    }

    @Test
    fun anOwnershipOnAnUnknownStoreIsDropped() = runTest {
        repository.importLibrary(
            listOf(LibraryImportDecision(exported(igdbId = 300L, stores = listOf("BATTLE_NET", "STEAM")))),
        )

        val game = dao.getGameByIgdbId(300L)!!
        assertEquals(listOf(Store.STEAM), dao.ownershipsFor(game.id).map { it.store })
    }

    @Test
    fun reImportingTheSameFileIsIdempotent() = runTest {
        val decisions = listOf(LibraryImportDecision(exported(igdbId = 5L, stores = listOf("GOG"))))
        repository.importLibrary(decisions)

        val summary = repository.importLibrary(decisions)

        assertEquals(ImportSummary(added = 0, attached = 1), summary)
        assertEquals(1, dao.ownershipsFor(dao.getGameByIgdbId(5L)!!.id).size) // (Game, Store) stays unique
    }

    private fun exported(
        name: String = "Sample Game",
        igdbId: Long? = null,
        wishlist: Boolean = false,
        stores: List<String> = emptyList(),
    ): ExportedGame = ExportedGame(
        name = name,
        igdbId = igdbId,
        wishlist = wishlist,
        ownerships = stores.map { ExportedOwnership(it, "MANUAL") },
    )

    private fun sampleIgdb(igdbId: Long, name: String = "Sample Game"): IgdbGame = IgdbGame(
        igdbId = igdbId,
        name = name,
        slug = "a-slug",
        firstReleaseDate = 1488499200L,
        coverImageId = "co3p2d",
        developer = "Some Studio",
        totalRating = 90.0,
        totalRatingCount = 100,
        platforms = listOf(Platform("PC (Microsoft Windows)", "PC")),
        alternativeNames = emptyList(),
        externalGames = emptyList(),
    )
}
