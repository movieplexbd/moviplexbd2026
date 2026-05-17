package com.ottapp.moviestream.data.model

data class MovieRating(
    var id: String = "",
    var movieId: String = "",
    var userId: String = "",
    var userName: String = "",
    var rating: Float = 0f,
    var review: String = "",
    var timestamp: Long = 0L
) {
    constructor() : this("", "", "", "", 0f, "", 0L)
}
