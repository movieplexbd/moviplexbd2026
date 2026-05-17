package com.ottapp.moviestream.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Release থেকে auto-update check করে
 * PlayStore নেই — GitHub Releases থেকে APK directly download করে update করে
 *
 * Firebase DB তে update config রাখার option আছে (admin control)
 * GitHub API ও check করে latest release এর জন্য
 */
class GitHubUpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "GitHubUpdate"

        // ── GitHub Repo config — আপনার repo অনুযায়ী পরিবর্তন করুন ──────────
        const val GITHUB_OWNER = "YOUR_GITHUB_USERNAME"   // e.g. "ottdev"
        const val GITHUB_REPO  = "CineStreamOTT"          // repo name
        // ─────────────────────────────────────────────────────────────────────

        private const val GITHUB_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }

    data class UpdateInfo(
        val latestVersion: String,
        val currentVersion: String,
        val downloadUrl: String,       // APK direct link
        val releaseNotes: String,
        val updateType: String,        // "FORCE" or "SOFT"
        val isUpdateAvailable: Boolean
    )

    /**
     * GitHub Releases API থেকে latest version check করে
     * tag_name format: v3.5.0 বা 3.5.0
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion()

            val conn = (URL(GITHUB_API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout    = 10_000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "CineStream-Android")
            }

            if (conn.responseCode != 200) {
                Log.w(TAG, "GitHub API returned: ${conn.responseCode}")
                return@withContext null
            }

            val response = conn.inputStream.bufferedReader().readText()
            val json     = JSONObject(response)

            val tagName      = json.getString("tag_name").trimStart('v', 'V')
            val releaseNotes = json.optString("body", "Bug fixes & improvements")
            val prerelease   = json.optBoolean("prerelease", false)
            if (prerelease) return@withContext null   // pre-release skip

            // APK asset খুঁজি
            val assets     = json.getJSONArray("assets")
            var apkUrl     = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name  = asset.getString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }

            if (apkUrl.isEmpty()) {
                // APK না থাকলে release page link
                apkUrl = json.optString("html_url", "")
            }

            val isNewer     = isVersionNewer(tagName, currentVersion)
            val updateType  = determineUpdateType(tagName, currentVersion)

            UpdateInfo(
                latestVersion      = tagName,
                currentVersion     = currentVersion,
                downloadUrl        = apkUrl,
                releaseNotes       = releaseNotes,
                updateType         = updateType,
                isUpdateAvailable  = isNewer
            )

        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}")
            null
        }
    }

    /**
     * Firebase DB থেকেও update config পড়তে পারে (Admin control)
     * Firebase > GitHub priority — admin force update করতে পারবে
     */
    suspend fun checkFromFirebase(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val db = com.google.firebase.database.FirebaseDatabase
                .getInstance("https://movies-bee24-default-rtdb.firebaseio.com")
                .reference
            val snap = db.child("app_update_config").get()
                .addOnSuccessListener { }.addOnFailureListener { }
            // coroutine wait
            kotlinx.coroutines.tasks.await(
                com.google.firebase.database.FirebaseDatabase
                    .getInstance("https://movies-bee24-default-rtdb.firebaseio.com")
                    .reference.child("app_update_config").get()
            ).let { snapshot ->
                val data = snapshot.value as? Map<*, *> ?: return@withContext null
                val latestVer   = data["latestVersion"]?.toString() ?: return@withContext null
                val downloadUrl = data["downloadUrl"]?.toString() ?: ""
                val updateType  = data["updateType"]?.toString() ?: "SOFT"
                val changelog   = data["changelog"]?.toString() ?: ""
                val currentVer  = getCurrentVersion()

                UpdateInfo(
                    latestVersion     = latestVer,
                    currentVersion    = currentVer,
                    downloadUrl       = downloadUrl,
                    releaseNotes      = changelog,
                    updateType        = updateType,
                    isUpdateAvailable = isVersionNewer(latestVer, currentVer)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase update check failed: ${e.message}")
            null
        }
    }

    /** In-app download করে install করে (Android 8+) */
    fun openInstallPage(downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open install page: ${e.message}")
        }
    }

    private fun getCurrentVersion(): String {
        return try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "0.0.0"
        } catch (e: Exception) { "0.0.0" }
    }

    /** Semantic versioning compare: "3.5.0" > "3.4.1" */
    private fun isVersionNewer(latest: String, current: String): Boolean {
        return try {
            val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val c = current.split(".").map { it.toIntOrNull() ?: 0 }
            val max = maxOf(l.size, c.size)
            for (i in 0 until max) {
                val lv = l.getOrElse(i) { 0 }
                val cv = c.getOrElse(i) { 0 }
                if (lv > cv) return true
                if (lv < cv) return false
            }
            false
        } catch (e: Exception) { false }
    }

    /**
     * Major version bump = FORCE update
     * Minor/patch = SOFT update
     */
    private fun determineUpdateType(latest: String, current: String): String {
        return try {
            val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val c = current.split(".").map { it.toIntOrNull() ?: 0 }
            if ((l.getOrElse(0) { 0 }) > (c.getOrElse(0) { 0 })) "FORCE" else "SOFT"
        } catch (e: Exception) { "SOFT" }
    }
}
