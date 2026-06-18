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
    fun matchedAddCachesMetadataAndExternals() = runTest {
        val result = repository.addMatchedGame(
            igdb = sampleIgdb(igdbId = 7346L, name = "Breath of the Wild"),
            wishlist = false,
            status = Status.PLAYING,
            stores = setOf(Store.NINTENDO),
        )

        assertEquals(false, result.alreadyExisted)
        val game = dao.getGame(result.gameId)!!
        assertEquals(7346L, game.igdbId)
        assertEquals("a-slug", game.slug)
        assertEquals("Nintendo", game.developer)
        assertEquals(97.5, game.totalRating)
        assertEquals(1200, game.totalRatingCount)
        assertEquals(listOf(Platform("Nintendo Switch", "Switch")), game.platforms)
        assertEquals(listOf("BotW"), game.alternativeNames)
        assertEquals(Status.PLAYING, game.status)
        assertEquals(listOf(Store.NINTENDO), dao.ownershipsFor(result.gameId).map { it.store })

        val externals = dao.externalGamesFor(result.gameId)
        assertEquals("1234", externals.single().uid)
    }

    @Test
    fun matchedWishlistAddHasNoStatusNorStores() = runTest {
        val result = repository.addMatchedGame(sampleIgdb(igdbId = 1L), wishlist = true)

        val game = dao.getGame(result.gameId)!!
        assertTrue(game.wishlist)
        assertNull(game.status)
        assertTrue(dao.ownershipsFor(result.gameId).isEmpty())
        assertTrue(repository.ownedGames.first().isEmpty())
    }

    @Test
    fun matchedAddDedupsByIgdbIdWithoutDuplicating() = runTest {
        val first = repository.addMatchedGame(sampleIgdb(igdbId = 26226L), wishlist = false, stores = setOf(Store.STEAM))
        val second = repository.addMatchedGame(sampleIgdb(igdbId = 26226L), wishlist = false, stores = setOf(Store.GOG))

        assertEquals(false, first.alreadyExisted)
        assertTrue(second.alreadyExisted)
        assertEquals(first.gameId, second.gameId)
        assertEquals(1, repository.ownedGames.first().size)
        assertEquals(setOf(Store.STEAM, Store.GOG), dao.ownershipsFor(first.gameId).map { it.store }.toSet())
    }

    @Test
    fun matchedOwnItAttachesToAWishlistedGameClearingWishlist() = runTest {
        val wished = repository.addMatchedGame(sampleIgdb(igdbId = 5L), wishlist = true)
        val owned = repository.addMatchedGame(sampleIgdb(igdbId = 5L), wishlist = false, stores = setOf(Store.STEAM))

        assertTrue(owned.alreadyExisted)
        assertEquals(wished.gameId, owned.gameId)
        val game = dao.getGame(owned.gameId)!!
        assertEquals(false, game.wishlist)
        assertEquals(Status.BACKLOG, game.status)
        assertEquals(listOf(Store.STEAM), dao.ownershipsFor(owned.gameId).map { it.store })
    }

    @Test
    fun matchedWishlistPickOnOwnedGameIsANoOp() = runTest {
        val owned = repository.addMatchedGame(sampleIgdb(igdbId = 9L), wishlist = false, stores = setOf(Store.STEAM))
        val again = repository.addMatchedGame(sampleIgdb(igdbId = 9L), wishlist = true)

        assertTrue(again.alreadyExisted)
        assertEquals(owned.gameId, again.gameId)
        val game = dao.getGame(again.gameId)!!
        assertEquals(false, game.wishlist)
        assertEquals(Status.BACKLOG, game.status)
        assertEquals(listOf(Store.STEAM), dao.ownershipsFor(again.gameId).map { it.store })
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

    @Test
    fun migratesV2DatabasePreservingGamesAndDefaultingMetadata() = runTest {
        database.close()
        dbFile.delete()
        seedVersion2Database(dbFile)

        database = Room.databaseBuilder<GamesLibraryDatabase>(name = dbFile.absolutePath)
            .buildGamesLibraryDatabase()
        val migratedDao = database.gameDao()
        repository = GameRepository(migratedDao)

        val tile = repository.ownedGames.first().single()
        assertEquals("Existing Game", tile.game.name)
        assertEquals(42L, tile.game.igdbId)
        assertEquals(Status.PLAYING, tile.game.status)
        assertNull(tile.game.slug)
        assertNull(tile.game.coverImageId)
        assertTrue(tile.game.platforms.isEmpty())
        assertTrue(tile.game.alternativeNames.isEmpty())
        assertEquals(listOf(Store.STEAM), tile.ownerships.map { it.store })
        assertTrue(migratedDao.externalGamesFor(tile.game.id).isEmpty())
    }

    private fun sampleIgdb(igdbId: Long, name: String = "Sample Game"): IgdbGame = IgdbGame(
        igdbId = igdbId,
        name = name,
        slug = "a-slug",
        firstReleaseDate = 1488499200L,
        coverImageId = "co3p2d",
        developer = "Nintendo",
        totalRating = 97.5,
        totalRatingCount = 1200,
        platforms = listOf(Platform("Nintendo Switch", "Switch")),
        alternativeNames = listOf("BotW"),
        externalGames = listOf(ExternalRef(category = 1, uid = "1234", url = "https://store.example/1234")),
    )

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

    /** Writes a v2 schema database (Game with igdbId/wishlist/status + Ownership, user_version = 2). */
    private fun seedVersion2Database(file: File) {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `game` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `igdbId` INTEGER, `wishlist` INTEGER NOT NULL, `status` TEXT)",
            )
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_game_igdbId` ON `game` (`igdbId`)")
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `ownership` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`gameId` INTEGER NOT NULL, `store` TEXT NOT NULL, `source` TEXT NOT NULL, " +
                    "FOREIGN KEY(`gameId`) REFERENCES `game`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            connection.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_ownership_gameId_store` ON `ownership` (`gameId`, `store`)",
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_ownership_gameId` ON `ownership` (`gameId`)")
            connection.execSQL(
                "INSERT INTO `game` (`name`, `igdbId`, `wishlist`, `status`) " +
                    "VALUES ('Existing Game', 42, 0, 'PLAYING')",
            )
            connection.execSQL("INSERT INTO `ownership` (`gameId`, `store`, `source`) VALUES (1, 'STEAM', 'MANUAL')")
            connection.execSQL("PRAGMA user_version = 2")
        } finally {
            connection.close()
        }
    }
}
