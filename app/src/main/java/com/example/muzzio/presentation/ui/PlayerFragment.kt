package com.example.muzzio.presentation.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.fragment.navArgs
import com.example.muzzio.R
import com.example.muzzio.databinding.FragmentPlayerBinding
import com.example.muzzio.player.PlayerController
import com.example.muzzio.service.MusicService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlayerFragment : Fragment() {

    @Inject
    lateinit var controller: PlayerController

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val args: PlayerFragmentArgs by navArgs()

    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false
    private var uiUpdater: Runnable? = null

    private val coverCache = mutableMapOf<String, ByteArray?>()

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (_binding == null) return

            binding.btnPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )

            binding.tvTitle.text = controller.getCurrentTitle()
            updateCover()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (_binding == null) return

            binding.tvTitle.text = controller.getCurrentTitle()
            updateCover()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // просто подключаемся к уже настроенному плееру
        requireContext().startService(Intent(requireContext(), MusicService::class.java))

        val player = controller.getPlayer()
        player.addListener(playerListener)

        binding.tvTitle.text = controller.getCurrentTitle()
        updateCover()
        updateRepeatIcon()

        binding.seekBar.max = player.duration.toInt().coerceAtLeast(1)

        binding.seekBar.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(
                seekBar: android.widget.SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    binding.tvCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                isUserSeeking = false
                player.seekTo(seekBar?.progress?.toLong() ?: 0L)
            }
        })

        binding.btnPlayPause.setOnClickListener { controller.toggle() }
        binding.btnNext.setOnClickListener { controller.next() }
        binding.btnPrev.setOnClickListener { controller.previous() }

        binding.btnRepeat.setOnClickListener {
            controller.toggleRepeat()
            updateRepeatIcon()
        }

        uiUpdater = object : Runnable {
            override fun run() {
                if (_binding == null) return

                val p = controller.getPlayer()

                if (!isUserSeeking) {
                    binding.seekBar.max = p.duration.toInt().coerceAtLeast(1)
                    binding.seekBar.progress = p.currentPosition.toInt()
                    binding.tvCurrentTime.text = formatTime(p.currentPosition)
                    binding.tvTotalTime.text = formatTime(p.duration)
                }

                handler.postDelayed(this, 200)
            }
        }
        handler.post(uiUpdater!!)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun updateRepeatIcon() {
        when (controller.getRepeatMode()) {
            PlayerController.RepeatMode.OFF ->
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat_off)

            PlayerController.RepeatMode.ALL ->
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat_all)

            PlayerController.RepeatMode.ONE ->
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat_one)
        }
    }

    private fun extractCover(path: String): ByteArray? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val data = retriever.embeddedPicture
            retriever.release()
            data
        } catch (e: Exception) {
            null
        }
    }

    private fun updateCover() {
        val player = controller.getPlayer()
        val index = player.currentMediaItemIndex

        val trackPath = controller.getCurrentPlaylist()
            ?.tracks
            ?.getOrNull(index)
            ?: TrackListFragment.tracks.getOrNull(index)?.path
            ?: return

        val coverBytes = coverCache.getOrPut(trackPath) {
            extractCover(trackPath)
        }

        if (coverBytes != null) {
            val bitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
            binding.ivCoverBig.setImageBitmap(bitmap)
        } else {
            binding.ivCoverBig.setImageResource(R.drawable.ic_music_note)
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        uiUpdater?.let { handler.removeCallbacks(it) }
        uiUpdater = null

        controller.getPlayer().removeListener(playerListener)

        _binding = null
    }
}
