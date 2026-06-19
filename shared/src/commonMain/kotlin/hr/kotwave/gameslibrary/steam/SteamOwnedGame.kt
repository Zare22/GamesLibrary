package hr.kotwave.gameslibrary.steam

/** One game owned on Steam, as returned by GetOwnedGames. `appid` is the IGDB external-games match key. */
data class SteamOwnedGame(val appid: Long, val name: String)
