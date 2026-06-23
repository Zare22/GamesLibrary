package hr.kotwave.gameslibrary.gog

/** One game owned on GOG, from getFilteredProducts. `id` is the IGDB external-games match key (category 5). */
data class GogOwnedGame(val id: Long, val title: String)
