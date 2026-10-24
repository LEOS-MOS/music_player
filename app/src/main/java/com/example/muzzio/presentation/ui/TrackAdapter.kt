package com.example.muzzio.presentation.ui

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
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

    private val coverCache = mutableMapOf<String, ByteArray?>()

    inner class TrackViewHolder(val binding: ItemTrackBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemTrackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]

        holder.binding.tvTitle.text = track.title

        val coverBytes = coverCache.getOrPut(track.path) {
            extractCover(track.path)
        }

        if (coverBytes != null) {
            val bitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
            holder.binding.ivCover.setImageBitmap(bitmap)

            // 🔥 Убираем подсветку, если есть обложка
            holder.binding.ivCover.background = null

        } else {
            holder.binding.ivCover.setImageResource(R.drawable.ic_music_placeholder)

            // 🔥 Возвращаем подсветку, если обложки нет
            holder.binding.ivCover.setBackgroundResource(R.drawable.bg_button_gradient)
        }

        holder.binding.root.setOnClickListener {
            onClick(position)
        }
    }

    override fun getItemCount(): Int = tracks.size

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
}
