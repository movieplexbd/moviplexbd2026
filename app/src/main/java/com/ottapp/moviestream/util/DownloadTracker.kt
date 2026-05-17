package com.ottapp.moviestream.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that tracks active (in-progress) downloads.
 * DownloadService writes here; DownloadViewModel/Fragment reads here.
 */
object DownloadTracker {

    data class ActiveDownload(
        val movieId:   String,
        val title:     String,
        val bannerUrl: String,
        val progress:  Int,       // 0-100; -1 = indeterminate (unknown size)
        val sizeLabel: String = "" // e.g. "345.2 MB / 1.2 GB (28%)"
    )

    private val _active = MutableStateFlow<Map<String, ActiveDownload>>(emptyMap())
    val active: StateFlow<Map<String, ActiveDownload>> = _active.asStateFlow()

    fun start(movieId: String, title: String, bannerUrl: String) {
        _active.value = _active.value + (movieId to ActiveDownload(movieId, title, bannerUrl, -1))
    }

    fun update(movieId: String, progress: Int) {
        val current = _active.value[movieId] ?: return
        _active.value = _active.value + (movieId to current.copy(progress = progress))
    }

    fun updateProgress(movieId: String, progress: Int, sizeLabel: String = "") {
        val current = _active.value[movieId] ?: return
        _active.value = _active.value + (movieId to current.copy(progress = progress, sizeLabel = sizeLabel))
    }

    fun complete(movieId: String) {
        val current = _active.value[movieId] ?: return
        _active.value = _active.value + (movieId to current.copy(progress = 100))
    }

    fun error(movieId: String) {
        val current = _active.value[movieId] ?: return
        _active.value = _active.value + (movieId to current.copy(progress = -1))
    }

    fun remove(movieId: String) {
        _active.value = _active.value - movieId
    }

    fun isActive(movieId: String) = _active.value.containsKey(movieId)
}
