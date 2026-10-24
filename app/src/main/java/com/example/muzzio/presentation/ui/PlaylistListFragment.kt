package com.example.muzzio.presentation.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.muzzio.R
import com.example.muzzio.data.PlaylistStorage
import com.example.muzzio.data.models.Playlist
import com.example.muzzio.databinding.FragmentPlaylistListBinding

class PlaylistListFragment : Fragment() {

    private var _binding: FragmentPlaylistListBinding? = null
    private val binding get() = _binding!!

    private lateinit var storage: PlaylistStorage
    private lateinit var adapter: PlaylistListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        storage = PlaylistStorage(requireContext())

        adapter = PlaylistListAdapter(storage.getAll()) { playlist ->
            val action = PlaylistListFragmentDirections
                .actionPlaylistListFragmentToPlaylistFragment(playlist.id)
            findNavController().navigate(action)
        }

        binding.recyclerPlaylists.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPlaylists.adapter = adapter

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerPlaylists) { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                bottom
            )
            insets
        }

        // 🔥 Диалог ввода имени плейлиста (без кастомных стилей)
        binding.btnAddPlaylist.setOnClickListener {

            val input = EditText(requireContext()).apply {
                hint = "Введите название"
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Название плейлиста")
                .setView(input)
                .setPositiveButton("Создать") { _, _ ->
                    val name = input.text.toString().trim()

                    if (name.isNotEmpty()) {
                        val new = Playlist(
                            id = System.currentTimeMillis().toString(),
                            name = name,
                            tracks = mutableListOf()
                        )
                        storage.addPlaylist(new)
                        adapter.update(storage.getAll())
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
