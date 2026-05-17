package com.ottapp.moviestream.data.model

data class Reel(
    var id: String = "",
    var title: String = "",
    var videoUrl: String = "", // Can be direct mp4 or YouTube link
    var movieTitle: String = "", // For the search button
    var movieId: String = "" // Optional, if we want to link directly to movie detail
) {
    constructor() : this("", "", "", "", "")
}
