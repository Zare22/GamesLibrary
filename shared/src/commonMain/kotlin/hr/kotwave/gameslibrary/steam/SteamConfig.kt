package hr.kotwave.gameslibrary.steam

/** Steam Web API endpoint + the developer key. `baseUrl` is swappable for the post-v1 proxy (ADR 0003). */
internal data class SteamConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.steampowered.com",
)
