package com.ottapp.moviestream.data.repository

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.data.model.Banner
import kotlinx.coroutines.tasks.await

class BannerRepository {

    companion object {
        private const val TAG = "BannerRepository"
        private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"
    }

    private val db by lazy {
        FirebaseDatabase.getInstance(DB_URL).reference
    }

    private val bannersRef by lazy {
        db.child("banners")
    }

    private fun snapshotToBanner(snapshot: com.google.firebase.database.DataSnapshot): Banner? {
        return try {
            val rawValue = snapshot.value ?: return null
            val data: Map<*, *> = when (rawValue) {
                is Map<*, *> -> rawValue
                else -> return null
            }

            Banner(
                id         = snapshot.key ?: "",
                imageUrl   = data["imageUrl"]?.toString() ?: "",
                title      = data["title"]?.toString() ?: "",
                category   = data["category"]?.toString() ?: "",
                imdbRating = when (val r = data["imdbRating"]) {
                    is Double -> r
                    is Long   -> r.toDouble()
                    is Int    -> r.toDouble()
                    is String -> r.toDoubleOrNull() ?: 0.0
                    else      -> 0.0
                },
                testMovie  = when (val f = data["testMovie"]) {
                    is Boolean -> f
                    is String  -> f.equals("true", ignoreCase = true)
                    else       -> false
                },
                movieId    = data["movieId"]?.toString() ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse banner error: ${e.message}")
            null
        }
    }

    suspend fun getAllBanners(): List<Banner> {
        return try {
            val snapshot = bannersRef.get().await()
            snapshot.children.mapNotNull { child -> snapshotToBanner(child) }
        } catch (e: Exception) {
            Log.e(TAG, "getAllBanners error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun addBanner(banner: Banner): String {
        val newRef = bannersRef.push()
        val id = newRef.key ?: throw Exception("ID তৈরি হয়নি")
        val bannerWithId = banner.copy(id = id)
        newRef.setValue(bannerToMap(bannerWithId)).await()
        return id
    }

    suspend fun updateBanner(banner: Banner) {
        if (banner.id.isEmpty()) throw Exception("Banner ID নেই")
        bannersRef.child(banner.id).updateChildren(bannerToMap(banner)).await()
    }

    suspend fun deleteBanner(id: String) {
        if (id.isEmpty()) throw Exception("Banner ID নেই")
        bannersRef.child(id).removeValue().await()
    }

    private fun bannerToMap(banner: Banner): Map<String, Any?> = mapOf(
        "imageUrl"   to banner.imageUrl,
        "title"      to banner.title,
        "category"   to banner.category,
        "imdbRating" to banner.imdbRating,
        "testMovie"  to banner.testMovie,
        "movieId"    to banner.movieId
    )
}
