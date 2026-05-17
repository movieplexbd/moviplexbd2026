package com.ottapp.moviestream.util

import android.content.Context
import android.content.SharedPreferences
import com.ottapp.moviestream.data.model.Movie
import org.json.JSONArray
import org.json.JSONObject

/**
 * Recently viewed movie tracker - সর্বশেষ ৩০টি দেখা movie save করে
 */
data class RecentlyViewedEntry(
    val movieId: String,
    val title: String,
    val bannerUrl: String,
    val category: String,
    val imdbRating: String,
    val viewedAt: Long
)

class RecentlyViewedManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("recently_viewed", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LIST = "viewed_list"
        private const val MAX_ITEMS = 30
    }

    fun addMovie(movie: Movie) {
        val list = getAll().toMutableList()
        list.removeAll { it.movieId == movie.id }
        list.add(0, RecentlyViewedEntry(
            movieId    = movie.id,
            title      = movie.title,
            bannerUrl  = movie.bannerImageUrl,
            category   = movie.category,
            imdbRating = movie.imdbRating,
            viewedAt   = System.currentTimeMillis()
        ))
        if (list.size > MAX_ITEMS) list.removeAt(list.size - 1)
        save(list)
    }

    fun getAll(): List<RecentlyViewedEntry> {
        val json = prefs.getString(KEY_LIST, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                RecentlyViewedEntry(
                    movieId    = o.optString("movieId"),
                    title      = o.optString("title"),
                    bannerUrl  = o.optString("bannerUrl"),
                    category   = o.optString("category"),
                    imdbRating = o.optString("imdbRating"),
                    viewedAt   = o.optLong("viewedAt")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun removeMovie(movieId: String) {
        val list = getAll().toMutableList()
        list.removeAll { it.movieId == movieId }
        save(list)
    }

    fun clearAll() {
        prefs.edit().remove(KEY_LIST).apply()
    }

    private fun save(list: List<RecentlyViewedEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("movieId",    e.movieId)
                put("title",      e.title)
                put("bannerUrl",  e.bannerUrl)
                put("category",   e.category)
                put("imdbRating", e.imdbRating)
                put("viewedAt",   e.viewedAt)
            })
        }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }
}
