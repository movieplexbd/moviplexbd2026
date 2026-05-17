package com.ottapp.moviestream.data.model

data class Banner(
    var id: String = "",
    var imageUrl: String = "",
    var title: String = "",
    var category: String = "",
    var imdbRating: Double = 0.0,
    var testMovie: Boolean = false,
    var movieId: String = "" // Optional: link to a movie if needed, but requirements say manual
) {
    constructor() : this("", "", "", "", 0.0, false, "")
}
