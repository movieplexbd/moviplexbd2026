package com.ottapp.moviestream.data.model

data class DownloadedMovie(
    val movieId: String = "",
    val title: String = "",
    val bannerImageUrl: String = "",
    val localFilePath: String = "",
    val fileSize: String = "",       // human-readable e.g. "1.2 GB"
    val fileSizeBytes: Long = 0L,    // raw bytes for sorting
    val downloadedAt: Long = 0L,   // Unix timestamp
    val imdbRating: Float = 0f,
    val category: String = "",
    val duration: String = ""
)
