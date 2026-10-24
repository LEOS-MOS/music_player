package com.example.muzzio.presentation.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import com.example.muzzio.R
import java.util.Collections
import java.util.concurrent.Executors

/**
 * Загружает обложки треков в фоне, уменьшает их до нужного размера
 * и кэширует готовые Bitmap.
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

    fun decodeCover(path: String, targetSizePx: Int): Bitmap? =
        extractEmbeddedPicture(path)?.let { decodeSampled(it, targetSizePx) }

    /** Достаёт встроенную обложку из аудиофайла (сырые байты картинки). */
    fun extractEmbeddedPicture(path: String): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.embeddedPicture
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /** Декодирует картинку сразу в уменьшенном размере, чтобы не тратить память. */
    fun decodeSampled(bytes: ByteArray, targetSizePx: Int): Bitmap? {
        return try {
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
        }
    }
}

/**
 * Показывает обложку трека в списке: скруглённую картинку или
 * скруглённую градиентную заглушку, если обложки нет.
 */
fun ImageView.showListCover(path: String) {
    val density = resources.displayMetrics.density
    val sizePx = (48 * density).toInt()

    CoverLoader.load(this, path, sizePx) { bitmap ->
        if (bitmap != null) {
            setPadding(0, 0, 0, 0)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.bg_cover_list_rounded)
            setImageBitmap(bitmap)
        } else {
            val pad = (2 * density).toInt()
            setPadding(pad, pad, pad, pad)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.bg_cover_list_placeholder)
            setImageResource(R.drawable.ic_music_placeholder)
        }
    }
}
