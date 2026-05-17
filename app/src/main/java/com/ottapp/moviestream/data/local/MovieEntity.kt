package com.ottapp.moviestream.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: String,
    val title: String = "",
    val description: String = "",
    val bannerImageUrl: String = "",
    val detailThumbnailUrl: String = "",
    val videoStreamUrl: String = "",
    val downloadUrl: String = "",
    val category: String = "",
    val imdbRating: Double = 0.0,
    val year: Int = 0,
    val duration: String = "",
    val trending: Boolean = false,
    val testMovie: Boolean = false,
    val actorIdsJson: String = "[]",
    val downloadsJson: String = "[]",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey val id: String,
    val imageUrl: String = "",
    val title: String = "",
    val category: String = "",
    val imdbRating: Double = 0.0,
    val testMovie: Boolean = false,
    val movieId: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
