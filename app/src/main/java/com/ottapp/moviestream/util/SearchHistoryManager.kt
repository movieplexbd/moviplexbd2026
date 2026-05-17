package com.ottapp.moviestream.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Search history manager - সর্বশেষ ২০টি সার্চ locally save করে
 */
class SearchHistoryManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY = "search_queries"
        private const val MAX_ITEMS = 20
    }

    fun addQuery(query: String) {
        if (query.isBlank() || query.length < 2) return
        val list = getHistory().toMutableList()
        list.remove(query) // duplicate remove
        list.add(0, query.trim())
        if (list.size > MAX_ITEMS) list.removeAt(list.size - 1)
        saveList(list)
    }

    fun getHistory(): List<String> {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) { emptyList() }
    }

    fun removeQuery(query: String) {
        val list = getHistory().toMutableList()
        list.remove(query)
        saveList(list)
    }

    fun clearAll() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveList(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }
}
