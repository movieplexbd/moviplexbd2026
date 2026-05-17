package com.ottapp.moviestream.data.repository

import android.content.Context
import com.ottapp.moviestream.data.model.DownloadedMovie
import org.json.JSONObject
import java.io.File

class DownloadRepository(private val context: Context) {

    private val moviesDir: File
        get() = File(context.filesDir, "movies").also { it.mkdirs() }

    private val metaDir: File
        get() = File(context.filesDir, "meta").also { it.mkdirs() }

    fun isDownloaded(movieId: String): Boolean {
        return File(moviesDir, "$movieId.mp4").exists() &&
               File(metaDir,   "$movieId.json").exists()
    }

    fun getLocalFilePath(movieId: String): String =
        File(moviesDir, "$movieId.mp4").absolutePath

    fun saveDownload(movie: DownloadedMovie) {
        val json = JSONObject().apply {
            put("movieId",        movie.movieId)
            put("title",          movie.title)
            put("bannerImageUrl", movie.bannerImageUrl)
            put("localFilePath",  movie.localFilePath)
            put("fileSize",       movie.fileSize)
            put("fileSizeBytes",  movie.fileSizeBytes)
            put("downloadedAt",   movie.downloadedAt)
            put("imdbRating",     movie.imdbRating.toDouble())
            put("category",       movie.category)
            put("duration",       movie.duration)
        }
        File(metaDir, "${movie.movieId}.json").writeText(json.toString())
    }

    // Legacy alias
    fun saveMetadata(movie: DownloadedMovie) = saveDownload(movie)

    fun getAllDownloads(): List<DownloadedMovie> {
        val files = metaDir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { f ->
            try {
                val j = JSONObject(f.readText())
                val m = DownloadedMovie(
                    movieId        = j.getString("movieId"),
                    title          = j.getString("title"),
                    bannerImageUrl = j.optString("bannerImageUrl"),
                    localFilePath  = j.getString("localFilePath"),
                    fileSize       = j.optString("fileSize", ""),
                    fileSizeBytes  = j.optLong("fileSizeBytes", 0L),
                    downloadedAt   = j.getLong("downloadedAt"),
                    imdbRating     = j.optDouble("imdbRating", 0.0).toFloat(),
                    category       = j.optString("category"),
                    duration       = j.optString("duration")
                )
                if (File(moviesDir, "${m.movieId}.mp4").exists()) m else null
            } catch (e: Exception) { null }
        }.sortedByDescending { it.downloadedAt }
    }

    fun deleteDownload(movieId: String): Boolean {
        File(moviesDir, "$movieId.mp4").delete()
        File(metaDir,   "$movieId.json").delete()
        return !File(moviesDir, "$movieId.mp4").exists()
    }

    fun getTotalStorageUsed(): Long =
        moviesDir.listFiles()?.sumOf { it.length() } ?: 0L

    fun getTempFile(movieId: String): File =
        File(moviesDir, "$movieId.tmp")

    fun getFinalFile(movieId: String): File =
        File(moviesDir, "$movieId.mp4")

    fun finalizeTempFile(movieId: String): Boolean =
        File(moviesDir, "$movieId.tmp").renameTo(File(moviesDir, "$movieId.mp4"))
}
