package com.example.muzzio.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.muzzio.data.models.Playlist
import com.example.muzzio.presentation.ui.TrackListFragment
import java.io.File
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

    private fun titleFromPath(path: String): String =
        path.substringAfterLast("/").substringBeforeLast(".")

    /**
     * Один трек в очереди. mediaId = путь к файлу, по нему мы всегда
     * можем понять, какой трек играет. В metadata лежат название
     * (для уведомления) и метка для загрузки обложки.
     */
    private fun buildMediaItem(path: String, title: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtworkUri(CoverBitmapLoader.coverUri(path))
            .build()

        return MediaItem.Builder()
            .setMediaId(path)
            .setUri(Uri.fromFile(File(path)))
            .setMediaMetadata(metadata)
            .build()
    }

    // Только для режима списка (главное меню)
    fun isSameTrack(index: Int): Boolean {
        if (!trackListMode || currentPlaylist != null) return false

        val currentPath = player.currentMediaItem?.mediaId ?: return false
        val newPath = TrackListFragment.tracks.getOrNull(index)?.path ?: return false

        return currentPath == newPath
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
            player.addMediaItem(buildMediaItem(path, titleFromPath(path)))
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
            player.addMediaItem(buildMediaItem(track.path, track.title))
        }

        player.prepare()
        player.seekTo(index, 0)
        player.play()
    }

    /** Трек удалён из плейлиста, который сейчас играет: убираем его и из очереди. */
    fun onPlaylistTrackRemoved(playlist: Playlist, index: Int) {
        val current = currentPlaylist ?: return
        if (current.id != playlist.id) return

        // если это тот же самый список, он уже изменён снаружи
        if (current.tracks !== playlist.tracks && index in current.tracks.indices) {
            current.tracks.removeAt(index)
        }
        if (index in 0 until player.mediaItemCount) {
            player.removeMediaItem(index)
        }
    }

    /** Трек добавлен в плейлист, который сейчас играет: добавляем в конец очереди. */
    fun onPlaylistTrackAdded(playlistId: String, path: String) {
        val current = currentPlaylist ?: return
        if (current.id != playlistId || current.tracks.contains(path)) return

        current.tracks.add(path)
        player.addMediaItem(buildMediaItem(path, titleFromPath(path)))
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

    fun getCurrentTitle(): String =
        player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "No track"

    /** Путь к файлу играющего трека (или null, если очередь пуста). */
    fun getCurrentTrackPath(): String? =
        player.currentMediaItem?.mediaId?.takeIf { it.isNotEmpty() }

    fun getCurrentPlaylist(): Playlist? = currentPlaylist
    fun getPlayer(): ExoPlayer = player
}
