package com.ottapp.moviestream.service

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ottapp.moviestream.MainActivity
import com.ottapp.moviestream.util.Constants

/**
 * Deep Link handler - share করলে সরাসরি movie খুলবে
 * URL format: cinestream://movie/{movieId}
 * Web format: https://cinestream.app/movie/{movieId}
 */
class MovieDeepLinkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val movieId = extractMovieId(intent)
        if (movieId != null) {
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(Constants.EXTRA_MOVIE_ID, movieId)
                putExtra("deep_link_movie", true)
            }
            startActivity(mainIntent)
        } else {
            // No valid deep link - open app normally
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(mainIntent)
        }
        finish()
    }

    private fun extractMovieId(intent: Intent?): String? {
        return when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val data: Uri? = intent.data
                // Handle: cinestream://movie/ABC123
                // Handle: https://cinestream.app/movie/ABC123
                when {
                    data?.scheme == "cinestream" && data.host == "movie" ->
                        data.pathSegments.firstOrNull() ?: data.lastPathSegment
                    data?.pathSegments?.contains("movie") == true ->
                        data.lastPathSegment
                    else -> intent.getStringExtra(Constants.EXTRA_MOVIE_ID)
                }
            }
            else -> intent?.getStringExtra(Constants.EXTRA_MOVIE_ID)
        }
    }
}
