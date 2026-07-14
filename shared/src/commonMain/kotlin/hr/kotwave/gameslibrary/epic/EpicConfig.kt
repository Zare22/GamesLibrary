package hr.kotwave.gameslibrary.epic

/** Epic's OAuth2 + library/catalog endpoints, plus the Epic Games Launcher's public client credentials. */
internal data class EpicConfig(
    val tokenUrl: String = "https://account-public-service-prod.ol.epicgames.com/account/api/oauth/token",
    val libraryItemsUrl: String = "https://library-service.live.use1a.on.epicgames.com/library/api/public/items",
    val catalogNamespaceUrl: String = "https://catalog-public-service-prod06.ol.epicgames.com/catalog/api/shared/namespace",
    val signInUrl: String = EPIC_SIGN_IN_URL,
    val basicAuth: String = EPIC_BASIC_AUTH,
)

/** The Epic Games Launcher's OAuth2 client — public, not a secret: the same pair ships in every install. */
private const val EPIC_CLIENT_ID = "34a02cf8f4414e29b15921876da36f9a"

/** base64("client_id:client_secret") of the same public pair, for the token endpoint's Basic auth. */
private const val EPIC_BASIC_AUTH =
    "MzRhMDJjZjhmNDQxNGUyOWIxNTkyMTg3NmRhMzZmOWE6ZGFhZmJjY2M3Mzc3NDUwMzlkZmZlNTNkOTRmYzc2Y2Y="

/**
 * Epic's own sign-in page, bouncing to the redirect endpoint that renders `{"authorizationCode":"…"}`
 * as plain JSON once signed in. A real browser is required — Epic's hCaptcha breaks inside WebViews.
 */
private const val EPIC_SIGN_IN_URL =
    "https://www.epicgames.com/id/login?redirectUrl=" +
        "https%3A%2F%2Fwww.epicgames.com%2Fid%2Fapi%2Fredirect%3FclientId%3D$EPIC_CLIENT_ID%26responseType%3Dcode"
