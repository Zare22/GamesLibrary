package hr.kotwave.gameslibrary.steam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GetOwnedGames response. A private profile returns an empty `response` object (no `games`). */
@Serializable
internal data class OwnedGamesResponse(val response: OwnedGamesBody = OwnedGamesBody())

@Serializable
internal data class OwnedGamesBody(
    val gameCount: Int = 0,
    val games: List<OwnedGameDto>? = null,
)

@Serializable
internal data class OwnedGameDto(val appid: Long, val name: String? = null)

internal fun OwnedGameDto.toOwnedGame(): SteamOwnedGame? =
    name?.takeIf { it.isNotBlank() }?.let { SteamOwnedGame(appid = appid, name = it) }

/** GetPlayerSummaries response. Steam's persona fields are lowercase (not snake_case) — pinned by name. */
@Serializable
internal data class PlayerSummariesResponse(val response: PlayerSummariesBody = PlayerSummariesBody())

@Serializable
internal data class PlayerSummariesBody(val players: List<PlayerSummaryDto> = emptyList())

@Serializable
internal data class PlayerSummaryDto(
    @SerialName("steamid") val steamId: String,
    @SerialName("personaname") val personaName: String,
    @SerialName("avatarfull") val avatarUrl: String,
)

internal fun PlayerSummaryDto.toPlayerSummary(): SteamPlayerSummary =
    SteamPlayerSummary(personaName = personaName, avatarUrl = avatarUrl)
