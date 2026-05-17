package com.ottapp.moviestream.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.ottapp.moviestream.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrialManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(Constants.PREF_TRIAL, Context.MODE_PRIVATE)
    }

    private val userRepo = UserRepository()

    fun isTrialUsedLocally(): Boolean = prefs.getBoolean(Constants.PREF_TRIAL_USED, false)

    fun getLocalTrialExpiry(): Long = prefs.getLong(Constants.PREF_TRIAL_EXPIRY, 0L)

    fun isLocalTrialActive(): Boolean {
        if (!isTrialUsedLocally()) return false
        return getLocalTrialExpiry() > System.currentTimeMillis()
    }

    private fun saveTrialLocally(expiry: Long) {
        prefs.edit()
            .putBoolean(Constants.PREF_TRIAL_USED, true)
            .putLong(Constants.PREF_TRIAL_EXPIRY, expiry)
            .apply()
    }

    suspend fun activateTrialIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = userRepo.getCurrentUser() ?: return@withContext false

            if (user.trialUsed) {
                saveTrialLocally(user.trialExpiry)
                return@withContext user.isTrialActive
            }

            if (isTrialUsedLocally()) {
                return@withContext isLocalTrialActive()
            }

            val expiry = System.currentTimeMillis() + Constants.TRIAL_DURATION_MS
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext false

            userRepo.activateTrial(uid, expiry)
            saveTrialLocally(expiry)

            Log.d("TrialManager", "Trial activated. Expiry: $expiry")
            true
        } catch (e: Exception) {
            Log.e("TrialManager", "activateTrialIfNeeded error: ${e.message}", e)
            false
        }
    }

    fun getRemainingTrialMillis(): Long {
        val expiry = getLocalTrialExpiry()
        return if (expiry > System.currentTimeMillis()) expiry - System.currentTimeMillis() else 0L
    }

    fun getRemainingTrialText(): String {
        val ms = getRemainingTrialMillis()
        if (ms <= 0) return "ট্রায়াল শেষ"
        val minutes = ms / (60 * 1000)
        val hours   = minutes / 60
        val mins    = minutes % 60
        return if (hours > 0) "${hours} ঘণ্টা ${mins} মিনিট বাকি" else "${mins} মিনিট বাকি"
    }
}
