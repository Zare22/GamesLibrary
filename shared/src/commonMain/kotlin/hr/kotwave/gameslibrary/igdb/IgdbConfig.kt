package hr.kotwave.gameslibrary.igdb

/** IGDB/Twitch endpoints and credentials; `baseUrl` can point at a proxy. */
internal data class IgdbConfig(
    val clientId: String,
    val clientSecret: String,
    val baseUrl: String = "https://api.igdb.com/v4",
    val authUrl: String = "https://id.twitch.tv/oauth2/token",
)
