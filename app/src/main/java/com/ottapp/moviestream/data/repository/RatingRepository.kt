package com.ottapp.moviestream.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ottapp.moviestream.data.model.MovieRating
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RatingRepository {

    companion object {
        private const val TAG = "RatingRepository"
        private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance(DB_URL).reference }

    suspend fun submitRating(movieId: String, rating: Float, review: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val userName = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "ব্যবহারকারী"
        return try {
            val ratingId = "${movieId}_${uid}"
            val ratingObj = MovieRating(
                id = ratingId,
                movieId = movieId,
                userId = uid,
                userName = userName,
                rating = rating,
                review = review,
                timestamp = System.currentTimeMillis()
            )
            db.child("ratings").child(movieId).child(uid).setValue(ratingObj).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "submitRating error: ${e.message}", e)
            false
        }
    }

    suspend fun getUserRating(movieId: String): MovieRating? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snap = db.child("ratings").child(movieId).child(uid).get().await()
            if (!snap.exists()) return null
            val data = snap.value as? Map<*, *> ?: return null
            MovieRating(
                id = data["id"]?.toString() ?: "",
                movieId = data["movieId"]?.toString() ?: movieId,
                userId = data["userId"]?.toString() ?: uid,
                userName = data["userName"]?.toString() ?: "",
                rating = data["rating"]?.toString()?.toFloatOrNull() ?: 0f,
                review = data["review"]?.toString() ?: "",
                timestamp = data["timestamp"]?.toString()?.toLongOrNull() ?: 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "getUserRating error: ${e.message}", e)
            null
        }
    }

    fun getRatingsFlow(movieId: String): Flow<List<MovieRating>> = callbackFlow {
        val ref = db.child("ratings").child(movieId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<MovieRating>()
                for (child in snapshot.children) {
                    try {
                        val data = child.value as? Map<*, *> ?: continue
                        list.add(MovieRating(
                            id = data["id"]?.toString() ?: "",
                            movieId = data["movieId"]?.toString() ?: movieId,
                            userId = data["userId"]?.toString() ?: "",
                            userName = data["userName"]?.toString() ?: "",
                            rating = data["rating"]?.toString()?.toFloatOrNull() ?: 0f,
                            review = data["review"]?.toString() ?: "",
                            timestamp = data["timestamp"]?.toString()?.toLongOrNull() ?: 0L
                        ))
                    } catch (_: Exception) {}
                }
                trySend(list.sortedByDescending { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "ratings flow cancelled: ${error.message}")
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun getAverageRating(movieId: String): Pair<Float, Int> {
        return try {
            val snap = db.child("ratings").child(movieId).get().await()
            if (!snap.exists()) return Pair(0f, 0)
            val ratings = mutableListOf<Float>()
            for (child in snap.children) {
                val data = child.value as? Map<*, *> ?: continue
                val r = data["rating"]?.toString()?.toFloatOrNull() ?: continue
                ratings.add(r)
            }
            if (ratings.isEmpty()) Pair(0f, 0)
            else Pair(ratings.average().toFloat(), ratings.size)
        } catch (e: Exception) {
            Log.e(TAG, "getAverageRating error: ${e.message}", e)
            Pair(0f, 0)
        }
    }
}
