package hr.kotwave.gameslibrary.igdb

/** Builds an IGDB image URL from a cover `image_id`; the loader's disk cache holds the bytes. */
object IgdbImage {
    fun coverUrl(imageId: String, size: String = "t_cover_big"): String =
        "https://images.igdb.com/igdb/image/upload/$size/$imageId.jpg"
}
