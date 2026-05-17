package com.ottapp.moviestream.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.util.Constants
import kotlinx.coroutines.tasks.await

class SubscriptionRepository {

    companion object {
        private const val TAG    = "SubscriptionRepo"
        private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseDatabase.getInstance(DB_URL).reference }

    suspend fun submitPayment(transactionId: String, deviceId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("ব্যবহারকারী লগইন করা নেই"))

        return try {
            val now    = System.currentTimeMillis()
            val expiry = now + Constants.PENDING_ACCESS_DURATION_MS

            val subData = mapOf(
                "uid"           to uid,
                "transactionId" to transactionId,
                "deviceId"      to deviceId,
                "status"        to "PENDING",
                "access"        to true,
                "submittedAt"   to now,
                "expiry"        to expiry
            )

            db.child(Constants.DB_SUBSCRIPTIONS).child(uid).setValue(subData).await()

            db.child(Constants.DB_USERS).child(uid).updateChildren(
                mapOf(
                    "subscriptionStatus" to Constants.SUB_PENDING,
                    "subscriptionExpiry" to expiry
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "submitPayment error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getSubscriptionStatus(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snap = db.child(Constants.DB_SUBSCRIPTIONS).child(uid).child("status").get().await()
            snap.getValue(String::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "getStatus error: ${e.message}")
            null
        }
    }
}
