package com.example.muzzio.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.example.muzzio.R
import com.example.muzzio.presentation.ui.CoverLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Достаёт обложку трека для уведомления и экрана блокировки.
 * Если у трека нет обложки, рисует сине-фиолетовую градиентную заглушку.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class CoverBitmapLoader(private val context: Context) : BitmapLoader {

    private val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    // сюда приходят байты обложки, которые ExoPlayer сам прочитал из тегов файла
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        executor.submit(Callable<Bitmap> {
            CoverLoader.decodeSampled(data, SIZE_PX) ?: createPlaceholder()
        })

    // сюда приходит наш собственный адрес muzzio-cover:/путь/к/файлу.mp3
    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        executor.submit(Callable<Bitmap> {
            val path = if (uri.scheme == SCHEME) uri.path else null
            val bytes = path?.let { CoverLoader.extractEmbeddedPicture(it) }
            bytes?.let { CoverLoader.decodeSampled(it, SIZE_PX) } ?: createPlaceholder()
        })

    // Заглушка для треков без обложки: тёмно-синий -> королевский -> фиолетовый градиент
    // с белой нотой. Android 12+ подбирает цвет карточки уведомления по цветам обложки,
    // поэтому карточка получается в сине-фиолетовых тонах.
    private fun createPlaceholder(): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF101A66.toInt(), 0xFF3A2BB0.toInt(), 0xFF7E3FC8.toInt())
        ).apply {
            setBounds(0, 0, SIZE_PX, SIZE_PX)
            draw(canvas)
        }

        ContextCompat.getDrawable(context, R.drawable.ic_music_note)?.mutate()?.let {
            val pad = SIZE_PX / 4
            it.setBounds(pad, pad, SIZE_PX - pad, SIZE_PX - pad)
            it.draw(canvas)
        }
        return bitmap
    }

    companion object {
        private const val SCHEME = "muzzio-cover"
        private const val SIZE_PX = 512

        /** Адрес-«метка» для MediaItem: по нему загрузчик находит файл с обложкой. */
        fun coverUri(path: String): Uri =
            Uri.Builder().scheme(SCHEME).path(path).build()
    }
}
