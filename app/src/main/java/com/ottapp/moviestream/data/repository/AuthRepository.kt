package com.ottapp.moviestream.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.data.model.User
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    companion object {
        private const val TAG = "AuthRepository"
        private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"
    }

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val db by lazy {
        FirebaseDatabase.getInstance(DB_URL).reference
    }

    val currentFirebaseUser: FirebaseUser?
        get() = try { auth.currentUser } catch (e: Exception) { null }

    val isLoggedIn: Boolean
        get() = try { auth.currentUser != null } catch (e: Exception) { false }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Firebase user null"))
            saveUserToDatabase(firebaseUser)
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Firebase user null"))
            saveUserToDatabase(firebaseUser, displayName)
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-up failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun saveUserToDatabase(firebaseUser: FirebaseUser, overrideName: String? = null) {
        try {
            val uid = firebaseUser.uid
            val userRef = db.child("users").child(uid)
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                val newUser = User(
                    uid                = uid,
                    email              = firebaseUser.email ?: "",
                    displayName        = overrideName ?: firebaseUser.displayName ?: "",
                    photoUrl           = firebaseUser.photoUrl?.toString() ?: "",
                    subscriptionStatus = User.PLAN_FREE,
                    subscriptionExpiry = 0L
                )
                userRef.setValue(newUser).await()
            } else {
                val updates = mutableMapOf<String, Any>()
                firebaseUser.email?.let { updates["email"] = it }
                if (overrideName != null) updates["displayName"] = overrideName
                if (updates.isNotEmpty()) userRef.updateChildren(updates).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Save user to DB failed: ${e.message}", e)
        }
    }

    suspend fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase sign-out error: ${e.message}")
        }
    }
}
