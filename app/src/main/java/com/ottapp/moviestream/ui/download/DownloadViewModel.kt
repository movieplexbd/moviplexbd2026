package com.ottapp.moviestream.ui.download

import android.app.Application
import androidx.lifecycle.*
import com.ottapp.moviestream.data.model.DownloadedMovie
import com.ottapp.moviestream.data.repository.DownloadRepository
import com.ottapp.moviestream.util.DownloadTracker
import com.ottapp.moviestream.util.toReadableSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DownloadRepository(app)

    // ── Completed downloads ───────────────────────────────────────────────────
    private val _downloads = MutableLiveData<List<DownloadedMovie>>()
    val downloads: LiveData<List<DownloadedMovie>> = _downloads

    private val _storageUsed = MutableLiveData<String>()
    val storageUsed: LiveData<String> = _storageUsed

    // ── Active (in-progress) downloads — from DownloadTracker ────────────────
    val activeDownloads: LiveData<Map<String, DownloadTracker.ActiveDownload>> =
        DownloadTracker.active.asLiveData()

    init { loadDownloads() }

    fun loadDownloads() = viewModelScope.launch {
        val list  = withContext(Dispatchers.IO) { repo.getAllDownloads() }
        val bytes = withContext(Dispatchers.IO) { repo.getTotalStorageUsed() }
        _downloads.value   = list
        _storageUsed.value = bytes.toReadableSize()
    }

    fun deleteDownload(movieId: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) { repo.deleteDownload(movieId) }
        loadDownloads()
    }

    fun isDownloaded(movieId: String) = repo.isDownloaded(movieId)
    fun getLocalPath(movieId: String)  = repo.getLocalFilePath(movieId)
}
