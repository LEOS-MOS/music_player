package com.example.muzzio.presentation.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.util.Collections
import java.util.concurrent.Executors

/**
 * Загружает обложки треков в фоне, уменьшает их до нужного размера
 * и кэширует готовые Bitmap. Раньше всё это происходило в главном потоке
 * прямо во время прокрутки списка, из-за чего и были подлагивания.
 */
object CoverLoader {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newFixedThreadPool(2)

    // кэш готовых картинок: ~1/8 доступной памяти приложения
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    // пути треков, у которых точно нет обложки (чтобы не читать файл повторно)
    private val noCover: MutableSet<String> =
        Collections.synchronizedSet(HashSet())

    /**
     * onResult(bitmap) - обложка найдена.
     * onResult(null)   - обложки нет ИЛИ она ещё грузится (показываем заглушку).
     */
    fun load(
        imageView: ImageView,
        path: String,
        targetSizePx: Int,
        onResult: (Bitmap?) -> Unit
    ) {
        val key = "$path#$targetSizePx"
        imageView.tag = key

        cache.get(key)?.let {
            onResult(it)
            return
        }

        onResult(null) // сразу заглушка, пока грузится

        if (noCover.contains(path)) return

        executor.execute {
            val bitmap = decodeCover(path, targetSizePx)
            if (bitmap == null) {
                noCover.add(path)
                return@execute
            }
            cache.put(key, bitmap)
            mainHandler.post {
                // ячейку могли переиспользовать для другого трека
                if (imageView.tag == key) onResult(bitmap)
            }
        }
    }

    private fun decodeCover(path: String, targetSizePx: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val bytes = retriever.embeddedPicture ?: return null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetSizePx &&
                bounds.outHeight / (sample * 2) >= targetSizePx
            ) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }
}
