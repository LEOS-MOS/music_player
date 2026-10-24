package com.example.muzzio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val player: ExoPlayer
) : ViewModel() {

    fun setPlaylist(list: List<String>, index: Int) {
        player.clearMediaItems()
        list.forEach { path ->
            player.addMediaItem(MediaItem.fromUri(path))
        }

        player.prepare()
        player.seekTo(index, 0)
        player.play()
    }

    fun player() = player

    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() = player.seekToNext()
    fun previous() = player.seekToPrevious()
    fun seekTo(pos: Long) = player.seekTo(pos)
}
