package com.elva.music.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.elva.music.data.Track
import com.google.common.util.concurrent.MoreExecutors

/** Bridges the UI to the background PlaybackService via a MediaController. */
class PlayerController(context: Context) {
    private var controller: MediaController? = null
    private val byId = HashMap<String, Track>()

    var current by mutableStateOf<Track?>(null); private set
    var isPlaying by mutableStateOf(false); private set
    var shuffle by mutableStateOf(false); private set
    var repeatAll by mutableStateOf(false); private set
    var positionMs by mutableLongStateOf(0L); private set
    var durationMs by mutableLongStateOf(0L); private set

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            isPlaying = player.isPlaying
            shuffle = player.shuffleModeEnabled
            repeatAll = player.repeatMode == Player.REPEAT_MODE_ALL
            current = player.currentMediaItem?.mediaId?.let { byId[it] }
            durationMs = player.duration.coerceAtLeast(0L)
            positionMs = player.currentPosition
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get().also { it.addListener(listener) }
        }, MoreExecutors.directExecutor())
    }

    private fun Track.toItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri(audio)
        .setMediaMetadata(
            MediaMetadata.Builder().setTitle(name).setArtist(artist_name)
                .setAlbumTitle(album_name)
                .setArtworkUri(if (image.isNotBlank()) Uri.parse(image) else null).build()
        ).build()

    fun play(queue: List<Track>, index: Int) {
        val c = controller ?: return
        queue.forEach { byId[it.id] = it }
        c.setMediaItems(queue.map { it.toItem() }, index, 0L)
        c.prepare(); c.play()
    }

    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }
    fun seekTo(ms: Long) { controller?.seekTo(ms); positionMs = ms }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun toggleRepeat() {
        controller?.let {
            it.repeatMode = if (it.repeatMode == Player.REPEAT_MODE_ALL) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL
        }
    }

    /** Called by the UI on a timer while playing. */
    fun refreshPosition() { controller?.let { positionMs = it.currentPosition; durationMs = it.duration.coerceAtLeast(0L) } }

    fun release() { controller?.release(); controller = null }
}
