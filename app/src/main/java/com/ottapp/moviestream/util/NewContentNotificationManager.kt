package com.ottapp.moviestream.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ottapp.moviestream.R
import com.ottapp.moviestream.MainActivity

object NewContentNotificationManager {

    private const val CHANNEL_ID = "new_content_channel"
    private const val CHANNEL_NAME = "নতুন কনটেন্ট"
    private const val PREFS_NAME = "notif_prefs"
    private const val KEY_LAST_SEEN_COUNT = "last_seen_movie_count"
    private const val KEY_NOTIF_ENABLED = "notifications_enabled"
    private var notifId = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "নতুন মুভি ও কনটেন্ট আপডেটের নোটিফিকেশন"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_NOTIF_ENABLED, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply()
    }

    fun checkAndNotifyNewContent(context: Context, currentMovieCount: Int, latestMovieTitle: String) {
        if (!isNotificationsEnabled(context)) return
        val lastCount = prefs(context).getInt(KEY_LAST_SEEN_COUNT, -1)
        if (lastCount == -1) {
            prefs(context).edit().putInt(KEY_LAST_SEEN_COUNT, currentMovieCount).apply()
            return
        }
        if (currentMovieCount > lastCount) {
            val newCount = currentMovieCount - lastCount
            sendNewContentNotification(context, newCount, latestMovieTitle)
            prefs(context).edit().putInt(KEY_LAST_SEEN_COUNT, currentMovieCount).apply()
        }
    }

    fun sendNewContentNotification(context: Context, newCount: Int, title: String) {
        if (!isNotificationsEnabled(context)) return
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val bodyText = if (newCount == 1)
                "\"$title\" এখন উপলব্ধ! এখনই দেখুন।"
            else
                "$newCount টি নতুন মুভি যোগ হয়েছে। এখনই দেখুন!"

            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play)
                .setContentTitle("নতুন কনটেন্ট এসেছে! 🎬")
                .setContentText(bodyText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(notifId++, notif)
        } catch (e: Exception) {
            android.util.Log.e("NotifManager", "sendNotification error: ${e.message}", e)
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
