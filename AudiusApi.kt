package com.elva.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Audius public API: artist-uploaded music, searchable and streamable without an API key. */
object AudiusApi {
    private const val BASE = "https://api.audius.co/v1"
    private const val APP = "Elva"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun trending(genre: String? = null, limit: Int = 30): List<Track> =
        fetch("$BASE/tracks/trending") {
            addQueryParameter("limit", limit.toString())
            if (genre != null) addQueryParameter("genre", genre)
        }

    suspend fun search(query: String, limit: Int = 30): List<Track> =
        fetch("$BASE/tracks/search") {
            addQueryParameter("query", query)
            addQueryParameter("limit", limit.toString())
        }

    private suspend fun fetch(url: String, params: okhttp3.HttpUrl.Builder.() -> Unit): List<Track> =
        withContext(Dispatchers.IO) {
            val u = url.toHttpUrl().newBuilder().addQueryParameter("app_name", APP).apply(params).build()
            client.newCall(Request.Builder().url(u).build()).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val data = json.parseToJsonElement(resp.body!!.string()).jsonObject["data"]?.jsonArray ?: return@use emptyList()
                data.mapNotNull { runCatching { parse(it.jsonObject) }.getOrNull() }
            }
        }

    private fun parse(o: JsonObject): Track? {
        if (o["is_streamable"]?.jsonPrimitive?.booleanOrNull == false) return null
        val id = o["id"]?.jsonPrimitive?.content ?: return null
        val art = o["artwork"]?.takeIf { it !is JsonNull }?.jsonObject
        return Track(
            id = id,
            name = o["title"]?.jsonPrimitive?.content ?: "Untitled",
            artist_name = o["user"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "",
            album_name = o["genre"]?.jsonPrimitive?.contentOrNull ?: "",
            image = art?.get("480x480")?.jsonPrimitive?.contentOrNull ?: art?.get("150x150")?.jsonPrimitive?.contentOrNull ?: "",
            audio = "$BASE/tracks/$id/stream?app_name=$APP",
            duration = o["duration"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    }
}
