package hr.kotwave.gameslibrary.data

import androidx.room.Room
import hr.kotwave.gameslibrary.data.db.GameDao
import hr.kotwave.gameslibrary.data.db.GamesLibraryDatabase
import hr.kotwave.gameslibrary.data.db.buildGamesLibraryDatabase
import hr.kotwave.gameslibrary.data.sync.SyncEntry
import hr.kotwave.gameslibrary.data.sync.SyncTailRow
import hr.kotwave.gameslibrary.secure.EPIC_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.GOG_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_HOST_ENDPOINT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_HOST_FINGERPRINT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_LAST_MIRROR_AT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_NEEDS_REPAIR_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_PAIRED_AT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_HOST_KEYSTORE_PASSWORD_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_HOST_TOKEN_HASH_KEY
import hr.kotwave.gameslibrary.secure.PSN_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.STEAM_ID_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalDataResetTest {

    private lateinit var dbFile: File
    private lateinit var database: GamesLibraryDatabase
    private lateinit var dao: GameDao
    private lateinit var repository: GameRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("games_library_reset_test", ".db").apply { delete() }
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
    fun resetEmptiesEveryTableAndRemovesEverySecret() = runTest {
        // A synced-unmatched game seeds all three tables: Game + Ownership + external reference.
        repository.syncStore(Store.STEAM, listOf(SyncEntry.Unmatched(uids = listOf("999"), name = "Some Indie")))
        repository.addWishlistGame(name = "Hollow Knight: Silksong")
        repository.confirmSyncReview(
            Store.STEAM,
            picks = emptyList(),
            bare = emptyList(),
            dismissed = listOf(SyncTailRow("Dismissed App", listOf("111"))),
        )
        dao.upsertMirrorBaseline(MirrorBaseline("desktop", "{\"schemaVersion\":1,\"games\":[]}"))
        assertTrue(dao.allGamesWithOwnerships().isNotEmpty())
        assertTrue(dao.allExternalGames().isNotEmpty())
        assertTrue(dao.dismissedSyncUids(1, listOf("111")).isNotEmpty())
        assertNotNull(dao.mirrorBaseline("desktop"))

        val secure = RecordingSecureStorage().apply {
            put(STEAM_ID_KEY, "76561190000000000")
            put(GOG_TOKEN_KEY, "{\"access\":\"x\"}")
            put(PSN_TOKEN_KEY, "{\"access\":\"y\"}")
            put(EPIC_TOKEN_KEY, "{\"access\":\"z\"}")
            put(MIRROR_CLIENT_TOKEN_KEY, "mirror-token")
            put(MIRROR_CLIENT_HOST_FINGERPRINT_KEY, "aabb")
            put(MIRROR_CLIENT_HOST_ENDPOINT_KEY, "192.168.1.10:56789")
            put(MIRROR_CLIENT_PAIRED_AT_KEY, "1752800000000")
            put(MIRROR_CLIENT_NEEDS_REPAIR_KEY, "true")
            put(MIRROR_CLIENT_LAST_MIRROR_AT_KEY, "1752810000000")
            put(MIRROR_HOST_KEYSTORE_PASSWORD_KEY, "keystore-pw")
            put(MIRROR_HOST_TOKEN_HASH_KEY, "token-hash")
        }

        LocalDataReset(repository, secure).reset()

        assertTrue(dao.allGamesWithOwnerships().isEmpty())
        assertTrue(dao.allExternalGames().isEmpty())
        assertTrue(dao.dismissedSyncUids(1, listOf("111")).isEmpty())
        assertNull(dao.mirrorBaseline("desktop"))
        assertTrue(repository.ownedGames.first().isEmpty())
        assertTrue(repository.wishlistGames.first().isEmpty())
        assertNull(secure.get(STEAM_ID_KEY))
        assertNull(secure.get(GOG_TOKEN_KEY))
        assertNull(secure.get(PSN_TOKEN_KEY))
        assertNull(secure.get(EPIC_TOKEN_KEY))
        assertNull(secure.get(MIRROR_CLIENT_TOKEN_KEY))
        assertNull(secure.get(MIRROR_CLIENT_HOST_FINGERPRINT_KEY))
        assertNull(secure.get(MIRROR_CLIENT_HOST_ENDPOINT_KEY))
        assertNull(secure.get(MIRROR_CLIENT_PAIRED_AT_KEY))
        assertNull(secure.get(MIRROR_CLIENT_NEEDS_REPAIR_KEY))
        assertNull(secure.get(MIRROR_CLIENT_LAST_MIRROR_AT_KEY))
        assertNull(secure.get(MIRROR_HOST_KEYSTORE_PASSWORD_KEY))
        assertNull(secure.get(MIRROR_HOST_TOKEN_HASH_KEY))
        assertEquals(
            listOf(
                STEAM_ID_KEY, GOG_TOKEN_KEY, PSN_TOKEN_KEY, EPIC_TOKEN_KEY,
                MIRROR_CLIENT_TOKEN_KEY, MIRROR_CLIENT_HOST_FINGERPRINT_KEY, MIRROR_CLIENT_HOST_ENDPOINT_KEY,
                MIRROR_CLIENT_PAIRED_AT_KEY, MIRROR_CLIENT_NEEDS_REPAIR_KEY, MIRROR_CLIENT_LAST_MIRROR_AT_KEY,
                MIRROR_HOST_KEYSTORE_PASSWORD_KEY, MIRROR_HOST_TOKEN_HASH_KEY,
            ),
            secure.removed,
        )
    }
}

/** In-memory [SecureStorage] that records which keys were removed, in order. */
private class RecordingSecureStorage : SecureStorage {
    private val store = mutableMapOf<String, String>()
    val removed = mutableListOf<String>()

    override suspend fun get(key: String): String? = store[key]
    override suspend fun put(key: String, value: String) {
        store[key] = value
    }

    override suspend fun remove(key: String) {
        store.remove(key)
        removed += key
    }
}
