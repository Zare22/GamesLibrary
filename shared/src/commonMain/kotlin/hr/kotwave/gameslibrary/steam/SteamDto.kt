package hr.kotwave.gameslibrary.steam

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
