package com.ottapp.moviestream.ui.movies

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.data.model.User
import com.ottapp.moviestream.data.repository.MovieRepository
import com.ottapp.moviestream.data.repository.UserRepository
import com.ottapp.moviestream.util.Constants
import com.ottapp.moviestream.util.MovieCache
import kotlinx.coroutines.launch

class MoviesViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repo     = MovieRepository()
    private val userRepo = UserRepository()
    private val ctx      = app.applicationContext

    private val allMovies       = mutableListOf<Movie>()
    private val displayedMovies = mutableListOf<Movie>()

    private val _filteredMovies = MutableLiveData<List<Movie>>(emptyList())
    val filteredMovies: LiveData<List<Movie>> = _filteredMovies

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _currentUser = MutableLiveData<User?>(null)
    val currentUser: LiveData<User?> = _currentUser

    private var selectedCategory = Constants.CAT_ALL

    // Pagination
    private val pageSize    = 24
    private var pageOffset  = 0
    private var isLoadingMore = false
    private var hasMore     = true

    init { loadMovies() }

    fun loadMovies() {
        viewModelScope.launch {
            _loading.value = true
            _error.value   = null

            // Load user first so lock icons are correct from the start
            try {
                _currentUser.value = userRepo.getCurrentUser()
            } catch (e: Exception) {
                Log.e("MoviesViewModel", "getUser error: ${e.message}")
                _currentUser.value = null
            }

            // Show cache immediately
            val cached = MovieCache.loadMovies(ctx)
            if (cached != null) {
                allMovies.clear()
                allMovies.addAll(cached)
                resetPagination()
                if (MovieCache.isFresh(ctx)) {
                    _loading.value = false
                    return@launch
                }
            }

            try {
                val movies = repo.getAllMovies()
                allMovies.clear()
                allMovies.addAll(movies)
                if (movies.isNotEmpty()) MovieCache.saveMovies(ctx, movies)
                resetPagination()
            } catch (e: Exception) {
                Log.e("MoviesViewModel", "loadMovies error: ${e.message}", e)
                _error.value = when {
                    e is java.net.UnknownHostException -> "ইন্টারনেট সংযোগ নেই। WiFi বা ডেটা চেক করুন।"
                    e is java.net.SocketTimeoutException -> "সার্ভার সাড়া দিচ্ছে না। পরে চেষ্টা করুন।"
                    e.message?.contains("network", ignoreCase = true) == true -> "নেটওয়ার্ক সমস্যা হয়েছে।"
                    else -> "কিছু একটা সমস্যা হয়েছে। আবার চেষ্টা করুন।"
                }
                if (allMovies.isEmpty()) _filteredMovies.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun setCategory(cat: String) {
        selectedCategory = cat
        resetPagination()
    }

    /** Called by adapter when near the end of the list */
    fun loadNextPage() {
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true

        val filtered = filteredFrom(allMovies)
        val end = minOf(pageOffset + pageSize, filtered.size)
        if (pageOffset >= filtered.size) {
            hasMore = false
            isLoadingMore = false
            return
        }

        displayedMovies.addAll(filtered.subList(pageOffset, end))
        pageOffset = end
        hasMore = pageOffset < filtered.size
        _filteredMovies.value = displayedMovies.toList()
        isLoadingMore = false
    }

    private fun resetPagination() {
        pageOffset = 0
        hasMore = true
        isLoadingMore = false
        displayedMovies.clear()
        loadNextPage()
    }

    private fun filteredFrom(all: List<Movie>): List<Movie> = when (selectedCategory) {
        Constants.CAT_ALL      -> all
        Constants.CAT_TRENDING -> all.filter { it.trending }
        else                   -> all.filter { movie ->
            val cat = movie.category.lowercase().trim()
            val sel = selectedCategory.lowercase().trim()
            cat == sel || cat.contains(sel) || sel.contains(cat)
        }
    }
}
