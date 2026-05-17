package com.ottapp.moviestream.data.model

data class MovieRequest(
    val id: String = "",
    val title: String = "",
    val userId: String = "",
    val userName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending", // pending, added, rejected
    val count: Int = 1
)
