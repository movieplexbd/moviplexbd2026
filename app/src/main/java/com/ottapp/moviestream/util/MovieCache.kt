package com.ottapp.moviestream.util

import android.content.Context
import android.content.SharedPreferences
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.data.model.DownloadQuality
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent LRU cache for movie list and individual movie data.
 * Used for offline support: if network fails, serve cached data.
 * Cache TTL: 6 hours for fresh re-fetch, otherwise serve stale.
 */
object MovieCache {

    private const val PREFS_NAME     = "movie_cache"
    private const val KEY_MOVIES     = "all_movies_json"
    private const val KEY_MOVIE_TS   = "all_movies_ts"
    private const val KEY_MOVIE_PFX  = "movie_"
    private const val KEY_BANNERS    = "all_banners_json"
    private const val KEY_BANNER_TS  = "all_banners_ts"
    private const val CACHE_TTL_MS   = 6L * 60L * 60L * 1000L // 6 hours

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /* ── Save / load banner list ── */

    fun saveBanners(ctx: Context, banners: List<com.ottapp.moviestream.data.model.Banner>) {
        val arr = JSONArray()
        banners.forEach { b ->
            arr.put(JSONObject().apply {
                put("id", b.id)
                put("imageUrl", b.imageUrl)
                put("title", b.title)
                put("category", b.category)
                put("imdbRating", b.imdbRating)
                put("testMovie", b.testMovie)
                put("movieId", b.movieId)
            })
        }
        prefs(ctx).edit()
            .putString(KEY_BANNERS, arr.toString())
            .putLong(KEY_BANNER_TS, System.currentTimeMillis())
            .apply()
    }

    fun loadBanners(ctx: Context): List<com.ottapp.moviestream.data.model.Banner>? {
        val json = prefs(ctx).getString(KEY_BANNERS, null) ?: return null
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val j = arr.getJSONObject(i)
                com.ottapp.moviestream.data.model.Banner(
                    id         = j.optString("id"),
                    imageUrl   = j.optString("imageUrl"),
                    title      = j.optString("title"),
                    category   = j.optString("category"),
                    imdbRating = j.optDouble("imdbRating", 0.0),
                    testMovie  = j.optBoolean("testMovie", false),
                    movieId    = j.optString("movieId")
                )
            }
        } catch (_: Exception) { null }
    }

    /* ── Save / load movie list ── */

    fun saveMovies(ctx: Context, movies: List<Movie>) {
        val arr = JSONArray()
        movies.forEach { arr.put(movieToJson(it)) }
        prefs(ctx).edit()
            .putString(KEY_MOVIES, arr.toString())
            .putLong(KEY_MOVIE_TS, System.currentTimeMillis())
            .apply()
    }

    fun loadMovies(ctx: Context): List<Movie>? {
        val p = prefs(ctx)
        val json = p.getString(KEY_MOVIES, null) ?: return null
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { jsonToMovie(arr.getJSONObject(it)) }
        } catch (_: Exception) { null }
    }

    fun isFresh(ctx: Context): Boolean {
        val ts = prefs(ctx).getLong(KEY_MOVIE_TS, 0L)
        return System.currentTimeMillis() - ts < CACHE_TTL_MS
    }

    /* ── Save / load single movie ── */

    fun saveMovie(ctx: Context, movie: Movie) {
        prefs(ctx).edit().putString("$KEY_MOVIE_PFX${movie.id}", movieToJson(movie).toString()).apply()
    }

    fun loadMovie(ctx: Context, id: String): Movie? {
        val json = prefs(ctx).getString("$KEY_MOVIE_PFX$id", null) ?: return null
        return try { jsonToMovie(JSONObject(json)) } catch (_: Exception) { null }
    }

    /* ── JSON serialisation ── */

    private fun movieToJson(m: Movie): JSONObject = JSONObject().apply {
        put("id", m.id)
        put("title", m.title)
        put("description", m.description)
        put("bannerImageUrl", m.bannerImageUrl)
        put("detailThumbnailUrl", m.detailThumbnailUrl)
        put("videoStreamUrl", m.videoStreamUrl)
        put("downloadUrl", m.downloadUrl)
        put("category", m.category)
        put("imdbRating", m.imdbRating)
        put("year", m.year)
        put("duration", m.duration)
        put("trending", m.trending)
        put("testMovie", m.testMovie)
        put("actorIds", JSONArray(m.actorIds))
        val dlArr = JSONArray()
        m.downloads.forEach { dl ->
            dlArr.put(JSONObject().apply {
                put("quality", dl.quality)
                put("url", dl.url)
                put("size", dl.size)
            })
        }
        put("downloads", dlArr)
    }

    private fun jsonToMovie(j: JSONObject): Movie? = try {
        val actorIds = mutableListOf<String>()
        val actorArr = j.optJSONArray("actorIds")
        if (actorArr != null) {
            for (i in 0 until actorArr.length()) actorIds.add(actorArr.optString(i))
        }
        val downloads = mutableListOf<DownloadQuality>()
        val dlArr = j.optJSONArray("downloads")
        if (dlArr != null) {
            for (i in 0 until dlArr.length()) {
                val dl = dlArr.getJSONObject(i)
                downloads.add(DownloadQuality(
                    quality = dl.optString("quality"),
                    url     = dl.optString("url"),
                    size    = dl.optString("size")
                ))
            }
        }
        Movie(
            id                 = j.optString("id"),
            title              = j.optString("title"),
            description        = j.optString("description"),
            bannerImageUrl     = j.optString("bannerImageUrl"),
            detailThumbnailUrl = j.optString("detailThumbnailUrl"),
            videoStreamUrl     = j.optString("videoStreamUrl"),
            downloadUrl        = j.optString("downloadUrl"),
            category           = j.optString("category"),
            imdbRating         = j.optDouble("imdbRating", 0.0),
            year               = j.optInt("year", 0),
            duration           = j.optString("duration"),
            trending           = j.optBoolean("trending", false),
            testMovie          = j.optBoolean("testMovie", false),
            actorIds           = actorIds,
            downloads          = downloads
        )
    } catch (_: Exception) { null }
}
