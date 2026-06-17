package hr.kotwave.gameslibrary.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameRepositoryTest {

    private lateinit var dbFile: File
    private lateinit var database: GamesLibraryDatabase
    private lateinit var dao: GameDao
    private lateinit var repository: GameRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("games_library_test", ".db").apply { delete() }
        database = Room.databaseBuilder<GamesLibraryDatabase>(name = dbFile.absolutePath)
            .buildGamesLibraryDatabase()
        dao = database.gameDao()
        repository = GameRepository(dao)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun ownedGameHasStatusAndItsStores() = runTest {
        val id = repository.addOwnedGame(
            name = "Baldur's Gate 3",
            status = Status.PLAYING,
            stores = setOf(Store.STEAM, Store.GOG),
        )

        val tile = repository.ownedGames.first().single()
        assertEquals(id, tile.game.id)
        assertEquals(false, tile.game.wishlist)
        assertEquals(Status.PLAYING, tile.game.status)
        assertEquals(setOf(Store.STEAM, Store.GOG), tile.ownerships.map { it.store }.toSet())
    }

    @Test
    fun ownedGameWithNoStoresIsAllowed() = runTest {
        repository.addOwnedGame(name = "An old boxed copy", status = Status.COMPLETED)

        val tile = repository.ownedGames.first().single()
        assertTrue(tile.ownerships.isEmpty())
        assertEquals(Status.COMPLETED, tile.game.status)
    }

    @Test
    fun wishlistGameHasNoStatusNoOwnershipAndIsNotInOwned() = runTest {
        val id = repository.addWishlistGame(name = "Hollow Knight: Silksong")

        assertTrue(repository.ownedGames.first().isEmpty())
        val game = dao.getGame(id)!!
        assertTrue(game.wishlist)
        assertNull(game.status)
        assertTrue(dao.ownershipsFor(id).isEmpty())
    }

    @Test
    fun ownershipIsUniquePerGameAndStore() = runTest {
        val id = repository.addOwnedGame(name = "Stardew Valley", stores = setOf(Store.STEAM))
        repository.addOwnership(id, Store.STEAM)

        assertEquals(1, dao.ownershipsFor(id).size)
    }

    @Test
    fun addingOwnershipToWishlistedGameClearsWishlist() = runTest {
        val id = repository.addWishlistGame(name = "Silksong")

        repository.addOwnership(id, Store.STEAM)

        val game = dao.getGame(id)!!
        assertEquals(false, game.wishlist)
        assertEquals(Status.BACKLOG, game.status)
        assertEquals(listOf(Store.STEAM), dao.ownershipsFor(id).map { it.store })
    }

    @Test
    fun igdbIdIsUniqueWhenPresent() = runTest {
        repository.addOwnedGame(name = "Celeste", igdbId = 26226L)

        assertFails {
            repository.addOwnedGame(name = "Celeste (re-add)", igdbId = 26226L)
        }
    }

    @Test
    fun manualGamesShareTitlesWithNullIgdbId() = runTest {
        repository.addOwnedGame(name = "Untitled Game", igdbId = null)
        repository.addOwnedGame(name = "Untitled Game", igdbId = null)

        assertEquals(2, repository.ownedGames.first().size)
    }

    @Test
    fun similarTitlesMatchesCaseInsensitively() = runTest {
        repository.addOwnedGame(name = "DOOM")

        assertEquals(1, repository.similarTitles("doom").size)
        assertTrue(repository.similarTitles("Quake").isEmpty())
    }

    @Test
    fun migratesV1DatabasePreservingGames() = runTest {
        database.close()
        dbFile.delete()
        seedVersion1Database(dbFile)

        database = Room.databaseBuilder<GamesLibraryDatabase>(name = dbFile.absolutePath)
            .buildGamesLibraryDatabase()
        repository = GameRepository(database.gameDao())

        val tile = repository.ownedGames.first().single()
        assertEquals("Legacy Game", tile.game.name)
        assertEquals(Status.BACKLOG, tile.game.status)
        assertEquals(false, tile.game.wishlist)
        assertTrue(tile.ownerships.isEmpty())
    }

    /** Writes a v1 schema database (thin `game(id, name)`, user_version = 1) with one row. */
    private fun seedVersion1Database(file: File) {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `game` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)",
            )
            connection.execSQL("INSERT INTO `game` (`name`) VALUES ('Legacy Game')")
            connection.execSQL("PRAGMA user_version = 1")
        } finally {
            connection.close()
        }
    }
}
