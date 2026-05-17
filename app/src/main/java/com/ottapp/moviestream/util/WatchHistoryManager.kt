package com.ottapp.moviestream.util

import android.content.Context
import android.content.SharedPreferences
import com.ottapp.moviestream.data.model.Movie
import org.json.JSONArray
import org.json.JSONObject

data class WatchHistoryEntry(
    val movieId: String,
    val title: String,
    val bannerUrl: String,
    val category: String,
    val progressMs: Long,
    val durationMs: Long,
    val lastWatched: Long
) {
    val progressPercent: Int
        get() = if (durationMs > 0) ((progressMs * 100) / durationMs).toInt().coerceIn(0, 100) else 0
}

class WatchHistoryManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("watch_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY = "history_list"
        private const val MAX_ENTRIES = 20
    }

    fun saveProgress(movie: Movie, positionMs: Long, durationMs: Long) {
        val entries = getHistory().toMutableList()
        entries.removeAll { it.movieId == movie.id }
        entries.add(0, WatchHistoryEntry(
            movieId     = movie.id,
            title       = movie.title,
            bannerUrl   = movie.bannerImageUrl,
            category    = movie.category,
            progressMs  = positionMs,
            durationMs  = durationMs,
            lastWatched = System.currentTimeMillis()
        ))
        if (entries.size > MAX_ENTRIES) entries.removeAt(entries.size - 1)
        saveHistory(entries)
    }

    fun getProgress(movieId: String): Long {
        return getHistory().firstOrNull { it.movieId == movieId }?.progressMs ?: 0L
    }

    fun getHistory(): List<WatchHistoryEntry> {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                WatchHistoryEntry(
                    movieId     = obj.optString("movieId"),
                    title       = obj.optString("title"),
                    bannerUrl   = obj.optString("bannerUrl"),
                    category    = obj.optString("category"),
                    progressMs  = obj.optLong("progressMs"),
                    durationMs  = obj.optLong("durationMs"),
                    lastWatched = obj.optLong("lastWatched")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun getContinueWatching(): List<WatchHistoryEntry> {
        return getHistory().filter { it.progressPercent in 5..95 }
    }

    fun removeEntry(movieId: String) {
        val entries = getHistory().toMutableList()
        entries.removeAll { it.movieId == movieId }
        saveHistory(entries)
    }

    fun clearAll() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistory(entries: List<WatchHistoryEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("movieId",     e.movieId)
                put("title",       e.title)
                put("bannerUrl",   e.bannerUrl)
                put("category",    e.category)
                put("progressMs",  e.progressMs)
                put("durationMs",  e.durationMs)
                put("lastWatched", e.lastWatched)
            })
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }
}
