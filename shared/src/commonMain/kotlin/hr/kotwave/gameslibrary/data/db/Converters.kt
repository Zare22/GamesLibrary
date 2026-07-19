package hr.kotwave.gameslibrary.data.db

import androidx.room.TypeConverter
import hr.kotwave.gameslibrary.data.Platform
import hr.kotwave.gameslibrary.data.Source
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/** Persists the domain enums as their `name` strings and the metadata lists as JSON. */
class Converters {
    @TypeConverter fun storeToString(value: Store): String = value.name
    @TypeConverter fun stringToStore(value: String): Store = Store.valueOf(value)

    @TypeConverter fun statusToString(value: Status?): String? = value?.name
    @TypeConverter fun stringToStatus(value: String?): Status? = value?.let(Status::valueOf)

    @TypeConverter fun sourceToString(value: Source): String = value.name
    @TypeConverter fun stringToSource(value: String): Source = Source.valueOf(value)

    @TypeConverter fun platformsToJson(value: List<Platform>): String = json.encodeToString(value)
    @TypeConverter fun jsonToPlatforms(value: String): List<Platform> = json.decodeFromString(value)

    @TypeConverter fun stringsToJson(value: List<String>): String = json.encodeToString(value)
    @TypeConverter fun jsonToStrings(value: String): List<String> = json.decodeFromString(value)
}
