package hr.kotwave.gameslibrary.ui.model

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The release year for an IGDB epoch-second timestamp, or null. */
fun releaseYear(epochSeconds: Long?): Int? =
    epochSeconds?.let { Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.UTC).year }

/** "2022 · FromSoftware", "2022", "FromSoftware", or null — the meta line under a game's name. */
fun gameMeta(firstReleaseDate: Long?, developer: String?): String? =
    listOfNotNull(releaseYear(firstReleaseDate)?.toString(), developer)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")
