package com.ottapp.moviestream.data.model

data class Movie(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var bannerImageUrl: String = "",
    var detailThumbnailUrl: String = "",
    var videoStreamUrl: String = "",
    var downloadUrl: String = "",
    var category: String = "",
    var imdbRating: Double = 0.0,
    var trending: Boolean = false,
    var testMovie: Boolean = false,
    var year: Int = 0,
    var duration: String = "",
    var actorIds: List<String> = emptyList(),
    var downloads: List<DownloadQuality> = emptyList(),
    // ── Genre tags (comma-separated or list) ─────────────────────────────
    var genre: String = "",
    var genres: List<String> = emptyList(),
    // ── Trailer ──────────────────────────────────────────────────────────
    var trailerUrl: String = "",
    // ── Series / Episodes ─────────────────────────────────────────────────
    var isSeries: Boolean = false,
    var totalSeasons: Int = 0,
    var seasons: List<Season> = emptyList()
) {
    constructor() : this("", "", "", "", "", "", "", "", 0.0, false, false, 0, "", emptyList(), emptyList(), "", emptyList(), "", false, 0, emptyList())

    val genreList: List<String>
        get() = when {
            genres.isNotEmpty() -> genres
            genre.isNotBlank()  -> genre.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else                -> emptyList()
        }

    companion object {
        const val CATEGORY_BANGLA  = "Bangla Dubbed"
        const val CATEGORY_HINDI   = "Hindi Dubbed"
        const val CATEGORY_ENGLISH = "English"
    }
}

data class DownloadQuality(
    var quality: String = "",
    var url: String = "",
    var size: String = ""
)

// ── Series Models ─────────────────────────────────────────────────────────────
data class Season(
    var seasonNumber: Int = 0,
    var title: String = "",
    var episodes: List<Episode> = emptyList()
) {
    constructor() : this(0, "", emptyList())
}

data class Episode(
    var episodeNumber: Int = 0,
    var title: String = "",
    var description: String = "",
    var duration: String = "",
    var thumbnailUrl: String = "",
    var streamUrl: String = "",
    var downloadUrl: String = "",
    var isFree: Boolean = false
) {
    constructor() : this(0, "", "", "", "", "", "", false)
}
