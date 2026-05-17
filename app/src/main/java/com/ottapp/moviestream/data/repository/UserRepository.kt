package com.ottapp.moviestream.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ottapp.moviestream.data.model.User
import com.ottapp.moviestream.data.model.UserDevice
import com.ottapp.moviestream.data.model.UserActivity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {

    companion object {
        private const val TAG   = "UserRepository"
        private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val db by lazy { FirebaseDatabase.getInstance(DB_URL).reference }

    @Suppress("UNCHECKED_CAST")
    private fun snapshotToUser(snapshot: DataSnapshot): User? {
        return try {
            val data = snapshot.value as? Map<*, *> ?: return null
            User(
                uid                = data["uid"]?.toString() ?: snapshot.key ?: "",
                email              = data["email"]?.toString() ?: "",
                displayName        = data["displayName"]?.toString() ?: "",
                photoUrl           = data["photoUrl"]?.toString() ?: "",
                subscriptionStatus = data["subscriptionStatus"]?.toString() ?: User.PLAN_FREE,
                subscriptionExpiry = data["subscriptionExpiry"]?.toString()?.toLongOrNull()
                    ?: (data["subscriptionExpiry"] as? Long) ?: 0L,
                trialUsed          = data["trialUsed"]?.toString()?.toBoolean() ?: false,
                trialExpiry        = data["trialExpiry"]?.toString()?.toLongOrNull()
                    ?: (data["trialExpiry"] as? Long) ?: 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse user error: ${e.message}")
            null
        }
    }

    suspend fun getCurrentUser(): User? {
        val uid = try { auth.currentUser?.uid } catch (e: Exception) { null } ?: return null
        return try {
            val snapshot = db.child("users").child(uid).get().await()
            snapshotToUser(snapshot)
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentUser error: ${e.message}", e)
            null
        }
    }

    fun getCurrentUserFlow(): Flow<User?> = callbackFlow {
        val uid = try { auth.currentUser?.uid } catch (e: Exception) { null }
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val userRef = db.child("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { trySend(snapshotToUser(snapshot)) }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "User flow cancelled: ${error.message}")
                trySend(null)
            }
        }
        userRef.addValueEventListener(listener)
        awaitClose { userRef.removeEventListener(listener) }
    }

    suspend fun updateSubscription(uid: String, status: String, expiry: Long) {
        db.child("users").child(uid).updateChildren(
            mapOf("subscriptionStatus" to status, "subscriptionExpiry" to expiry)
        ).await()
    }

    suspend fun activateTrial(uid: String, expiry: Long) {
        db.child("users").child(uid).updateChildren(
            mapOf("trialUsed" to true, "trialExpiry" to expiry)
        ).await()
    }

    suspend fun logActivity(uid: String, activity: UserActivity) {
        val key = db.child("users").child(uid).child("activityLogs").push().key ?: return
        activity.id = key
        db.child("users").child(uid).child("activityLogs").child(key).setValue(activity).await()
    }

    suspend fun updateDevice(uid: String, device: UserDevice) {
        db.child("users").child(uid).child("devices").child(device.deviceId).setValue(device).await()
    }

    suspend fun logoutDevice(uid: String, deviceId: String) {
        db.child("users").child(uid).child("devices").child(deviceId).child("isActive").setValue(false).await()
    }
}
