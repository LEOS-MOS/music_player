package com.example.muzzio.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
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

    // Обновляет ползунок и время. Повторяется, только пока музыка играет.
    private val progressRunnable = object : Runnable {
        override fun run() {
            if (_binding == null) return
            updateProgress()
            if (controller.getPlayer().isPlaying) {
                handler.postDelayed(this, 200)
            }
        }
    }

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (_binding == null) return

            updatePlayPauseIcon(isPlaying)
            binding.tvTitle.text = controller.getCurrentTitle()
            startProgress()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (_binding == null) return

            binding.tvTitle.text = controller.getCurrentTitle()
            updateCover()
            startProgress()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (_binding == null) return
            startProgress()
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
        updatePlayPauseIcon(player.isPlaying)
        updateCover()
        updateRepeatIcon()

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                player.seekTo(seekBar?.progress?.toLong() ?: 0L)
                startProgress()
            }
        })

        binding.btnPlayPause.setOnClickListener { controller.toggle() }
        binding.btnNext.setOnClickListener { controller.next() }
        binding.btnPrev.setOnClickListener { controller.previous() }

        binding.btnRepeat.setOnClickListener {
            controller.toggleRepeat()
            updateRepeatIcon()
        }

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    // таймер работает, только пока экран плеера виден
    override fun onStart() {
        super.onStart()
        startProgress()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(progressRunnable)
    }

    private fun startProgress() {
        handler.removeCallbacks(progressRunnable)
        handler.post(progressRunnable)
    }

    private fun updateProgress() {
        if (_binding == null || isUserSeeking) return

        val p = controller.getPlayer()
        val duration = p.duration.coerceAtLeast(0L)

        binding.seekBar.max = duration.toInt().coerceAtLeast(1)
        binding.seekBar.progress = p.currentPosition.toInt()
        binding.tvCurrentTime.text = formatTime(p.currentPosition)
        binding.tvTotalTime.text = formatTime(duration)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
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

    private fun updateCover() {
        val trackPath = controller.getCurrentTrackPath() ?: return

        val cover = binding.ivCoverBig
        val density = resources.displayMetrics.density

        CoverLoader.load(cover, trackPath, (300 * density).toInt()) { bitmap ->
            if (_binding == null) return@load
            if (bitmap != null) {
                cover.scaleType = ImageView.ScaleType.CENTER_CROP
                cover.setPadding(0, 0, 0, 0)
                cover.setImageBitmap(bitmap)
            } else {
                val pad = (56 * density).toInt()
                cover.scaleType = ImageView.ScaleType.CENTER_INSIDE
                cover.setPadding(pad, pad, pad, pad)
                cover.setImageResource(R.drawable.ic_music_note)
            }
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

        handler.removeCallbacks(progressRunnable)
        controller.getPlayer().removeListener(playerListener)

        _binding = null
    }
}
