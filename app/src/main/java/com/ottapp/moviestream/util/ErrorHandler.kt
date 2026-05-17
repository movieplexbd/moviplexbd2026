package com.ottapp.moviestream.util

import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Centralized error handler - proper error messages দেখাবে
 */
object ErrorHandler {

    fun getErrorMessage(e: Exception): String {
        return when (e) {
            is UnknownHostException   -> "ইন্টারনেট সংযোগ নেই। WiFi বা মোবাইল ডেটা চেক করুন।"
            is SocketTimeoutException -> "সার্ভার সাড়া দিচ্ছে না। পরে আবার চেষ্টা করুন।"
            else -> when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "নেটওয়ার্ক সমস্যা। ইন্টারনেট সংযোগ চেক করুন।"
                e.message?.contains("permission", ignoreCase = true) == true ->
                    "অনুমতি নেই। সাইন ইন করুন।"
                e.message?.contains("firestore", ignoreCase = true) == true ||
                e.message?.contains("firebase", ignoreCase = true) == true ->
                    "সার্ভার সমস্যা। একটু পরে চেষ্টা করুন।"
                else -> "কিছু একটা সমস্যা হয়েছে। আবার চেষ্টা করুন।"
            }
        }
    }

    fun showError(view: View, e: Exception, retryAction: (() -> Unit)? = null) {
        val msg = getErrorMessage(e)
        val snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
        if (retryAction != null) {
            snackbar.setAction("আবার চেষ্টা করুন") { retryAction() }
        }
        snackbar.show()
    }

    fun showNetworkError(view: View, retryAction: (() -> Unit)? = null) {
        val snackbar = Snackbar.make(
            view,
            "ইন্টারনেট সংযোগ নেই",
            Snackbar.LENGTH_INDEFINITE
        )
        if (retryAction != null) {
            snackbar.setAction("আবার চেষ্টা করুন") { retryAction() }
        }
        snackbar.show()
    }
}
