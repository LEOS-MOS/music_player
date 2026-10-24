package com.example.muzzio.presentation.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.muzzio.R
import com.example.muzzio.data.models.Track
import com.example.muzzio.databinding.ItemTrackBinding

class TrackAdapter(
    private val tracks: List<Track>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    inner class TrackViewHolder(val binding: ItemTrackBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemTrackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val holder = TrackViewHolder(binding)
        // слушатель кликов ставим один раз, а не при каждой прокрутке
        binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onClick(pos)
        }
        return holder
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]
        val cover = holder.binding.ivCover

        holder.binding.tvTitle.text = track.title

        val sizePx = (48 * cover.resources.displayMetrics.density).toInt()

        CoverLoader.load(cover, track.path, sizePx) { bitmap ->
            if (bitmap != null) {
                cover.setImageBitmap(bitmap)
                cover.background = null
            } else {
                cover.setImageResource(R.drawable.ic_music_placeholder)
                cover.setBackgroundResource(R.drawable.bg_button_gradient)
            }
        }
    }

    override fun getItemCount(): Int = tracks.size
}
