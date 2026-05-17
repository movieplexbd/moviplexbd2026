package com.ottapp.moviestream.data.repository

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.data.model.Movie
import com.ottapp.moviestream.data.model.DownloadQuality
import com.ottapp.moviestream.data.model.Season
import com.ottapp.moviestream.data.model.Episode
import kotlinx.coroutines.tasks.await

class MovieRepository {

    companion object {
        private const val TAG = "MovieRepository"
        private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"
    }

    private val db by lazy { FirebaseDatabase.getInstance(DB_URL).reference }
    private val moviesRef by lazy { db.child("movies") }

    @Suppress("UNCHECKED_CAST")
    private fun snapshotToMovie(snapshot: com.google.firebase.database.DataSnapshot): Movie? {
        return try {
            val rawValue = snapshot.value ?: return null
            val data: Map<*, *> = rawValue as? Map<*, *> ?: return null

            // ── Download qualities ────────────────────────────────────────────
            val downloadsList = mutableListOf<DownloadQuality>()
            (data["downloads"] as? List<*>)?.forEach { item ->
                (item as? Map<*, *>)?.let { map ->
                    downloadsList.add(DownloadQuality(
                        quality = map["quality"]?.toString() ?: "",
                        url     = map["url"]?.toString() ?: "",
                        size    = map["size"]?.toString() ?: ""
                    ))
                }
            }

            // ── Genres ────────────────────────────────────────────────────────
            val genresList = (data["genres"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val genreStr   = data["genre"]?.toString() ?: ""

            // ── Seasons / Episodes ────────────────────────────────────────────
            val seasonsList = mutableListOf<Season>()
            (data["seasons"] as? List<*>)?.forEachIndexed { sIdx, seasonRaw ->
                (seasonRaw as? Map<*, *>)?.let { sMap ->
                    val episodesList = mutableListOf<Episode>()
                    (sMap["episodes"] as? List<*>)?.forEachIndexed { eIdx, epRaw ->
                        (epRaw as? Map<*, *>)?.let { eMap ->
                            episodesList.add(Episode(
                                episodeNumber = (eMap["episodeNumber"] as? Long)?.toInt() ?: (eIdx + 1),
                                title         = eMap["title"]?.toString() ?: "Episode ${eIdx + 1}",
                                description   = eMap["description"]?.toString() ?: "",
                                duration      = eMap["duration"]?.toString() ?: "",
                                thumbnailUrl  = eMap["thumbnailUrl"]?.toString() ?: "",
                                streamUrl     = eMap["streamUrl"]?.toString() ?: eMap["videoStreamUrl"]?.toString() ?: "",
                                downloadUrl   = eMap["downloadUrl"]?.toString() ?: "",
                                isFree        = when (val f = eMap["isFree"] ?: eMap["testMovie"]) {
                                    is Boolean -> f; is String -> f.equals("true", true); else -> false
                                }
                            ))
                        }
                    }
                    seasonsList.add(Season(
                        seasonNumber = (sMap["seasonNumber"] as? Long)?.toInt() ?: (sIdx + 1),
                        title        = sMap["title"]?.toString() ?: "Season ${sIdx + 1}",
                        episodes     = episodesList
                    ))
                }
            }

            Movie(
                id                 = snapshot.key ?: "",
                title              = data["title"]?.toString() ?: "",
                description        = data["description"]?.toString() ?: "",
                bannerImageUrl     = data["bannerImageUrl"]?.toString()
                    ?: data["imageUrl"]?.toString()
                    ?: data["banner"]?.toString() ?: "",
                detailThumbnailUrl = data["detailThumbnailUrl"]?.toString() ?: "",
                videoStreamUrl     = data["videoStreamUrl"]?.toString()
                    ?: data["streamUrl"]?.toString()
                    ?: data["videoUrl"]?.toString() ?: "",
                downloadUrl        = data["downloadUrl"]?.toString() ?: "",
                category           = data["category"]?.toString() ?: "",
                imdbRating         = when (val r = data["imdbRating"] ?: data["rating"]) {
                    is Double -> r; is Long -> r.toDouble(); is Int -> r.toDouble()
                    is String -> r.toDoubleOrNull() ?: 0.0; else -> 0.0
                },
                year               = when (val y = data["year"]) {
                    is Long -> y.toInt(); is Int -> y; is Double -> y.toInt()
                    is String -> y.toIntOrNull() ?: 0; else -> 0
                },
                duration           = data["duration"]?.toString() ?: "",
                trending           = when (val t = data["trending"]) {
                    is Boolean -> t; is String -> t.equals("true", true); else -> false
                },
                testMovie          = when (val f = data["testMovie"] ?: data["isFree"]) {
                    is Boolean -> f; is String -> f.equals("true", true); else -> false
                },
                actorIds           = (data["actorIds"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                downloads          = downloadsList,
                genre              = genreStr,
                genres             = genresList,
                trailerUrl         = data["trailerUrl"]?.toString() ?: data["trailer"]?.toString() ?: "",
                isSeries           = when (val s = data["isSeries"] ?: data["series"]) {
                    is Boolean -> s; is String -> s.equals("true", true); else -> false
                },
                totalSeasons       = when (val ts = data["totalSeasons"]) {
                    is Long -> ts.toInt(); is Int -> ts; else -> seasonsList.size
                },
                seasons            = seasonsList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse movie error: ${e.message}")
            null
        }
    }

    suspend fun getAllMovies(): List<Movie> {
        return try {
            val snapshot = moviesRef.get().await()
            snapshot.children.mapNotNull { snapshotToMovie(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getAllMovies error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getMovieById(id: String): Movie? {
        if (id.isEmpty()) return null
        return try {
            val snapshot = moviesRef.child(id).get().await()
            snapshotToMovie(snapshot)
        } catch (e: Exception) {
            Log.e(TAG, "getMovieById error: ${e.message}", e)
            null
        }
    }

    suspend fun getMoviesByGenre(genre: String): List<Movie> {
        return getAllMovies().filter { movie ->
            movie.genreList.any { it.equals(genre, ignoreCase = true) }
        }
    }

    suspend fun getRelatedMovies(movie: Movie, limit: Int = 10): List<Movie> {
        val all = getAllMovies()
        return all.filter { m ->
            m.id != movie.id && (
                m.category.equals(movie.category, ignoreCase = true) ||
                m.genreList.any { g -> movie.genreList.any { it.equals(g, ignoreCase = true) } }
            )
        }.sortedByDescending { it.imdbRating }.take(limit)
    }

    suspend fun addMovie(movie: Movie): String {
        val newRef = moviesRef.push()
        val id = newRef.key ?: throw Exception("ID তৈরি হয়নি")
        newRef.setValue(movieToMap(movie.copy(id = id))).await()
        return id
    }

    suspend fun updateMovie(movie: Movie) {
        if (movie.id.isEmpty()) throw Exception("Movie ID নেই")
        moviesRef.child(movie.id).updateChildren(movieToMap(movie)).await()
    }

    suspend fun deleteMovie(id: String) {
        if (id.isEmpty()) throw Exception("Movie ID নেই")
        moviesRef.child(id).removeValue().await()
    }

    private fun movieToMap(movie: Movie): Map<String, Any?> = mapOf(
        "title"              to movie.title,
        "description"        to movie.description,
        "bannerImageUrl"     to movie.bannerImageUrl,
        "detailThumbnailUrl" to movie.detailThumbnailUrl,
        "videoStreamUrl"     to movie.videoStreamUrl,
        "downloadUrl"        to movie.downloadUrl,
        "category"           to movie.category,
        "imdbRating"         to movie.imdbRating,
        "year"               to movie.year,
        "duration"           to movie.duration,
        "trending"           to movie.trending,
        "testMovie"          to movie.testMovie,
        "actorIds"           to movie.actorIds,
        "downloads"          to movie.downloads.map { mapOf("quality" to it.quality, "url" to it.url, "size" to it.size) },
        "genre"              to movie.genre,
        "genres"             to movie.genres,
        "trailerUrl"         to movie.trailerUrl,
        "isSeries"           to movie.isSeries,
        "totalSeasons"       to movie.totalSeasons,
        "seasons"            to movie.seasons.map { season ->
            mapOf(
                "seasonNumber" to season.seasonNumber,
                "title"        to season.title,
                "episodes"     to season.episodes.map { ep ->
                    mapOf(
                        "episodeNumber" to ep.episodeNumber,
                        "title"         to ep.title,
                        "description"   to ep.description,
                        "duration"      to ep.duration,
                        "thumbnailUrl"  to ep.thumbnailUrl,
                        "streamUrl"     to ep.streamUrl,
                        "downloadUrl"   to ep.downloadUrl,
                        "isFree"        to ep.isFree
                    )
                }
            )
        }
    )
}
