package com.example.muzzio.presentation.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.muzzio.data.models.Track
import com.example.muzzio.databinding.ItemTrackBinding

class TrackAdapter(
    private var tracks: List<Track>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    inner class TrackViewHolder(val binding: ItemTrackBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun update(newTracks: List<Track>) {
        if (tracks == newTracks) return
        tracks = newTracks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemTrackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val holder = TrackViewHolder(binding)
        binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onClick(pos)
        }
        return holder
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]
        holder.binding.tvTitle.text = track.title
        holder.binding.ivCover.showListCover(track.path)
    }

    override fun getItemCount(): Int = tracks.size
}
