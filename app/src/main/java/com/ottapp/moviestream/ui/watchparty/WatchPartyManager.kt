package com.ottapp.moviestream.ui.watchparty

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Watch Party — Real-time synchronized watching via Firebase
 * Host creates room → shares code → Guests join → sync playback
 * Firebase path: watch_parties/{roomId}/
 */
object WatchPartyManager {

    private const val TAG    = "WatchParty"
    private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"

    private val db   by lazy { FirebaseDatabase.getInstance(DB_URL).reference }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val uid:  String get() = auth.currentUser?.uid ?: "guest"
    private val name: String get() = auth.currentUser?.displayName?.ifBlank { "User" } ?: "User"

    data class RoomState(
        val roomId:      String  = "",
        val movieId:     String  = "",
        val movieTitle:  String  = "",
        val videoUrl:    String  = "",
        val hostUid:     String  = "",
        val positionMs:  Long    = 0L,
        val isPlaying:   Boolean = false,
        val lastSyncAt:  Long    = 0L,
        val memberCount: Int     = 0
    )

    suspend fun createRoom(movieId: String, movieTitle: String, videoUrl: String): String {
        val roomId = generateRoomCode()
        val room   = mapOf(
            "movieId"    to movieId,
            "movieTitle" to movieTitle,
            "videoUrl"   to videoUrl,
            "hostUid"    to uid,
            "hostName"   to name,
            "positionMs" to 0L,
            "isPlaying"  to false,
            "lastSyncAt" to System.currentTimeMillis(),
            "createdAt"  to System.currentTimeMillis(),
            "expiresAt"  to (System.currentTimeMillis() + 4 * 60 * 60 * 1000L)
        )
        db.child("watch_parties").child(roomId).setValue(room).await()
        joinRoomInternal(roomId)
        return roomId
    }

    suspend fun joinRoom(roomId: String): RoomState? {
        return try {
            val snap = db.child("watch_parties").child(roomId.uppercase()).get().await()
            if (!snap.exists()) return null
            joinRoomInternal(roomId.uppercase())
            snapshotToRoomState(snap)
        } catch (e: Exception) {
            Log.e(TAG, "Join failed: ${e.message}")
            null
        }
    }

    suspend fun pushPlaybackState(roomId: String, positionMs: Long, isPlaying: Boolean) {
        try {
            db.child("watch_parties").child(roomId).updateChildren(mapOf(
                "positionMs" to positionMs,
                "isPlaying"  to isPlaying,
                "lastSyncAt" to System.currentTimeMillis()
            )).await()
        } catch (e: Exception) {
            Log.w(TAG, "Push state failed: ${e.message}")
        }
    }

    fun observeRoom(roomId: String): Flow<RoomState> = callbackFlow {
        val ref = db.child("watch_parties").child(roomId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshotToRoomState(snapshot)?.let { trySend(it) }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Room observe cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun leaveRoom(roomId: String) {
        try { db.child("watch_parties").child(roomId).child("members").child(uid).removeValue() }
        catch (e: Exception) { }
    }

    suspend fun deleteRoom(roomId: String) {
        try { db.child("watch_parties").child(roomId).removeValue().await() }
        catch (e: Exception) { }
    }

    private fun joinRoomInternal(roomId: String) {
        db.child("watch_parties").child(roomId).child("members").child(uid)
            .setValue(mapOf("name" to name, "joinedAt" to System.currentTimeMillis()))
    }

    private fun snapshotToRoomState(snap: DataSnapshot): RoomState? {
        val data = snap.value as? Map<*, *> ?: return null
        val members = snap.child("members").childrenCount.toInt()
        return RoomState(
            roomId      = snap.key ?: "",
            movieId     = data["movieId"]?.toString() ?: "",
            movieTitle  = data["movieTitle"]?.toString() ?: "",
            videoUrl    = data["videoUrl"]?.toString() ?: "",
            hostUid     = data["hostUid"]?.toString() ?: "",
            positionMs  = (data["positionMs"] as? Long) ?: 0L,
            isPlaying   = (data["isPlaying"] as? Boolean) ?: false,
            lastSyncAt  = (data["lastSyncAt"] as? Long) ?: 0L,
            memberCount = members
        )
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
