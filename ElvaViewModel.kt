package com.elva.music

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elva.music.data.AudiusApi
import com.elva.music.data.LibraryData
import com.elva.music.data.LibraryStore
import com.elva.music.data.Playlist
import com.elva.music.data.Track
import com.elva.music.player.PlayerController
import kotlinx.coroutines.launch

class ElvaViewModel(app: Application) : AndroidViewModel(app) {
    val player = PlayerController(app)
    private val store = LibraryStore(app)

    var library by mutableStateOf(store.load()); private set
    var trending by mutableStateOf<List<Track>>(emptyList()); private set
    var genreRows by mutableStateOf<Map<String, List<Track>>>(emptyMap()); private set
    var results by mutableStateOf<List<Track>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    val genres = listOf("Electronic", "Rock", "Pop", "Hip-Hop/Rap", "Jazz", "Ambient")

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            loading = true; error = null
            runCatching {
                trending = AudiusApi.trending()
                genreRows = genres.take(4).associateWith { AudiusApi.trending(genre = it, limit = 15) }
            }.onFailure { error = "Couldn't load music: ${it.message}" }
            loading = false
        }
    }

    fun search(q: String) {
        if (q.isBlank()) { results = emptyList(); return }
        viewModelScope.launch {
            loading = true
            runCatching { results = AudiusApi.search(q) }
                .onFailure { error = "Search failed: ${it.message}" }
            loading = false
        }
    }

    private fun update(data: LibraryData) { library = data; store.save(data) }

    fun isLiked(t: Track) = library.liked.any { it.id == t.id }
    fun toggleLike(t: Track) = update(
        if (isLiked(t)) library.copy(liked = library.liked.filterNot { it.id == t.id })
        else library.copy(liked = listOf(t) + library.liked)
    )

    fun createPlaylist(name: String) {
        if (name.isBlank() || library.playlists.any { it.name == name }) return
        update(library.copy(playlists = library.playlists + Playlist(name.trim())))
    }

    fun addToPlaylist(name: String, t: Track) = update(library.copy(playlists = library.playlists.map {
        if (it.name == name && it.tracks.none { x -> x.id == t.id }) it.copy(tracks = it.tracks + t) else it
    }))

    fun deletePlaylist(name: String) = update(library.copy(playlists = library.playlists.filterNot { it.name == name }))

    override fun onCleared() { player.release() }
}
