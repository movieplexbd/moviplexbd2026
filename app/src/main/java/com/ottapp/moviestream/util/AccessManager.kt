package com.ottapp.moviestream.util

import android.content.Context
import android.util.Log
import com.ottapp.moviestream.data.model.User
import com.ottapp.moviestream.data.repository.UserRepository

class AccessManager(private val context: Context) {

    private val trialManager = TrialManager(context)
    private val userRepo     = UserRepository()

    sealed class AccessResult {
        object Allowed   : AccessResult()
        object Blocked   : AccessResult()
        object NoAccess  : AccessResult()
        object Trial     : AccessResult()
        data class Premium(val expiry: Long) : AccessResult()
        data class Pending(val expiry: Long) : AccessResult()
    }

    suspend fun checkAccess(): AccessResult {
        return try {
            val user = userRepo.getCurrentUser()

            if (user?.isBlocked == true) return AccessResult.Blocked

            if (user?.isPremium == true) {
                val status = user.subscriptionStatus
                return if (status == User.PLAN_PENDING) {
                    AccessResult.Pending(user.subscriptionExpiry)
                } else {
                    AccessResult.Premium(user.subscriptionExpiry)
                }
            }

            val trialActive = if (user?.trialUsed == true) {
                user.isTrialActive
            } else {
                trialManager.activateTrialIfNeeded()
            }

            if (trialActive) {
                return AccessResult.Trial
            }

            if (trialManager.isLocalTrialActive()) {
                return AccessResult.Trial
            }

            AccessResult.NoAccess
        } catch (e: Exception) {
            Log.e("AccessManager", "checkAccess error: ${e.message}", e)
            if (trialManager.isLocalTrialActive()) AccessResult.Trial else AccessResult.NoAccess
        }
    }

    fun quickCheck(): Boolean {
        return trialManager.isLocalTrialActive()
    }
}
