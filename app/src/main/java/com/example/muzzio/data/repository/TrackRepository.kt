package com.example.muzzio.data.repository

import android.content.ContentResolver
import android.provider.MediaStore
import com.example.muzzio.data.models.Track
import java.io.File
import javax.inject.Inject

class TrackRepository @Inject constructor(
    private val contentResolver: ContentResolver
) {

    /**
     * Все аудиофайлы устройства (любые папки и форматы), кроме рингтонов,
     * уведомлений и будильников. Отсортированы по названию.
     * Вызывать из фонового потока.
     */
    fun getDeviceTracks(): List<Track> {
        val tracks = ArrayList<Track>()

        val projection = arrayOf(MediaStore.Audio.Media.DATA)

        // Если нужны только "настоящие" песни (без голосовых, записей и т.п.),
        // замени условие на: "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val selection = "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
                "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
                "${MediaStore.Audio.Media.IS_ALARM} = 0"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataIndex) ?: continue
                val file = File(path)
                // файл могли удалить, а в MediaStore запись ещё осталась
                if (!file.isFile) continue

                tracks.add(Track(file.nameWithoutExtension, path))
            }
        }

        return tracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }
}
