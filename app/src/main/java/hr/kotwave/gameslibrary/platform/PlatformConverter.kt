package hr.kotwave.gameslibrary.platform

import androidx.room.TypeConverter

class PlatformConverter {
    @TypeConverter
    fun fromPlatform(platform: Platform): String = platform.name

    @TypeConverter
    fun toPlatform(name: String): Platform = Platform.valueOf(name)
}