package hr.kotwave.gameslibrary.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import hr.kotwave.gameslibrary.data.db.GameDao
import hr.kotwave.gameslibrary.data.db.GamesLibraryDatabase
import hr.kotwave.gameslibrary.data.db.buildGamesLibraryDatabase
import hr.kotwave.gameslibrary.data.sync.RematchResult
import hr.kotwave.gameslibrary.data.sync.SyncEntry
import hr.kotwave.gameslibrary.data.sync.SyncSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant
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

    @Test
    fun setUserRatingStoresAndClears() = runTest {
        val id = repository.addOwnedGame(name = "Hades")

        repository.setUserRating(id, 8.5)
        assertEquals(8.5, dao.getGame(id)!!.userRating)

        repository.setUserRating(id, null)
        assertNull(dao.getGame(id)!!.userRating)
    }

    @Test
    fun setStatusUpdatesOwnedGameButIsNoOpOnWishlisted() = runTest {
        val owned = repository.addOwnedGame(name = "Celeste", status = Status.BACKLOG)
        repository.setStatus(owned, Status.COMPLETED)
        assertEquals(Status.COMPLETED, dao.getGame(owned)!!.status)

        val wished = repository.addWishlistGame(name = "Silksong")
        repository.setStatus(wished, Status.PLAYING)
        assertNull(dao.getGame(wished)!!.status)
    }

    @Test
    fun removeOwnershipLeavesGameOwnedNotWishlisted() = runTest {
        val id = repository.addOwnedGame(
            name = "Stardew Valley",
            status = Status.PLAYING,
            stores = setOf(Store.STEAM, Store.GOG),
        )

        repository.removeOwnership(id, Store.STEAM)
        assertEquals(listOf(Store.GOG), dao.ownershipsFor(id).map { it.store })

        repository.removeOwnership(id, Store.GOG)
        val game = dao.getGame(id)!!
        assertTrue(dao.ownershipsFor(id).isEmpty())
        assertEquals(false, game.wishlist)
        assertEquals(Status.PLAYING, game.status)
    }

    @Test
    fun deleteGameCascadesOwnershipsAndExternals() = runTest {
        val id = repository.addMatchedGame(
            sampleIgdb(igdbId = 100L),
            wishlist = false,
            stores = setOf(Store.STEAM),
        ).gameId
        assertTrue(dao.ownershipsFor(id).isNotEmpty())
        assertTrue(dao.externalGamesFor(id).isNotEmpty())

        repository.deleteGame(id)

        assertNull(dao.getGame(id))
        assertTrue(dao.ownershipsFor(id).isEmpty())
        assertTrue(dao.externalGamesFor(id).isEmpty())
    }

    @Test
    fun applyRefreshOverwritesIgdbFieldsKeepingLocalState() = runTest {
        val added = repository.addMatchedGame(
            sampleIgdb(igdbId = 7346L, name = "Old Name"),
            wishlist = false,
            status = Status.PLAYING,
            stores = setOf(Store.STEAM),
        )
        repository.setUserRating(added.gameId, 9.0)

        repository.applyRefresh(
            added.gameId,
            IgdbGame(
                igdbId = 7346L,
                name = "New Name",
                slug = "new-slug",
                developer = "New Dev",
                totalRating = 80.0,
                totalRatingCount = 50,
                platforms = listOf(Platform("PC", "PC")),
                alternativeNames = listOf("NN"),
                externalGames = listOf(ExternalRef(category = 5, uid = "9999")),
            ),
        )

        val game = dao.getGame(added.gameId)!!
        assertEquals("New Name", game.name)
        assertEquals("new-slug", game.slug)
        assertEquals("New Dev", game.developer)
        assertEquals(80.0, game.totalRating)
        assertEquals(listOf(Platform("PC", "PC")), game.platforms)
        assertEquals(false, game.orphaned)
        // Local state is never touched by a refresh.
        assertEquals(9.0, game.userRating)
        assertEquals(Status.PLAYING, game.status)
        assertEquals(listOf(Store.STEAM), dao.ownershipsFor(added.gameId).map { it.store })
        // External refs are replaced, not appended.
        assertEquals(listOf("9999"), dao.externalGamesFor(added.gameId).map { it.uid })
    }

    @Test
    fun applyRefreshWithNullFetchMarksOrphanedKeepingMetadata() = runTest {
        val added = repository.addMatchedGame(
            sampleIgdb(igdbId = 26226L, name = "Celeste"),
            wishlist = false,
            stores = setOf(Store.STEAM),
        )

        repository.applyRefresh(added.gameId, null)

        val game = dao.getGame(added.gameId)!!
        assertTrue(game.orphaned)
        assertEquals("Celeste", game.name)
        assertEquals("a-slug", game.slug)
        assertEquals(listOf(Store.STEAM), dao.ownershipsFor(added.gameId).map { it.store })
    }

    @Test
    fun applyRematchRepointsMetadataAndClearsOrphaned() = runTest {
        val added = repository.addMatchedGame(
            sampleIgdb(igdbId = 1L, name = "Wrong Match"),
            wishlist = false,
            stores = setOf(Store.STEAM),
        )
        repository.setUserRating(added.gameId, 7.0)
        repository.applyRefresh(added.gameId, null)

        val outcome = repository.applyRematch(added.gameId, sampleIgdb(igdbId = 2L, name = "Right Match"))

        assertEquals(RematchResult.Success, outcome)
        val game = dao.getGame(added.gameId)!!
        assertEquals(2L, game.igdbId)
        assertEquals("Right Match", game.name)
        assertEquals(false, game.orphaned)
        assertEquals(7.0, game.userRating)
        assertEquals(listOf(Store.STEAM), dao.ownershipsFor(added.gameId).map { it.store })
    }

    @Test
    fun applyRematchBlocksWhenIgdbIdAlreadyInLibrary() = runTest {
        val keep = repository.addMatchedGame(sampleIgdb(igdbId = 50L), wishlist = false, stores = setOf(Store.GOG))
        val orphan = repository.addMatchedGame(sampleIgdb(igdbId = 51L), wishlist = false, stores = setOf(Store.STEAM))
        repository.applyRefresh(orphan.gameId, null)

        val outcome = repository.applyRematch(orphan.gameId, sampleIgdb(igdbId = 50L))

        assertEquals(RematchResult.AlreadyInLibrary(keep.gameId), outcome)
        val game = dao.getGame(orphan.gameId)!!
        assertEquals(51L, game.igdbId)
        assertTrue(game.orphaned)
    }

    @Test
    fun steamSyncAddsMatchedAndUnmatchedGames() = runTest {
        val summary = repository.syncStore(Store.STEAM,
            listOf(
                SyncEntry.Matched(sampleIgdb(igdbId = 1942L, name = "The Witcher 3")),
                SyncEntry.Unmatched(uids = listOf("999"), name = "Some Indie"),
            ),
        )

        assertEquals(SyncSummary(added = 2, updated = 0), summary)
        val owned = repository.ownedGames.first()
        assertEquals(2, owned.size)
        owned.forEach { tile ->
            assertEquals(listOf(Store.STEAM), tile.ownerships.map { it.store })
            assertEquals(listOf(Source.STEAM_SYNC), tile.ownerships.map { it.source })
        }
        val unmatched = dao.getGameByExternalUid(1, "999")!!
        assertNull(unmatched.igdbId)
        assertEquals("Some Indie", unmatched.name)
    }

    @Test
    fun steamResyncIsIdempotent() = runTest {
        val entries = listOf(
            SyncEntry.Matched(sampleIgdb(igdbId = 1942L)),
            SyncEntry.Unmatched(uids = listOf("999"), name = "Some Indie"),
        )
        repository.syncStore(Store.STEAM, entries)

        val summary = repository.syncStore(Store.STEAM, entries)

        assertEquals(SyncSummary(added = 0, updated = 2), summary)
        assertEquals(2, repository.ownedGames.first().size)
    }

    @Test
    fun steamUnmatchedDedupsByAppidNotName() = runTest {
        repository.syncStore(Store.STEAM, listOf(SyncEntry.Unmatched(uids = listOf("999"), name = "Indie")))

        val summary = repository.syncStore(Store.STEAM, listOf(SyncEntry.Unmatched(uids = listOf("999"), name = "Indie Renamed")))

        assertEquals(SyncSummary(added = 0, updated = 1), summary)
        val game = dao.getGameByExternalUid(1, "999")!!
        assertEquals("Indie", game.name) // name is never overwritten
        assertEquals(1, repository.ownedGames.first().size)
    }

    @Test
    fun steamSyncUpgradesManualOwnershipToSteamSync() = runTest {
        val added = repository.addMatchedGame(sampleIgdb(igdbId = 5L), wishlist = false, stores = setOf(Store.STEAM))
        assertEquals(Source.MANUAL, dao.ownershipsFor(added.gameId).single().source)

        repository.syncStore(Store.STEAM, listOf(SyncEntry.Matched(sampleIgdb(igdbId = 5L))))

        val ownership = dao.ownershipsFor(added.gameId).single()
        assertEquals(Store.STEAM, ownership.store)
        assertEquals(Source.STEAM_SYNC, ownership.source)
    }

    @Test
    fun steamSyncPreservesLocalStateAndCachedMetadata() = runTest {
        val added = repository.addMatchedGame(
            sampleIgdb(igdbId = 7L, name = "Original Name"),
            wishlist = false,
            status = Status.PLAYING,
            stores = setOf(Store.STEAM),
        )
        repository.setUserRating(added.gameId, 9.0)

        repository.syncStore(Store.STEAM, listOf(SyncEntry.Matched(sampleIgdb(igdbId = 7L, name = "Changed Name"))))

        val game = dao.getGame(added.gameId)!!
        assertEquals("Original Name", game.name) // cached metadata is never overwritten by a sync
        assertEquals(Status.PLAYING, game.status)
        assertEquals(9.0, game.userRating)
        assertEquals(1, dao.ownershipsFor(added.gameId).size)
    }

    @Test
    fun steamSyncOnWishlistedGameClearsWishlistAndOwnsIt() = runTest {
        val wished = repository.addMatchedGame(sampleIgdb(igdbId = 8L), wishlist = true)

        repository.syncStore(Store.STEAM, listOf(SyncEntry.Matched(sampleIgdb(igdbId = 8L))))

        val game = dao.getGame(wished.gameId)!!
        assertEquals(false, game.wishlist)
        assertEquals(Status.BACKLOG, game.status)
        val ownership = dao.ownershipsFor(wished.gameId).single()
        assertEquals(Store.STEAM, ownership.store)
        assertEquals(Source.STEAM_SYNC, ownership.source)
    }

    @Test
    fun steamMatchedUpgradesBareGameInPlace() = runTest {
        repository.syncStore(Store.STEAM, listOf(SyncEntry.Unmatched(uids = listOf("570"), name = "Bare Steam Game")))
        val bare = dao.getGameByExternalUid(1, "570")!!
        repository.setUserRating(bare.id, 9.0)

        val summary = repository.syncStore(
            Store.STEAM,
            listOf(SyncEntry.Matched(sampleIgdb(igdbId = 7346L, name = "Breath of the Wild"), uids = listOf("570"))),
        )

        assertEquals(SyncSummary(added = 0, updated = 1), summary)
        assertEquals(1, repository.ownedGames.first().size)
        val upgraded = dao.getGame(bare.id)!!
        assertEquals(7346L, upgraded.igdbId)
        assertEquals("Breath of the Wild", upgraded.name)
        assertEquals("co3p2d", upgraded.coverImageId)
        assertEquals(9.0, upgraded.userRating) // local state survives the upgrade
        assertEquals(Status.BACKLOG, upgraded.status)
        assertEquals(bare.id, dao.getGameByExternalUid(1, "570")!!.id)
        assertEquals(bare.id, dao.getGameByExternalUid(1, "1234")!!.id)
    }

    @Test
    fun migratesV3DatabasePreservingGamesAndDefaultingNewColumns() = runTest {
        database.close()
        dbFile.delete()
        seedVersion3Database(dbFile)

        database = Room.databaseBuilder<GamesLibraryDatabase>(name = dbFile.absolutePath)
            .buildGamesLibraryDatabase()
        repository = GameRepository(database.gameDao())

        val tile = repository.ownedGames.first().single()
        assertEquals("Existing Game", tile.game.name)
        assertEquals(Status.PLAYING, tile.game.status)
        assertNull(tile.game.userRating)
        assertEquals(false, tile.game.orphaned)
        assertEquals(listOf(Store.STEAM), tile.ownerships.map { it.store })
    }

    @Test
    fun insertStampsAddedAtFromTheClock() = runTest {
        val stamped = GameRepository(dao, clock = fixedClock(1_700_000_000_000L))
        val owned = stamped.addOwnedGame(name = "Celeste")
        val matched = stamped.addMatchedGame(sampleIgdb(igdbId = 42L), wishlist = false, stores = setOf(Store.STEAM))

        assertEquals(1_700_000_000_000L, dao.getGame(owned)!!.addedAt)
        assertEquals(1_700_000_000_000L, dao.getGame(matched.gameId)!!.addedAt)
    }

    @Test
    fun migratesV4DatabaseDefaultingAddedAtNull() = runTest {
        database.close()
        dbFile.delete()
        seedVersion4Database(dbFile)

        database = Room.databaseBuilder<GamesLibraryDatabase>(name = dbFile.absolutePath)
            .buildGamesLibraryDatabase()
        repository = GameRepository(database.gameDao())

        val tile = repository.ownedGames.first().single()
        assertEquals("Existing Game", tile.game.name)
        assertNull(tile.game.addedAt)
    }

    private fun fixedClock(millis: Long): Clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
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

    /** Writes a v3 schema database (Game widened with the IGDB metadata set + external_game, user_version = 3). */
    private fun seedVersion3Database(file: File) {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `game` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `igdbId` INTEGER, `wishlist` INTEGER NOT NULL, `status` TEXT, " +
                    "`slug` TEXT, `firstReleaseDate` INTEGER, `coverImageId` TEXT, `developer` TEXT, " +
                    "`totalRating` REAL, `totalRatingCount` INTEGER, `platforms` TEXT NOT NULL, " +
                    "`alternativeNames` TEXT NOT NULL)",
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
                "CREATE TABLE IF NOT EXISTS `external_game` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`gameId` INTEGER NOT NULL, `category` INTEGER NOT NULL, `uid` TEXT NOT NULL, `url` TEXT, " +
                    "FOREIGN KEY(`gameId`) REFERENCES `game`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_external_game_gameId` ON `external_game` (`gameId`)")
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_external_game_category_uid` ON `external_game` (`category`, `uid`)",
            )
            connection.execSQL(
                "INSERT INTO `game` (`name`, `igdbId`, `wishlist`, `status`, `platforms`, `alternativeNames`) " +
                    "VALUES ('Existing Game', 42, 0, 'PLAYING', '[]', '[]')",
            )
            connection.execSQL("INSERT INTO `ownership` (`gameId`, `store`, `source`) VALUES (1, 'STEAM', 'MANUAL')")
            connection.execSQL("PRAGMA user_version = 3")
        } finally {
            connection.close()
        }
    }

    /** Writes a v4 schema database (Game with userRating + orphaned, before addedAt; user_version = 4). */
    private fun seedVersion4Database(file: File) {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `game` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `igdbId` INTEGER, `wishlist` INTEGER NOT NULL, `status` TEXT, " +
                    "`slug` TEXT, `firstReleaseDate` INTEGER, `coverImageId` TEXT, `developer` TEXT, " +
                    "`totalRating` REAL, `totalRatingCount` INTEGER, `platforms` TEXT NOT NULL, " +
                    "`alternativeNames` TEXT NOT NULL, `userRating` REAL, `orphaned` INTEGER NOT NULL DEFAULT 0)",
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
                "CREATE TABLE IF NOT EXISTS `external_game` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`gameId` INTEGER NOT NULL, `category` INTEGER NOT NULL, `uid` TEXT NOT NULL, `url` TEXT, " +
                    "FOREIGN KEY(`gameId`) REFERENCES `game`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_external_game_gameId` ON `external_game` (`gameId`)")
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_external_game_category_uid` ON `external_game` (`category`, `uid`)",
            )
            connection.execSQL(
                "INSERT INTO `game` (`name`, `igdbId`, `wishlist`, `status`, `platforms`, `alternativeNames`, `orphaned`) " +
                    "VALUES ('Existing Game', 42, 0, 'PLAYING', '[]', '[]', 0)",
            )
            connection.execSQL("INSERT INTO `ownership` (`gameId`, `store`, `source`) VALUES (1, 'STEAM', 'MANUAL')")
            connection.execSQL("PRAGMA user_version = 4")
        } finally {
            connection.close()
        }
    }
}
