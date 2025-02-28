package hr.kotwave.gameslibrary.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object GamesGraph : Route
    @Serializable
    data object GamesList : Route
    @Serializable
    data object GameDetails : Route
}