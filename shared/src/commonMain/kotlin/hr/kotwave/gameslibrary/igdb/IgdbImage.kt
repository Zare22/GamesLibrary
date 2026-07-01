package hr.kotwave.gameslibrary.igdb

/** Builds an IGDB image URL from a cover `image_id`; the loader's disk cache holds the bytes. */
object IgdbImage {
    /** Grid tiles and small thumbnails — display size ≈ source. */
    const val GRID = "t_cover_big"

    /** Detail heroes and the desktop crisp poster — upscaled large, so requests the full variant. */
    const val HERO = "t_1080p"

    fun coverUrl(imageId: String, size: String = GRID): String =
        "https://images.igdb.com/igdb/image/upload/$size/$imageId.jpg"
}
