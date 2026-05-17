package com.ottapp.moviestream.data.model

data class Actor(
    var id: String = "",
    var name: String = "",
    var imageUrl: String = ""
) {
    constructor() : this("", "", "")
}
