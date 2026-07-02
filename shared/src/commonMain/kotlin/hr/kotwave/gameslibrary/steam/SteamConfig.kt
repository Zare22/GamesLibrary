package hr.kotwave.gameslibrary.steam

/** Steam Web API endpoint + the developer key; `baseUrl` can point at a proxy. */
internal data class SteamConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.steampowered.com",
)
