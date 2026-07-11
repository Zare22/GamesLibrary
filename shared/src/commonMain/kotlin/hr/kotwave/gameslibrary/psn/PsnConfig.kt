package hr.kotwave.gameslibrary.psn

/** Sony's OAuth2 + library endpoints, plus the PlayStation App's public client credentials. */
internal data class PsnConfig(
    val authorizeUrl: String = "https://ca.account.sony.com/api/authz/v3/oauth/authorize",
    val tokenUrl: String = "https://ca.account.sony.com/api/authz/v3/oauth/token",
    val graphqlUrl: String = "https://web.np.playstation.com/api/graphql/v1/op",
    val playedTitlesUrl: String = "https://m.np.playstation.com/api/gamelist/v2/users/me/titles",
    val titleCatalogUrl: String = "https://m.np.playstation.com/api/catalog/v2/titles",
    val signInUrl: String = "https://www.playstation.com/",
    val npssoUrl: String = "https://ca.account.sony.com/api/v1/ssocookie",
    val clientId: String = PSN_CLIENT_ID,
    val basicAuth: String = PSN_BASIC_AUTH,
    val redirectUri: String = PSN_REDIRECT_URI,
    val scope: String = PSN_SCOPE,
)

/** The official PlayStation App's OAuth2 client — public, not a secret: the same pair ships in every install. */
private const val PSN_CLIENT_ID = "09515159-7237-4370-9b40-3806e67c0891"

/** base64("client_id:client_secret") of the same public pair, for the token endpoint's Basic auth. */
private const val PSN_BASIC_AUTH = "MDk1MTUxNTktNzIzNy00MzcwLTliNDAtMzgwNmU2N2MwODkxOnVjUGprYTV0bnRCMktxc1A="

/** The PS App's mobile deep link; the authorize 302's Location carries `?code=` on this scheme. */
private const val PSN_REDIRECT_URI = "com.scee.psxandroid.scecompcall://redirect"

private const val PSN_SCOPE = "psn:mobile.v2.core psn:clientapp"
