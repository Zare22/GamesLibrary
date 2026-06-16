package hr.kotwave.gameslibrary.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. */
sealed interface Route {
    @Serializable data object Library : Route
    @Serializable data object Wishlist : Route
    @Serializable data object Import : Route
    @Serializable data object Settings : Route
    @Serializable data object Add : Route
    @Serializable data object Gallery : Route
}
