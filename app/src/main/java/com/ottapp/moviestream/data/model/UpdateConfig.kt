package com.ottapp.moviestream.data.model

data class UpdateConfig(
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val updateTitle: String = "",
    val updateMessage: String = "",
    val changelog: List<String> = emptyList(),
    val downloadLink: String = "",
    val updateType: String = "SOFT", // SOFT or FORCE
    val isEnabled: Boolean = false
)
