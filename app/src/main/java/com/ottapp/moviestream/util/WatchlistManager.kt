package com.ottapp.moviestream.util

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.ottapp.moviestream.data.model.Movie
import org.json.JSONArray
import org.json.JSONObject

data class WatchlistEntry(
    val movieId: String,
    val title: String,
    val bannerUrl: String,
    val category: String,
    val rating: Double,
    val addedAt: Long
)

class WatchlistManager(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        private const val KEY_WATCHLIST = "watchlist_items"
        private const val PREFS_PREFIX  = "watchlist_prefs_"
    }

    /** Returns SharedPreferences keyed per Firebase user UID (falls back to "guest") */
    private fun prefs(): SharedPreferences {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        return appContext.getSharedPreferences("$PREFS_PREFIX$uid", Context.MODE_PRIVATE)
    }

    fun addToWatchlist(movie: Movie) {
        val list = getWatchlist().toMutableList()
        if (list.none { it.movieId == movie.id }) {
            list.add(0, WatchlistEntry(
                movieId   = movie.id,
                title     = movie.title,
                bannerUrl = movie.bannerImageUrl,
                category  = movie.category,
                rating    = movie.imdbRating,
                addedAt   = System.currentTimeMillis()
            ))
            saveWatchlist(list)
        }
    }

    fun removeFromWatchlist(movieId: String) {
        val list = getWatchlist().toMutableList()
        list.removeAll { it.movieId == movieId }
        saveWatchlist(list)
    }

    fun isInWatchlist(movieId: String): Boolean = getWatchlist().any { it.movieId == movieId }

    fun toggleWatchlist(movie: Movie): Boolean {
        return if (isInWatchlist(movie.id)) {
            removeFromWatchlist(movie.id)
            false
        } else {
            addToWatchlist(movie)
            true
        }
    }

    fun getWatchlist(): List<WatchlistEntry> {
        val json = prefs().getString(KEY_WATCHLIST, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                WatchlistEntry(
                    movieId   = obj.optString("movieId"),
                    title     = obj.optString("title"),
                    bannerUrl = obj.optString("bannerUrl"),
                    category  = obj.optString("category"),
                    rating    = obj.optDouble("rating", 0.0),
                    addedAt   = obj.optLong("addedAt")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun clearAll() {
        prefs().edit().remove(KEY_WATCHLIST).apply()
    }

    private fun saveWatchlist(list: List<WatchlistEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("movieId",   e.movieId)
                put("title",     e.title)
                put("bannerUrl", e.bannerUrl)
                put("category",  e.category)
                put("rating",    e.rating)
                put("addedAt",   e.addedAt)
            })
        }
        prefs().edit().putString(KEY_WATCHLIST, arr.toString()).apply()
    }
}
