package com.example.muzzio.presentation.ui

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.muzzio.R
import com.example.muzzio.databinding.ItemTrackBinding

class PlaylistTracksAdapter(
    private val tracks: MutableList<String>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<PlaylistTracksAdapter.TrackViewHolder>() {

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
        val path = tracks[position]
        val title = path.substringAfterLast("/").substringBeforeLast(".")

        holder.binding.tvTitle.text = title

        val coverBytes = coverCache.getOrPut(path) {
            extractCover(path)
        }

        if (coverBytes != null) {
            val bmp = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
            holder.binding.ivCover.setImageBitmap(bmp)

            // 🔥 Убираем подсветку, если есть обложка
            holder.binding.ivCover.background = null

        } else {
            holder.binding.ivCover.setImageResource(R.drawable.ic_music_placeholder)

            // 🔥 Возвращаем подсветку, если обложки нет
            holder.binding.ivCover.setBackgroundResource(R.drawable.bg_button_gradient)
        }

        holder.itemView.setOnClickListener {
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
