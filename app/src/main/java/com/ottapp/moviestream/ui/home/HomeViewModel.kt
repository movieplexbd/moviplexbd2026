package com.ottapp.moviestream.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.data.model.User
import com.ottapp.moviestream.util.WatchHistoryManager
import com.ottapp.moviestream.util.WatchHistoryEntry
import com.ottapp.moviestream.util.MovieCache
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ottapp.moviestream.data.model.Banner
import com.ottapp.moviestream.data.repository.BannerRepository
import com.ottapp.moviestream.data.repository.MovieRepository
import com.ottapp.moviestream.data.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.ottapp.moviestream.util.NewContentNotificationManager
import com.ottapp.moviestream.util.AIRecommendationManager

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val watchHistoryManager = WatchHistoryManager(app.applicationContext)
    private val ctx = app.applicationContext

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val movieRepo = MovieRepository()
    private val userRepo = UserRepository()
    private val bannerRepo = BannerRepository()

    private val _continueWatching = MutableLiveData<List<WatchHistoryEntry>>(emptyList())
    val continueWatching: LiveData<List<WatchHistoryEntry>> = _continueWatching

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    private val _banners = MutableLiveData<List<Banner>>(emptyList())
    val banners: LiveData<List<Banner>> = _banners

    private val _trendingMovies = MutableLiveData<List<Movie>>(emptyList())
    val trendingMovies: LiveData<List<Movie>> = _trendingMovies

    private val _banglaMovies = MutableLiveData<List<Movie>>(emptyList())
    val banglaMovies: LiveData<List<Movie>> = _banglaMovies

    private val _hindiMovies = MutableLiveData<List<Movie>>(emptyList())
    val hindiMovies: LiveData<List<Movie>> = _hindiMovies

    private val _allMovies = MutableLiveData<List<Movie>>(emptyList())
    val allMovies: LiveData<List<Movie>> = _allMovies

    private val _currentUser = MutableLiveData<User?>(null)
    val currentUser: LiveData<User?> = _currentUser

    private val _availableGenres = MutableLiveData<List<String>>(emptyList())
    val availableGenres: LiveData<List<String>> = _availableGenres

    private val _genreFilteredMovies = MutableLiveData<List<Movie>>(emptyList())
    val genreFilteredMovies: LiveData<List<Movie>> = _genreFilteredMovies

    private var selectedGenre: String = ""
    private var cachedAllMovies: List<Movie> = emptyList()

    private val _networkError = MutableLiveData<Boolean>(false)
    val networkError: LiveData<Boolean> = _networkError

    private val _aiRecommendations = MutableLiveData<List<Movie>>(emptyList())
    val aiRecommendations: LiveData<List<Movie>> = _aiRecommendations

    private val aiManager by lazy { AIRecommendationManager(app.applicationContext) }

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _loading.value = true

            // 1. Load user FIRST so premium status is known before movies render
            //    This prevents lock icons showing for premium users even briefly
            _continueWatching.value = watchHistoryManager.getContinueWatching()
            safeGetUser()

            // 2. Serve cache immediately if available (offline-first)
            val cachedMovies = MovieCache.loadMovies(ctx)
            val cachedBanners = MovieCache.loadBanners(ctx)

            if (cachedMovies != null) {
                applyMovieLists(cachedMovies)
            }
            if (cachedBanners != null) {
                _banners.value = cachedBanners
            }

            // If cache is fresh, skip network
            if (cachedMovies != null && cachedBanners != null && MovieCache.isFresh(ctx)) {
                _loading.value = false
                return@launch
            }

            try {
                val all = safeGetMovies()
                if (all.isNotEmpty()) {
                    _networkError.value = false
                    MovieCache.saveMovies(ctx, all)
                    applyMovieLists(all)
                    // Check for new content and notify user
                    try {
                        val latestTitle = all.maxByOrNull { it.id }?.title ?: ""
                        NewContentNotificationManager.checkAndNotifyNewContent(ctx, all.size, latestTitle)
                    } catch (_: Exception) {}
                }

                val banners = bannerRepo.getAllBanners()
                if (banners.isNotEmpty()) {
                    MovieCache.saveBanners(ctx, banners)
                }
                _banners.value = banners

            } catch (e: Exception) {
                Log.e(TAG, "loadData error: ${e.message}", e)
                // Show network error only if we have no cached data
                if (_allMovies.value.isNullOrEmpty()) {
                    _networkError.value = true
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun filterByGenre(genre: String) {
        selectedGenre = genre
        val filtered = if (genre.isBlank()) emptyList()
            else cachedAllMovies.filter { m -> m.genreList.any { it.equals(genre, ignoreCase = true) } }
        _genreFilteredMovies.value = filtered
    }

    private fun applyMovieLists(all: List<Movie>) {
        cachedAllMovies = all
        // Extract unique genres
        val genres = all.flatMap { it.genreList }.distinct().filter { it.isNotBlank() }.sorted()
        _availableGenres.value = genres
        // Re-apply genre filter if one is selected
        if (selectedGenre.isNotBlank()) filterByGenre(selectedGenre)
        _allMovies.value = all
        _trendingMovies.value = all.filter { it.trending }

        // AI Recommendations — watch history based
        val recommendations = aiManager.getRecommendations(all, limit = 12)
        _aiRecommendations.value = recommendations
        _banglaMovies.value = all.filter { m ->
            m.category.lowercase().let { it.contains("bangla") || it.contains("বাংলা") }
        }
        _hindiMovies.value = all.filter { m ->
            m.category.lowercase().let { it.contains("hindi") || it.contains("হিন্দি") }
        }
    }

    private suspend fun safeGetMovies(): List<Movie> {
        return try {
            withTimeoutOrNull(12000) { movieRepo.getAllMovies() } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getAllMovies error: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun safeGetUser() {
        try {
            val user = userRepo.getCurrentUser()
            _currentUser.value = user
        } catch (e: Exception) {
            Log.e(TAG, "getUser error: ${e.message}", e)
            _currentUser.value = null
        }
    }
}
