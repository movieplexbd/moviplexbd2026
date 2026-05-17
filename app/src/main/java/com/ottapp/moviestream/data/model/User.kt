package com.ottapp.moviestream.data.model

data class User(
    var uid: String = "",
    var email: String = "",
    var displayName: String = "",
    var photoUrl: String = "",
    var subscriptionStatus: String = PLAN_FREE,
    var subscriptionExpiry: Long = 0L,
    var trialUsed: Boolean = false,
    var trialExpiry: Long = 0L,
    var devices: Map<String, UserDevice> = emptyMap(),
    var activityLogs: Map<String, UserActivity> = emptyMap()
) {
    constructor() : this("", "", "", "", PLAN_FREE, 0L, false, 0L, emptyMap(), emptyMap())

    val isPremium: Boolean
        get() = (subscriptionStatus == PLAN_PREMIUM || subscriptionStatus == PLAN_PENDING) &&
                (subscriptionExpiry == 0L || subscriptionExpiry > System.currentTimeMillis())

    val isBlocked: Boolean
        get() = subscriptionStatus == PLAN_BLOCKED

    val isTrialActive: Boolean
        get() = trialUsed && trialExpiry > System.currentTimeMillis()

    val hasAccess: Boolean
        get() = isTrialActive || isPremium

    companion object {
        const val PLAN_FREE    = "free"
        const val PLAN_PREMIUM = "premium"
        const val PLAN_PENDING = "pending"
        const val PLAN_BLOCKED = "blocked"
    }
}

data class UserDevice(
    var deviceId: String = "",
    var deviceName: String = "",
    var lastLogin: Long = 0L,
    var isActive: Boolean = true
)

data class UserActivity(
    var id: String = "",
    var movieId: String = "",
    var movieTitle: String = "",
    var timestamp: Long = 0L,
    var durationWatched: Long = 0L,
    var action: String = "watch" // "watch", "search", "login"
)
