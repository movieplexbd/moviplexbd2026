package com.ottapp.moviestream.util

object Constants {
    // Firebase DB paths
    const val DB_MOVIES        = "movies"
    const val DB_USERS         = "users"
    const val DB_SUBSCRIPTIONS = "subscriptions"
    const val DB_REQUESTS      = "movie_requests"
    const val DB_BANNERS       = "banners"
    const val DB_REELS         = "reels"
    const val DB_UPDATE_CONFIG = "app_update_config"
    const val DB_URL           = "https://movies-bee24-default-rtdb.firebaseio.com"

    // Intent extras
    const val EXTRA_MOVIE_ID    = "extra_movie_id"
    const val EXTRA_MOVIE_TITLE = "extra_movie_title"
    const val EXTRA_VIDEO_URL   = "extra_video_url"
    const val EXTRA_BANNER_URL  = "extra_banner_url"
    const val EXTRA_IS_LOCAL    = "extra_is_local"
    const val EXTRA_START_POS   = "extra_start_pos"

    // Shared prefs
    const val PREF_PLAYBACK_POSITION = "pref_playback_position_"
    const val PREF_TRIAL             = "pref_trial"
    const val PREF_TRIAL_USED        = "trial_used"
    const val PREF_TRIAL_EXPIRY      = "trial_expiry"

    // Download
    const val DOWNLOAD_NOTIFICATION_ID = 1001
    const val DOWNLOAD_WORK_TAG        = "movie_download"

    // Categories
    const val CAT_ALL      = "All"
    const val CAT_BANGLA   = "Bangla Dubbed"
    const val CAT_HINDI    = "Hindi Dubbed"
    const val CAT_TRENDING = "Trending"

    // Subscription
    const val SUB_FREE    = "free"
    const val SUB_PREMIUM = "premium"
    const val SUB_PENDING = "pending"
    const val SUB_BLOCKED = "blocked"

    // Max test movies allowed
    const val MAX_TEST_MOVIES = 2

    // Trial duration = 2 hours in ms
    const val TRIAL_DURATION_MS = 2L * 60L * 60L * 1000L

    // Pending access duration = 6 hours in ms
    const val PENDING_ACCESS_DURATION_MS = 6L * 60L * 60L * 1000L

    // Full subscription duration = 30 days in ms
    const val SUBSCRIPTION_DURATION_MS = 30L * 24L * 60L * 60L * 1000L

    // bKash/Nagad payment number
    const val PAYMENT_NUMBER = "01913305107"

    // Deep Link
    const val DEEP_LINK_SCHEME = "cinestream"
    const val DEEP_LINK_HOST   = "movie"
    const val DEEP_LINK_WEB    = "https://cinestream.app/movie/"

    // Search History
    const val MAX_SEARCH_HISTORY = 20

    // Recently Viewed
    const val MAX_RECENTLY_VIEWED = 30
}
