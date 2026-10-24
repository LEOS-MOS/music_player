package com.example.muzzio.data.repository

import android.content.ContentResolver
import android.provider.MediaStore
import com.example.muzzio.data.models.Track
import javax.inject.Inject

class TrackRepository @Inject constructor(
    private val contentResolver: ContentResolver
) {

    fun getDeviceTracks(): List<Track> {
        val tracks = mutableListOf<Track>()

        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA
        )

        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val titleIndex = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val dataIndex = it.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val title = it.getString(titleIndex)
                val path = it.getString(dataIndex)
                tracks.add(Track(title, path))
            }
        }

        return tracks
    }
}
