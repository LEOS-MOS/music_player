package com.example.muzzio.presentation.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.muzzio.data.models.Playlist
import com.example.muzzio.databinding.ItemPlaylistBinding

class PlaylistListAdapter(
    private var playlists: MutableList<Playlist>,
    private val onClick: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistListAdapter.PlaylistViewHolder>() {

    inner class PlaylistViewHolder(val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]

        holder.binding.playlistName.text = playlist.name
        holder.binding.playlistCount.text = "${playlist.tracks.size} треков"

        holder.binding.root.setOnClickListener {
            onClick(playlist)
        }
    }

    override fun getItemCount(): Int = playlists.size

    fun update(newList: MutableList<Playlist>) {
        playlists = newList
        notifyDataSetChanged()
    }
}
