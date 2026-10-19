package com.elva.music.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Tiny on-device store for liked songs and playlists. */
class LibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences("elva_library", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): LibraryData = runCatching {
        json.decodeFromString<LibraryData>(prefs.getString("data", null) ?: return LibraryData())
    }.getOrDefault(LibraryData())

    fun save(data: LibraryData) {
        prefs.edit().putString("data", json.encodeToString(data)).apply()
    }
}
