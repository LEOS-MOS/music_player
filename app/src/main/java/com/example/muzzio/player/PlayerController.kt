package com.example.muzzio.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.muzzio.data.models.Playlist
import com.example.muzzio.presentation.ui.TrackListFragment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    private val player: ExoPlayer
) {

    private var currentPlaylist: Playlist? = null
    var trackListMode: Boolean = false
        private set

    enum class RepeatMode { OFF, ALL, ONE }
    private var repeatMode: RepeatMode = RepeatMode.OFF

    // Только для режима списка (главное меню)
    fun isSameTrack(index: Int): Boolean {
        val current = player.currentMediaItem?.localConfiguration?.uri ?: return false

        if (!trackListMode || currentPlaylist != null) return false

        val newPath = TrackListFragment.tracks.getOrNull(index)?.path ?: return false

        val currentName = current.lastPathSegment ?: return false
        val newName = newPath.substringAfterLast("/")

        return currentName == newName
    }

    fun resetPlaylist() {
        currentPlaylist = null
        trackListMode = true
        player.clearMediaItems()
    }

    fun setPlaylist(playlist: Playlist, startIndex: Int = 0) {
        currentPlaylist = playlist
        trackListMode = false

        player.clearMediaItems()
        playlist.tracks.forEach { path ->
            player.addMediaItem(MediaItem.fromUri(Uri.parse(path)))
        }

        player.prepare()
        player.seekTo(startIndex, 0)
        player.play()
    }

    fun setTrackListFromAllTracks(index: Int) {
        currentPlaylist = null
        trackListMode = true

        player.clearMediaItems()
        TrackListFragment.tracks.forEach { track ->
            player.addMediaItem(MediaItem.fromUri(Uri.parse(track.path)))
        }

        player.prepare()
        player.seekTo(index, 0)
        player.play()
    }

    fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                RepeatMode.ALL
            }
            RepeatMode.ALL -> {
                player.repeatMode = Player.REPEAT_MODE_ONE
                RepeatMode.ONE
            }
            RepeatMode.ONE -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                RepeatMode.OFF
            }
        }
    }

    fun getRepeatMode(): RepeatMode = repeatMode

    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        player.seekToNext()
        player.play()
    }

    fun previous() {
        player.seekToPrevious()
        player.play()
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun getCurrentTitle(): String {
        val index = player.currentMediaItemIndex

        currentPlaylist?.let { playlist ->
            return playlist.tracks
                .getOrNull(index)
                ?.substringAfterLast("/")
                ?.substringBeforeLast(".")
                ?: "No track"
        }

        if (trackListMode) {
            return TrackListFragment.tracks
                .getOrNull(index)
                ?.title
                ?: "No track"
        }

        player.currentMediaItem?.localConfiguration?.uri?.let { uri ->
            return uri.lastPathSegment
                ?.substringAfterLast("/")
                ?.substringBeforeLast(".")
                ?: "No track"
        }

        return "No track"
    }

    fun getCurrentPlaylist(): Playlist? = currentPlaylist
    fun getPlayer(): ExoPlayer = player
}
