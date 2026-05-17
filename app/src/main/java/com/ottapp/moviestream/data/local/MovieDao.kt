package com.ottapp.moviestream.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY title ASC")
    suspend fun getAllMovies(): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: String): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<MovieEntity>)

    @Query("DELETE FROM movies")
    suspend fun deleteAll()

    @Query("SELECT * FROM banners")
    suspend fun getAllBanners(): List<BannerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBanners(banners: List<BannerEntity>)

    @Query("DELETE FROM banners")
    suspend fun deleteAllBanners()
}
