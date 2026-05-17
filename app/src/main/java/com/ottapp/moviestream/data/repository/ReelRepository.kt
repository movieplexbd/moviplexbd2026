package com.ottapp.moviestream.data.repository

import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.ottapp.moviestream.data.model.Reel
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReelRepository @Inject constructor(
    private val db: DatabaseReference
) {

    companion object {
        private const val TAG = "ReelRepository"
    }

    private val reelsRef by lazy {
        db.child("reels")
    }

    @Suppress("UNCHECKED_CAST")
    private fun snapshotToReel(snapshot: com.google.firebase.database.DataSnapshot): Reel? {
        return try {
            val rawValue = snapshot.value ?: return null
            val data: Map<*, *> = when (rawValue) {
                is Map<*, *> -> rawValue
                else -> return null
            }

            Reel(
                id         = snapshot.key ?: "",
                title      = data["title"]?.toString() ?: "",
                videoUrl   = data["videoUrl"]?.toString() ?: "",
                movieTitle = data["movieTitle"]?.toString() ?: "",
                movieId    = data["movieId"]?.toString() ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse reel error: ${e.message}")
            null
        }
    }

    suspend fun getAllReels(): List<Reel> {
        return try {
            val snapshot = reelsRef.get().await()
            snapshot.children.mapNotNull { child -> snapshotToReel(child) }
        } catch (e: Exception) {
            Log.e(TAG, "getAllReels error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun addReel(reel: Reel): String {
        val newRef = reelsRef.push()
        val id = newRef.key ?: throw Exception("ID তৈরি হয়নি")
        val reelWithId = reel.copy(id = id)
        newRef.setValue(reelToMap(reelWithId)).await()
        return id
    }

    suspend fun updateReel(reel: Reel) {
        if (reel.id.isEmpty()) throw Exception("Reel ID নেই")
        reelsRef.child(reel.id).updateChildren(reelToMap(reel)).await()
    }

    suspend fun deleteReel(id: String) {
        if (id.isEmpty()) throw Exception("Reel ID নেই")
        reelsRef.child(id).removeValue().await()
    }

    private fun reelToMap(reel: Reel): Map<String, Any?> = mapOf(
        "title"      to reel.title,
        "videoUrl"   to reel.videoUrl,
        "movieTitle" to reel.movieTitle,
        "movieId"    to reel.movieId
    )
}
