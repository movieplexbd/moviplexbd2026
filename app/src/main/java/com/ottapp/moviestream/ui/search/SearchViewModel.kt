package com.ottapp.moviestream.ui.search

import android.app.Application
import androidx.lifecycle.*
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.data.model.MovieRequest
import com.ottapp.moviestream.data.repository.MovieRepository
import com.ottapp.moviestream.data.repository.RequestRepository
import com.ottapp.moviestream.util.Constants
import com.ottapp.moviestream.util.SearchHistoryManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MovieRepository()
    private val requestRepo = RequestRepository()
    val searchHistoryManager = SearchHistoryManager(application)

    private val _results = MutableLiveData<List<Movie>>(emptyList())
    val results: LiveData<List<Movie>> = _results

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _activeFilter = MutableLiveData(Constants.CAT_ALL)
    val activeFilter: LiveData<String> = _activeFilter

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _requestStatus = MutableLiveData<Boolean?>(null)
    val requestStatus: LiveData<Boolean?> = _requestStatus

    private val _trendingRequests = MutableLiveData<List<MovieRequest>>(emptyList())
    val trendingRequests: LiveData<List<MovieRequest>> = _trendingRequests

    // Search history LiveData
    private val _searchHistory = MutableLiveData<List<String>>(emptyList())
    val searchHistory: LiveData<List<String>> = _searchHistory

    private var allMovies: List<Movie> = emptyList()
    private var currentQuery = ""
    private var searchJob: Job? = null
    private var moviesLoaded = false

    init {
        loadMovies()
        refreshSearchHistory()
    }

    fun refreshSearchHistory() {
        _searchHistory.value = searchHistoryManager.getHistory()
    }

    fun saveSearchQuery(query: String) {
        if (query.isBlank() || query.length < 2) return
        searchHistoryManager.addQuery(query)
        refreshSearchHistory()
    }

    fun removeSearchQuery(query: String) {
        searchHistoryManager.removeQuery(query)
        refreshSearchHistory()
    }

    fun clearSearchHistory() {
        searchHistoryManager.clearAll()
        refreshSearchHistory()
    }

    private fun loadMovies() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                allMovies = repo.getAllMovies()
                moviesLoaded = allMovies.isNotEmpty()
                loadTrendingRequests()
                if (currentQuery.isNotBlank()) search(currentQuery)
            } catch (e: Exception) {
                _error.value = "নেটওয়ার্ক সমস্যা। পুনরায় চেষ্টা করুন।"
                allMovies = emptyList()
                moviesLoaded = false
            } finally {
                _loading.value = false
            }
        }
    }

    fun retryLoadMovies() {
        if (!moviesLoaded) loadMovies()
    }

    private fun loadTrendingRequests() {
        viewModelScope.launch {
            try {
                val requests = requestRepo.getAllRequests()
                _trendingRequests.value = requests.filter { it.count >= 5 }.sortedByDescending { it.count }
            } catch (e: Exception) { /* silent fail */ }
        }
    }

    fun search(query: String) {
        currentQuery = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _results.value = emptyList()
            _loading.value = false
            return
        }

        if (allMovies.isEmpty() && !moviesLoaded) {
            loadMovies()
            return
        }

        searchJob = viewModelScope.launch {
            _loading.value = true
            try {
                delay(300)
                val q = query.lowercase().trim()
                val filtered = allMovies.filter { movie ->
                    val title = movie.title.orEmpty().lowercase()
                    val desc  = movie.description.orEmpty().lowercase()
                    val cat   = movie.category.orEmpty().lowercase()
                    if (title.isEmpty() && desc.isEmpty() && cat.isEmpty()) return@filter false
                    val words = q.split("\\s+".toRegex()).filter { it.length >= 2 }
                    val titleMatch = title.contains(q)
                    val wordMatch  = words.isNotEmpty() && words.all { w ->
                        title.contains(w) || desc.contains(w) || cat.contains(w)
                    }
                    titleMatch || wordMatch || desc.contains(q) || cat.contains(q)
                }
                applyFilter(filtered)
            } catch (e: Exception) {
                _results.value = emptyList()
                _error.value = "সার্চে সমস্যা হয়েছে।"
            } finally {
                _loading.value = false
            }
        }
    }

    fun submitRequest(movieTitle: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val success = requestRepo.submitRequest(movieTitle)
                _requestStatus.value = success
                if (success) loadTrendingRequests()
            } catch (e: Exception) {
                _requestStatus.value = false
            } finally {
                _loading.value = false
            }
        }
    }

    fun resetRequestStatus() { _requestStatus.value = null }

    fun setFilter(cat: String) {
        _activeFilter.value = cat
        val q = currentQuery.lowercase()
        val base = if (currentQuery.isBlank()) allMovies else
            allMovies.filter {
                it.title.orEmpty().lowercase().contains(q) ||
                it.description.orEmpty().lowercase().contains(q) ||
                it.category.orEmpty().lowercase().contains(q)
            }
        applyFilter(base)
    }

    private fun applyFilter(list: List<Movie>) {
        val cat = _activeFilter.value ?: Constants.CAT_ALL
        _results.value = when (cat) {
            Constants.CAT_ALL      -> list
            Constants.CAT_TRENDING -> list.filter { it.trending }
            else -> list.filter { movie ->
                val c = movie.category.orEmpty().lowercase().trim()
                val s = cat.lowercase().trim()
                c == s || c.contains(s) || s.contains(c)
            }
        }
    }
}
