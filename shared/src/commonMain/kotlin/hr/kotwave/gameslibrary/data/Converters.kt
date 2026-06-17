package hr.kotwave.gameslibrary.data

import androidx.room.TypeConverter

/** Persists the domain enums as their `name` strings. */
class Converters {
    @TypeConverter fun storeToString(value: Store): String = value.name
    @TypeConverter fun stringToStore(value: String): Store = Store.valueOf(value)

    @TypeConverter fun statusToString(value: Status?): String? = value?.name
    @TypeConverter fun stringToStatus(value: String?): Status? = value?.let(Status::valueOf)

    @TypeConverter fun sourceToString(value: Source): String = value.name
    @TypeConverter fun stringToSource(value: String): Source = Source.valueOf(value)
}
