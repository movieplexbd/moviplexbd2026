package com.ottapp.moviestream.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Continue Watching Cross-Device Sync
 * Firebase Realtime DB তে progress save করে — যেকোনো device থেকে continue করা যায়
 */
class ContinueWatchingSyncManager(private val context: Context) {

    companion object {
        private const val TAG    = "CWSync"
        private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseDatabase.getInstance(DB_URL).reference }
    private val localManager = WatchHistoryManager(context)

    private val uid: String? get() = try { auth.currentUser?.uid } catch (e: Exception) { null }

    // ── Remote path: users/{uid}/continue_watching/{movieId} ─────────────────

    /** Player থেকে progress save (local + remote) */
    suspend fun syncProgress(
        movieId: String, title: String, bannerUrl: String,
        category: String, positionMs: Long, durationMs: Long
    ) {
        // local সবসময় save
        val movie = com.ottapp.moviestream.data.model.Movie(
            id = movieId, title = title, bannerImageUrl = bannerUrl, category = category
        )
        localManager.saveProgress(movie, positionMs, durationMs)

        // remote: logged in থাকলে Firebase এ push
        val currentUid = uid ?: return
        try {
            db.child("users").child(currentUid)
                .child("continue_watching").child(movieId)
                .setValue(mapOf(
                    "movieId"     to movieId,
                    "title"       to title,
                    "bannerUrl"   to bannerUrl,
                    "category"    to category,
                    "positionMs"  to positionMs,
                    "durationMs"  to durationMs,
                    "lastWatched" to System.currentTimeMillis(),
                    "device"      to android.os.Build.MODEL
                )).await()
        } catch (e: Exception) {
            Log.w(TAG, "Remote sync failed (offline?): ${e.message}")
        }
    }

    /**
     * App open বা login এ — Firebase থেকে pull করে local এ merge করে
     * Remote > Local (যেটা বেশি সাম্প্রতিক সেটা জেতে)
     */
    suspend fun pullFromRemote() {
        val currentUid = uid ?: return
        try {
            val snapshot = db.child("users").child(currentUid)
                .child("continue_watching").get().await()

            if (!snapshot.exists()) return

            val localHistory = localManager.getHistory().associateBy { it.movieId }.toMutableMap()

            snapshot.children.forEach { child ->
                val data = child.value as? Map<*, *> ?: return@forEach
                val movieId     = data["movieId"]?.toString() ?: return@forEach
                val title       = data["title"]?.toString() ?: ""
                val bannerUrl   = data["bannerUrl"]?.toString() ?: ""
                val category    = data["category"]?.toString() ?: ""
                val positionMs  = (data["positionMs"] as? Long) ?: 0L
                val durationMs  = (data["durationMs"] as? Long) ?: 0L
                val lastWatched = (data["lastWatched"] as? Long) ?: 0L

                val local = localHistory[movieId]
                // Remote newer হলে update করি
                if (local == null || lastWatched > local.lastWatched) {
                    val movie = com.ottapp.moviestream.data.model.Movie(
                        id = movieId, title = title, bannerImageUrl = bannerUrl, category = category
                    )
                    localManager.saveProgress(movie, positionMs, durationMs)
                }
            }
            Log.d(TAG, "Pulled ${snapshot.childrenCount} entries from Firebase")
        } catch (e: Exception) {
            Log.w(TAG, "Pull failed: ${e.message}")
        }
    }

    /**
     * Remote থেকে একটি movie এর progress পড়ে (cross-device resume)
     * Local এর চেয়ে newer হলে return করে
     */
    suspend fun getRemoteProgress(movieId: String): Long {
        val currentUid = uid ?: return 0L
        return try {
            val snap = db.child("users").child(currentUid)
                .child("continue_watching").child(movieId).get().await()
            val data  = snap.value as? Map<*, *> ?: return 0L
            val remotePos   = (data["positionMs"] as? Long) ?: 0L
            val remoteTime  = (data["lastWatched"] as? Long) ?: 0L
            val localEntry  = localManager.getHistory().firstOrNull { it.movieId == movieId }
            val localTime   = localEntry?.lastWatched ?: 0L

            if (remoteTime > localTime) remotePos else (localEntry?.progressMs ?: 0L)
        } catch (e: Exception) { 0L }
    }
}
