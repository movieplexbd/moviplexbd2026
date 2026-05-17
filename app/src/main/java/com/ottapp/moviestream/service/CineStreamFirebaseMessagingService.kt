package com.ottapp.moviestream.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ottapp.moviestream.MainActivity
import com.ottapp.moviestream.R
import com.ottapp.moviestream.util.Constants

/**
 * Firebase Cloud Messaging Service
 * নতুন মুভি আসলে push notification পাঠাবে
 *
 * Firebase Console থেকে notification পাঠানোর format:
 * {
 *   "title": "নতুন মুভি এসেছে! 🎬",
 *   "body": "Avengers: Endgame এখন দেখুন",
 *   "data": {
 *     "type": "new_movie",
 *     "movie_id": "ABC123",
 *     "movie_title": "Avengers: Endgame"
 *   }
 * }
 */
class CineStreamFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CineStreamFCM"
        const val CHANNEL_ID_MOVIES = "new_movies_channel"
        const val CHANNEL_ID_PROMO  = "promotions_channel"
        private var notificationId = 2000
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token: $token")
        // TODO: Send token to your server/Firebase to target this device
        // saveTokenToFirebase(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title   = notification?.title ?: data["title"] ?: "CineStream"
        val body    = notification?.body  ?: data["body"]  ?: ""
        val movieId = data["movie_id"]
        val type    = data["type"] ?: "general"

        createNotificationChannels()
        showNotification(title, body, movieId, type)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // New movies channel
            NotificationChannel(
                CHANNEL_ID_MOVIES,
                "নতুন মুভি",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "নতুন মুভি যোগ হলে notification আসবে"
                nm.createNotificationChannel(this)
            }

            // Promotions channel
            NotificationChannel(
                CHANNEL_ID_PROMO,
                "অফার ও প্রোমো",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "বিশেষ অফার ও ছাড়ের notification"
                nm.createNotificationChannel(this)
            }
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        movieId: String?,
        type: String
    ) {
        val channelId = when (type) {
            "new_movie" -> CHANNEL_ID_MOVIES
            else        -> CHANNEL_ID_PROMO
        }

        // Tap করলে movie detail বা main screen খুলবে
        val intent = if (movieId != null) {
            Intent(this, MovieDeepLinkActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(Constants.EXTRA_MOVIE_ID, movieId)
                putExtra("deep_link_movie", true)
            }
        } else {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId++, notification)
    }
}
