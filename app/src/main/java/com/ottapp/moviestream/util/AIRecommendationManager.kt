package com.ottapp.moviestream.util

import android.content.Context
import com.ottapp.moviestream.data.model.Movie

/**
 * AI Recommendation Engine
 * Watch history দেখে genre/category based movie suggest করে
 */
class AIRecommendationManager(context: Context) {

    private val watchHistoryManager = WatchHistoryManager(context)

    /**
     * সব movie থেকে top recommendations বের করে
     * Logic: user যে genre/category বেশি দেখে সেগুলো আগে দেখায়
     */
    fun getRecommendations(allMovies: List<Movie>, limit: Int = 10): List<Movie> {
        val history = watchHistoryManager.getHistory()
        if (history.isEmpty()) return allMovies.shuffled().take(limit)

        // ── Genre/Category score build করি ──────────────────────────
        val categoryScore = mutableMapOf<String, Int>()
        val genreScore    = mutableMapOf<String, Int>()
        val watchedIds    = history.map { it.movieId }.toSet()

        history.forEachIndexed { index, entry ->
            val recency = history.size - index  // সাম্প্রতিক দেখা বেশি গুরুত্বপূর্ণ
            val progress = entry.progressPercent
            val weight   = recency * (if (progress > 50) 2 else 1)

            categoryScore[entry.category] = (categoryScore[entry.category] ?: 0) + weight
        }

        // watched movies থেকে genre তুলি
        val watchedMovies = allMovies.filter { it.id in watchedIds }
        watchedMovies.forEachIndexed { index, movie ->
            val recency = watchedMovies.size - index
            movie.genreList.forEach { genre ->
                genreScore[genre] = (genreScore[genre] ?: 0) + recency
            }
        }

        // ── Score calculate করে sort করি ────────────────────────────
        val scored = allMovies
            .filter { it.id !in watchedIds }  // already watched বাদ দিই
            .map { movie ->
                val catScore   = categoryScore[movie.category] ?: 0
                val gScore     = movie.genreList.sumOf { genreScore[it] ?: 0 }
                val ratingBonus = (movie.imdbRating * 5).toInt()
                val trendBonus  = if (movie.trending) 20 else 0
                val total       = catScore * 3 + gScore * 2 + ratingBonus + trendBonus
                movie to total
            }
            .sortedByDescending { it.second }

        // ── যদি enough movie না থাকে trending add করি ───────────────
        val result = scored.map { it.first }.take(limit).toMutableList()
        if (result.size < limit) {
            val remaining = allMovies
                .filter { m -> m.id !in watchedIds && m.id !in result.map { it.id } }
                .filter { it.trending }
                .take(limit - result.size)
            result.addAll(remaining)
        }

        return result
    }

    /**
     * User এর top genres বের করে (Profile screen এ দেখানোর জন্য)
     */
    fun getTopGenres(allMovies: List<Movie>): List<String> {
        val history    = watchHistoryManager.getHistory()
        val watchedIds = history.map { it.movieId }.toSet()
        val genreScore = mutableMapOf<String, Int>()

        allMovies.filter { it.id in watchedIds }.forEach { movie ->
            movie.genreList.forEach { genre ->
                genreScore[genre] = (genreScore[genre] ?: 0) + 1
            }
        }

        return genreScore.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    /**
     * "Similar to what you watched" — একটি movie এর based on suggestions
     */
    fun getSimilarMovies(movie: Movie, allMovies: List<Movie>, limit: Int = 6): List<Movie> {
        return allMovies
            .filter { it.id != movie.id }
            .map { candidate ->
                val sameCategory = if (candidate.category == movie.category) 30 else 0
                val commonGenres = candidate.genreList.intersect(movie.genreList.toSet()).size * 20
                val ratingClose  = (10 - kotlin.math.abs(candidate.imdbRating - movie.imdbRating) * 2).toInt().coerceAtLeast(0)
                candidate to (sameCategory + commonGenres + ratingClose)
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}
