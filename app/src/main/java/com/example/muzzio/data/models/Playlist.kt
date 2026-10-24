package com.example.muzzio.data.models

data class Playlist(
    val id: String,
    var name: String,
    var tracks: MutableList<String>
)
