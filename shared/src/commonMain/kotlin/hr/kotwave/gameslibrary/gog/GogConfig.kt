package hr.kotwave.gameslibrary.gog

/** GOG's OAuth2 + embed endpoints, plus Galaxy's public client credentials. */
internal data class GogConfig(
    val authUrl: String = "https://auth.gog.com/auth",
    val tokenUrl: String = "https://auth.gog.com/token",
    val embedUrl: String = "https://embed.gog.com",
    val clientId: String = GOG_CLIENT_ID,
    val clientSecret: String = GOG_CLIENT_SECRET,
    val redirectUri: String = GOG_REDIRECT_URI,
)

/** GOG Galaxy's OAuth2 client — public, not a secret: the same id/secret ships in every GOG client. */
private const val GOG_CLIENT_ID = "46899977096215655"
private const val GOG_CLIENT_SECRET = "9d85c43b1482497dbbce61f6e4aa173a433796eeae2ca8c5f6129f2dc4de46d9"

/** GOG-hosted post-login page; the auth `code` is read off this redirect (no loopback, unlike Steam). */
private const val GOG_REDIRECT_URI = "https://embed.gog.com/on_login_success?origin=client"
