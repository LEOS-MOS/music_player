package com.example.muzzio.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.muzzio.R
import com.example.muzzio.data.PlaylistStorage
import com.example.muzzio.data.models.Playlist
import com.example.muzzio.databinding.FragmentPlaylistBinding
import com.example.muzzio.player.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaylistFragment : Fragment() {

    @Inject lateinit var controller: PlayerController

    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    private lateinit var playlist: Playlist
    private lateinit var storage: PlaylistStorage
    private lateinit var adapter: PlaylistTracksAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        storage = PlaylistStorage(requireContext())

        val playlistId = arguments?.getString("playlistId")!!
        playlist = storage.getAll().first { it.id == playlistId }

        binding.tvPlaylistName.text = playlist.name

        // 🔙 Кнопка назад
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        adapter = PlaylistTracksAdapter(playlist.tracks) { index ->
            controller.setPlaylist(playlist, index)

            val bundle = bundleOf("index" to index)
            findNavController().navigate(R.id.playerFragment, bundle)
        }


        binding.recyclerTracks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTracks.adapter = adapter

        // 🔥 Адаптивный нижний отступ
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

        // 🔥 Свайп‑удаление треков
        val swipeToDelete = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val index = viewHolder.adapterPosition

                // удаляем трек из плейлиста
                playlist.tracks.removeAt(index)
                storage.updatePlaylist(playlist)

                // обновляем адаптер
                adapter.notifyItemRemoved(index)
            }
        }

        ItemTouchHelper(swipeToDelete).attachToRecyclerView(binding.recyclerTracks)

        binding.btnAddTrack.setOnClickListener {
            val action = PlaylistFragmentDirections
                .actionPlaylistFragmentToTrackListFragment(playlist.id)
            findNavController().navigate(action)
        }

        binding.btnDeletePlaylist.setOnClickListener {
            storage.deletePlaylist(playlist.id)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
