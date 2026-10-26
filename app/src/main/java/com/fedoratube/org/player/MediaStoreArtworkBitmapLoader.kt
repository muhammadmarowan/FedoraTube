package com.fedoratube.org.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.fedoratube.org.data.localmedia.MediaStoreThumbnails
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Session artwork for files on the device. A local video's artwork is its own `content://` URI,
 * which the image decoder can't read as a picture, so the notification had none; this loads a
 * frame or cover through [MediaStoreThumbnails] and leaves every other URI to [delegate].
 */
@OptIn(UnstableApi::class)
internal class MediaStoreArtworkBitmapLoader(
    private val context: Context,
    private val delegate: BitmapLoader,
    private val sizePx: Int,
) : BitmapLoader by delegate {
    private val executor: ListeningExecutorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4))

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        if (!MediaStoreThumbnails.isMediaStoreUri(uri)) {
            return executor.submit<Bitmap> {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(uri.toString())
                        .size(sizePx, sizePx)
                        .allowHardware(false)
                        .build()
                    val result = kotlinx.coroutines.runBlocking {
                        SingletonImageLoader.get(context).execute(request)
                    }
                    val bitmap = (result as? SuccessResult)?.image?.toBitmap()
                    if (bitmap != null) return@submit bitmap
                } catch (_: Exception) {}

                try {
                    delegate.loadBitmap(uri).get()
                } catch (e: Exception) {
                    throw IOException("No artwork for $uri", e)
                }
            }
        }
        return executor.submit<Bitmap> { MediaStoreThumbnails.load(context, uri, sizePx) ?: throw IOException("No artwork for $uri") }
    }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        if (metadata.artworkData != null) {
            return decodeBitmap(metadata.artworkData!!)
        }
        if (metadata.artworkUri != null) {
            return loadBitmap(metadata.artworkUri!!)
        }
        return null
    }
}
