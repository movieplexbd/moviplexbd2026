package com.ottapp.moviestream.data.repository

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.data.model.UpdateConfig
import com.ottapp.moviestream.util.Constants
import kotlinx.coroutines.tasks.await

class UpdateRepository {

    companion object {
        private const val TAG = "UpdateRepository"
    }

    private val db by lazy {
        FirebaseDatabase.getInstance(Constants.DB_URL).reference
    }

    private val updateRef by lazy {
        db.child(Constants.DB_UPDATE_CONFIG)
    }

    suspend fun getUpdateConfig(): UpdateConfig? {
        return try {
            val snapshot = updateRef.get().await()
            if (!snapshot.exists()) return null

            val data = snapshot.value as? Map<*, *> ?: return null
            
            UpdateConfig(
                latestVersionCode = (data["latestVersionCode"] as? Long)?.toInt() ?: 0,
                latestVersionName = data["latestVersionName"]?.toString() ?: "",
                updateTitle = data["updateTitle"]?.toString() ?: "",
                updateMessage = data["updateMessage"]?.toString() ?: "",
                changelog = (data["changelog"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                downloadLink = data["downloadLink"]?.toString() ?: "",
                updateType = data["updateType"]?.toString() ?: "SOFT",
                isEnabled = data["isEnabled"] as? Boolean ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "getUpdateConfig error: ${e.message}")
            null
        }
    }

    suspend fun saveUpdateConfig(config: UpdateConfig) {
        try {
            val map = mapOf(
                "latestVersionCode" to config.latestVersionCode,
                "latestVersionName" to config.latestVersionName,
                "updateTitle" to config.updateTitle,
                "updateMessage" to config.updateMessage,
                "changelog" to config.changelog,
                "downloadLink" to config.downloadLink,
                "updateType" to config.updateType,
                "isEnabled" to config.isEnabled
            )
            updateRef.setValue(map).await()
        } catch (e: Exception) {
            Log.e(TAG, "saveUpdateConfig error: ${e.message}")
            throw e
        }
    }
}
