package com.ottapp.moviestream.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TmdbService {
    // Note: Using a placeholder API key. In a real app, this should be in BuildConfig or a secure place.
    private val API_KEY = "89620399793740293740293740293740" 
    
    private val BASE_URL = "https://api.themoviedb.org/3"
    private val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
    private val BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/original"

    suspend fun searchMovie(query: String): TmdbMovie? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/search/movie?api_key=$API_KEY&query=${query.replace(" ", "%20")}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val results = json.optJSONArray("results")
            
            if (results != null && results.length() > 0) {
                val firstResult = results.getJSONObject(0)
                val id = firstResult.getInt("id")
                return@withContext getMovieDetails(id)
            }
        } catch (e: Exception) {
            Log.e("TmdbService", "Error searching movie: ${e.message}")
        }
        null
    }

    private suspend fun getMovieDetails(tmdbId: Int): TmdbMovie? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/movie/$tmdbId?api_key=$API_KEY&append_to_response=credits")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            
            val title = json.optString("title")
            val overview = json.optString("overview")
            val posterPath = json.optString("poster_path")
            val backdropPath = json.optString("backdrop_path")
            val releaseDate = json.optString("release_date")
            val voteAverage = json.optDouble("vote_average", 0.0)
            val runtime = json.optInt("runtime", 0)
            
            val year = if (releaseDate.isNotEmpty()) {
                try { releaseDate.substring(0, 4).toInt() } catch (e: Exception) { 0 }
            } else 0
            
            val duration = if (runtime > 0) {
                val h = runtime / 60
                val m = runtime % 60
                if (h > 0) "${h}h ${m}m" else "${m}m"
            } else ""
            
            val cast = mutableListOf<TmdbCast>()
            val credits = json.optJSONObject("credits")
            if (credits != null) {
                val castArray = credits.optJSONArray("cast")
                if (castArray != null) {
                    for (i in 0 until minOf(castArray.length(), 10)) {
                        val actorJson = castArray.getJSONObject(i)
                        cast.add(TmdbCast(
                            name = actorJson.optString("name"),
                            profilePath = actorJson.optString("profile_path")
                        ))
                    }
                }
            }

            return@withContext TmdbMovie(
                title = title,
                description = overview,
                posterUrl = if (posterPath.isNotEmpty()) "$IMAGE_BASE_URL$posterPath" else "",
                backdropUrl = if (backdropPath.isNotEmpty()) "$BACKDROP_BASE_URL$backdropPath" else "",
                year = year,
                rating = voteAverage,
                duration = duration,
                cast = cast
            )
        } catch (e: Exception) {
            Log.e("TmdbService", "Error getting movie details: ${e.message}")
        }
        null
    }
}

data class TmdbMovie(
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val year: Int,
    val rating: Double,
    val duration: String,
    val cast: List<TmdbCast>
)

data class TmdbCast(
    val name: String,
    val profilePath: String?
)
