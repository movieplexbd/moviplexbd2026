package com.ottapp.moviestream.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.data.model.MovieRequest
import com.ottapp.moviestream.util.Constants
import kotlinx.coroutines.tasks.await

class RequestRepository {

    private val db = FirebaseDatabase.getInstance("https://movies-bee24-default-rtdb.firebaseio.com").reference
    private val auth = FirebaseAuth.getInstance()
    private val requestsRef = db.child(Constants.DB_REQUESTS)

    suspend fun submitRequest(movieTitle: String): Boolean {
        val uid = auth.currentUser?.uid ?: "anonymous"
        val userName = auth.currentUser?.displayName ?: "User"
        
        return try {
            // Check if this movie is already requested
            val snapshot = requestsRef.get().await()
            var existingKey: String? = null
            var existingCount = 0
            
            for (child in snapshot.children) {
                val title = child.child("title").value?.toString()
                if (title?.equals(movieTitle, ignoreCase = true) == true) {
                    existingKey = child.key
                    existingCount = (child.child("count").value as? Long)?.toInt() ?: 1
                    break
                }
            }

            if (existingKey != null) {
                // Update existing request count
                requestsRef.child(existingKey).child("count").setValue(existingCount + 1).await()
            } else {
                // Create new request
                val newRef = requestsRef.push()
                val request = MovieRequest(
                    id = newRef.key ?: "",
                    title = movieTitle,
                    userId = uid,
                    userName = userName,
                    timestamp = System.currentTimeMillis(),
                    status = "pending",
                    count = 1
                )
                newRef.setValue(request).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllRequests(): List<MovieRequest> {
        return try {
            val snapshot = requestsRef.get().await()
            snapshot.children.mapNotNull { it.getValue(MovieRequest::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
