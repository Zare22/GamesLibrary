package hr.kotwave.gameslibrary.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. */
sealed interface Route {
    @Serializable data object Library : Route
    @Serializable data object Wishlist : Route
    @Serializable data object Import : Route
    @Serializable data object LibraryImport : Route
    @Serializable data object Settings : Route
    @Serializable data object Add : Route
    @Serializable data object Gallery : Route
    @Serializable data object Steam : Route
    @Serializable data object Gog : Route
    @Serializable data object Psn : Route
    @Serializable data object Epic : Route
    @Serializable data object BattleNet : Route
    @Serializable data object Mirror : Route
    @Serializable data class Detail(val gameId: Long) : Route
}
