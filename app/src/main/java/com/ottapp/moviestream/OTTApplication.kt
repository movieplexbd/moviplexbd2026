package com.ottapp.moviestream

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ottapp.moviestream.util.NewContentNotificationManager
import com.ottapp.moviestream.service.CineStreamFirebaseMessagingService
import com.ottapp.moviestream.util.ThemeManager

@HiltAndroidApp
class OTTApplication : Application(), Configuration.Provider {

    companion object {
        const val DOWNLOAD_CHANNEL_ID = "ott_downloads"
        var firebaseReady = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        ThemeManager.applySavedTheme(this)
        initFirebaseSafely()
        initCrashlytics()
        createNotificationChannels()
        NewContentNotificationManager.createNotificationChannel(this)
        createFCMNotificationChannels()
    }

    private fun initFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            firebaseReady = FirebaseApp.getApps(this).isNotEmpty()
        } catch (e: Exception) {
            Log.e("OTTApplication", "Firebase init failed: ${e.message}", e)
            firebaseReady = false
        }
    }

    private fun initCrashlytics() {
        try {
            if (firebaseReady) {
                val crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics.setCrashlyticsCollectionEnabled(true)
            }
        } catch (e: Exception) {
            Log.e("OTTApplication", "Crashlytics init failed: ${e.message}", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val downloadChannel = NotificationChannel(
                    DOWNLOAD_CHANNEL_ID,
                    "Downloads",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Movie download progress"
                    setSound(null, null)
                }
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(downloadChannel)
            } catch (e: Exception) {
                Log.e("OTTApplication", "Notification channel error: ${e.message}", e)
            }
        }
    }

    private fun createFCMNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val nm = getSystemService(NotificationManager::class.java)
                // New movies notification channel
                val moviesChannel = NotificationChannel(
                    CineStreamFirebaseMessagingService.CHANNEL_ID_MOVIES,
                    "নতুন মুভি",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "নতুন মুভি যোগ হলে notification আসবে" }
                nm?.createNotificationChannel(moviesChannel)

                // Promotions channel
                val promoChannel = NotificationChannel(
                    CineStreamFirebaseMessagingService.CHANNEL_ID_PROMO,
                    "অফার ও প্রোমো",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "বিশেষ অফার ও ছাড়ের notification" }
                nm?.createNotificationChannel(promoChannel)
            } catch (e: Exception) {
                Log.e("OTTApplication", "FCM channel error: ${e.message}", e)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}
