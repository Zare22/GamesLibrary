package hr.kotwave.gameslibrary.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store

@Immutable
data class AppColors(
    val bg: Color,
    val bg2: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val brandGradient: List<Color>,
    val wordmark: List<Color>,
    val backdrop: List<Color>,
    val glowTopLeft: Color,
    val glowTopRight: Color,
)

@Immutable
data class StatusPalette(
    val backlog: Color,
    val playing: Color,
    val completed: Color,
    val dropped: Color,
) {
    fun color(status: Status): Color = when (status) {
        Status.BACKLOG -> backlog
        Status.PLAYING -> playing
        Status.COMPLETED -> completed
        Status.DROPPED -> dropped
    }

    /** Backlog renders flat; Playing/Completed/Dropped carry a colored glow. */
    fun glows(status: Status): Boolean = status != Status.BACKLOG
}

@Immutable
data class StorePalette(
    val steam: Color,
    val gog: Color,
    val psn: Color,
    val psnText: Color,
    val xbox: Color,
    val xboxText: Color,
    val epic: Color,
    val nintendo: Color,
    val itch: Color,
    val battleNet: Color,
) {
    /** Border / glow accent for a store. */
    fun accent(store: Store): Color = when (store) {
        Store.STEAM -> steam
        Store.GOG -> gog
        Store.PSN -> psn
        Store.XBOX -> xbox
        Store.EPIC -> epic
        Store.NINTENDO -> nintendo
        Store.ITCH -> itch
        Store.BATTLE_NET -> battleNet
    }

    /** Glyph text color — PSN/Xbox lifted for legibility on dark surfaces. */
    fun glyph(store: Store): Color = when (store) {
        Store.PSN -> psnText
        Store.XBOX -> xboxText
        else -> accent(store)
    }

    /** Epic + itch render without a glow. */
    fun glows(store: Store): Boolean = store != Store.EPIC && store != Store.ITCH
}

@Immutable
data class Spacing(
    val micro: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val gutter: Dp = 22.dp,
)

@Immutable
data class Radii(
    val window: Dp = 18.dp,
    val largeCard: Dp = 18.dp,
    val tile: Dp = 14.dp,
    val button: Dp = 13.dp,
    val badge: Dp = 9.dp,
    val pill: Dp = 999.dp,
)

@Immutable
data class GlassStyle(
    val blurRadius: Dp = 14.dp,
)

@Immutable
data class AppTokens(
    val colors: AppColors,
    val status: StatusPalette,
    val store: StorePalette,
    val spacing: Spacing,
    val radii: Radii,
    val glass: GlassStyle,
)

val LocalAppTokens = staticCompositionLocalOf<AppTokens> {
    error("AppTokens not provided — wrap content in GamesLibraryTheme")
}

fun gamesLibraryTokens(): AppTokens = AppTokens(
    colors = AppColors(
        bg = Bg,
        bg2 = Bg2,
        surface = Surface,
        surfaceRaised = SurfaceRaised,
        border = BorderHairline,
        borderStrong = BorderStrong,
        text = TextPrimary,
        muted = TextMuted,
        faint = TextFaint,
        accent = Accent,
        brandGradient = listOf(BrandGradientStart, BrandGradientEnd),
        wordmark = listOf(WordmarkTop, WordmarkBottom),
        backdrop = listOf(BackdropTop, BackdropBottom),
        glowTopLeft = GlowTopLeft,
        glowTopRight = GlowTopRight,
    ),
    status = StatusPalette(
        backlog = StatusBacklog,
        playing = StatusPlaying,
        completed = StatusCompleted,
        dropped = StatusDropped,
    ),
    store = StorePalette(
        steam = StoreSteam,
        gog = StoreGog,
        psn = StorePsn,
        psnText = StorePsnText,
        xbox = StoreXbox,
        xboxText = StoreXboxText,
        epic = StoreEpic,
        nintendo = StoreNintendo,
        itch = StoreItch,
        battleNet = StoreBattleNet,
    ),
    spacing = Spacing(),
    radii = Radii(),
    glass = GlassStyle(),
)
