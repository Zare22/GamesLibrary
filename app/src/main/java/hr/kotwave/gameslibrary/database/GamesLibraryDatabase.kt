package hr.kotwave.gameslibrary.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import hr.kotwave.gameslibrary.game.Game
import hr.kotwave.gameslibrary.game.GameDao
import hr.kotwave.gameslibrary.platform.PlatformConverter

@Database(entities = [Game::class], version = 2)
@TypeConverters(PlatformConverter::class)
abstract class GamesLibraryDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}