package com.example.muzzio.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.muzzio.data.PlaylistStorage
import com.example.muzzio.data.models.Track
import com.example.muzzio.databinding.FragmentTrackListBinding
import com.example.muzzio.domain.usecase.GetTracksUseCase
import com.example.muzzio.player.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class TrackListFragment : Fragment() {

    @Inject
    lateinit var controller: PlayerController

    @Inject
    lateinit var getTracks: GetTracksUseCase

    private var _binding: FragmentTrackListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TrackAdapter

    companion object {
        var tracks: List<Track> = emptyList()
    }

    private var playlistId: String? = null

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) loadTracksAndInit()
            else Toast.makeText(requireContext(), "Нет доступа к файлам", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = arguments?.getString("playlistId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        checkPermission()
    }

    private fun checkPermission() {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_AUDIO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission)
                    == PackageManager.PERMISSION_GRANTED -> loadTracksAndInit()

            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(requireContext(), "Нужно разрешение для загрузки музыки", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }

            else -> requestPermissionLauncher.launch(permission)
        }
    }

    private fun loadTracksAndInit() {
        // сразу показываем то, что уже загружено раньше (если есть),
        // а свежий список подгружаем в фоне
        adapter = TrackAdapter(tracks) { index -> onTrackClicked(index) }

        binding.recyclerTracks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTracks.adapter = adapter

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerTracks) { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                bottom
            )
            insets
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) { getTracks() }
            tracks = loaded
            adapter.update(loaded)
        }
    }

    private fun onTrackClicked(index: Int) {
        if (playlistId == null) {
            // очередь для главного меню создаётся здесь
            if (controller.getCurrentPlaylist() != null || !controller.isSameTrack(index)) {
                controller.resetPlaylist()
                controller.setTrackListFromAllTracks(index)
            }

            val action = TrackListFragmentDirections
                .actionTrackListFragmentToPlayerFragment(index)
            findNavController().navigate(action)

        } else {
            val storage = PlaylistStorage(requireContext())
            val playlist = storage.getAll().first { it.id == playlistId }
            val path = tracks[index].path

            if (!playlist.tracks.contains(path)) {
                playlist.tracks.add(path)
                storage.updatePlaylist(playlist)
                // если этот плейлист сейчас играет, трек сразу попадёт и в очередь
                controller.onPlaylistTrackAdded(playlist.id, path)
                Toast.makeText(requireContext(), "Трек добавлен", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Трек уже в плейлисте", Toast.LENGTH_SHORT).show()
            }

            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
