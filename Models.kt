package com.elva.music.data

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val name: String,
    val artist_name: String = "",
    val album_name: String = "",
    val image: String = "",
    val audio: String = "",
    val duration: Int = 0,
)

@Serializable
data class TrackResponse(val results: List<Track> = emptyList())

@Serializable
data class Playlist(val name: String, val tracks: List<Track> = emptyList())

@Serializable
data class LibraryData(
    val liked: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)
