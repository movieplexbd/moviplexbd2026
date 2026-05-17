package com.ottapp.moviestream.data.repository

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.data.model.Actor
import kotlinx.coroutines.tasks.await

class ActorRepository {

    companion object {
        private const val TAG = "ActorRepository"
        private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"
    }

    private val db by lazy {
        FirebaseDatabase.getInstance(DB_URL).reference
    }

    private val actorsRef by lazy {
        db.child("actors")
    }

    private fun snapshotToActor(snapshot: com.google.firebase.database.DataSnapshot): Actor? {
        return try {
            val data = snapshot.value as? Map<*, *> ?: return null
            Actor(
                id = snapshot.key ?: "",
                name = data["name"]?.toString() ?: "",
                imageUrl = data["imageUrl"]?.toString() ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse actor error: ${e.message}")
            null
        }
    }

    suspend fun getAllActors(): List<Actor> {
        return try {
            val snapshot = actorsRef.get().await()
            snapshot.children.mapNotNull { snapshotToActor(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getAllActors error: ${e.message}")
            emptyList()
        }
    }

    suspend fun getActorById(id: String): Actor? {
        return try {
            val snapshot = actorsRef.child(id).get().await()
            snapshotToActor(snapshot)
        } catch (e: Exception) {
            Log.e(TAG, "getActorById error: ${e.message}")
            null
        }
    }

    suspend fun getActorsByIds(ids: List<String>): List<Actor> {
        return ids.mapNotNull { getActorById(it) }
    }

    suspend fun addActor(actor: Actor): String {
        val newRef = actorsRef.push()
        val id = newRef.key ?: throw Exception("Failed to generate ID")
        val actorWithId = actor.copy(id = id)
        newRef.setValue(actorToMap(actorWithId)).await()
        return id
    }

    suspend fun updateActor(actor: Actor) {
        if (actor.id.isEmpty()) throw Exception("Actor ID is missing")
        actorsRef.child(actor.id).updateChildren(actorToMap(actor)).await()
    }

    suspend fun deleteActor(id: String) {
        actorsRef.child(id).removeValue().await()
    }

    private fun actorToMap(actor: Actor): Map<String, Any?> = mapOf(
        "name" to actor.name,
        "imageUrl" to actor.imageUrl
    )
}
