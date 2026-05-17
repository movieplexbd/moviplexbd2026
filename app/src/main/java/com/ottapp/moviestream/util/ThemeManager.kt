package com.ottapp.moviestream.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DARK_MODE = "is_dark_mode"

    fun isDarkMode(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(context: Context, dark: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, dark).apply()
        applyTheme(dark)
    }

    fun applyTheme(dark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun applySavedTheme(context: Context) {
        applyTheme(isDarkMode(context))
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
