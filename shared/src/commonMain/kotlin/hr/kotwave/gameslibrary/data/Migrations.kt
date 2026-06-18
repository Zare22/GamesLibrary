package hr.kotwave.gameslibrary.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v1 (thin `game(id, name)`) -> v2: widens Game with igdb_id/wishlist/status and adds the
 * Ownership table. Pre-v2 rows are treated as owned, so they default to Backlog.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `igdbId` INTEGER")
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `wishlist` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `status` TEXT")
        connection.execSQL("UPDATE `game` SET `status` = 'BACKLOG'")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_game_igdbId` ON `game` (`igdbId`)")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `ownership` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`gameId` INTEGER NOT NULL, " +
                "`store` TEXT NOT NULL, " +
                "`source` TEXT NOT NULL, " +
                "FOREIGN KEY(`gameId`) REFERENCES `game`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_ownership_gameId_store` ON `ownership` (`gameId`, `store`)",
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_ownership_gameId` ON `ownership` (`gameId`)")
    }
}

/**
 * v2 -> v3: widens Game with the IGDB metadata set (scalars nullable; platforms/alternativeNames as
 * JSON, defaulting empty) and adds the external_game table for cross-store references.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `slug` TEXT")
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `firstReleaseDate` INTEGER")
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `coverImageId` TEXT")
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `developer` TEXT")
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `totalRating` REAL")
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `totalRatingCount` INTEGER")
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `platforms` TEXT NOT NULL DEFAULT '[]'")
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `alternativeNames` TEXT NOT NULL DEFAULT '[]'")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `external_game` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`gameId` INTEGER NOT NULL, " +
                "`category` INTEGER NOT NULL, " +
                "`uid` TEXT NOT NULL, " +
                "`url` TEXT, " +
                "FOREIGN KEY(`gameId`) REFERENCES `game`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_external_game_gameId` ON `external_game` (`gameId`)")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_external_game_category_uid` ON `external_game` (`category`, `uid`)",
        )
    }
}

/**
 * v3 -> v4: adds the local-only `userRating` (nullable, 0.0–10.0) and the `orphaned` flag (whether
 * the Game's igdb_id no longer resolves). Both default to "no value" for existing rows.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `userRating` REAL")
        connection.execSQL("ALTER TABLE `game` ADD COLUMN `orphaned` INTEGER NOT NULL DEFAULT 0")
    }
}
