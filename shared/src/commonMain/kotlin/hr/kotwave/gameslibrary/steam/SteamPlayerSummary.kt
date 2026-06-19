package hr.kotwave.gameslibrary.steam

/** A signed-in player's public persona, from GetPlayerSummaries — the connected card's name + avatar. */
data class SteamPlayerSummary(val personaName: String, val avatarUrl: String)
