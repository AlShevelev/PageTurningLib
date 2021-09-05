package com.shevelev.page_turning_test_app.page_provider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.shevelev.page_turning_lib.page_curling.ResizingState
import com.shevelev.page_turning_test_app.R
import java.util.*

/**
 * Caches one bitmap of max size per page
 */
class BitmapRepositoryImpl(private val context: Context) : BitmapRepository {
    private val cachedBitmaps: MutableMap<Int, Bitmap> = TreeMap()

    override val pageCount: Int = 6

    private val bitmapIds = listOf(R.raw.p240035, R.raw.p7240031, R.raw.p7240039, R.raw.p8010067, R.raw.p8150085, R.raw.p8150090)

    override fun getByIndex(index: Int, viewAreaWidth: Int, viewAreaHeight: Int): Bitmap {
        return cachedBitmaps[index]
            ?: loadBitmap(index, viewAreaWidth, viewAreaHeight)
                .also {
                    cachedBitmaps[index] = it
                }
    }

    private fun loadBitmap(index: Int, viewAreaWidth: Int, viewAreaHeight: Int): Bitmap {
        context.resources.openRawResource(bitmapIds[index]).use { stream ->
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            BitmapFactory.decodeStream(stream, null, options) // Read image size only
            val imageWidth = options.outWidth
            val imageHeight = options.outHeight

            stream.reset()

            if (imageHeight <= viewAreaHeight || imageWidth <= viewAreaWidth) // Image is in-screen - decode it without scaling
                return BitmapFactory.decodeStream(stream)

            val viewAreaMaxWidth = (viewAreaWidth * ResizingState.MAX_SCALE).toInt()
            val viewAreaMaxHeight = (viewAreaHeight * ResizingState.MAX_SCALE).toInt() // Max possible image size
            options.inJustDecodeBounds = false
            options.inSampleSize = calculateInSampleSize(imageWidth, imageHeight, viewAreaMaxWidth, viewAreaMaxHeight)

            return BitmapFactory.decodeStream(stream, null, options)!!
        }
    }

    companion object {
        /**
         * Calculate scaling factor (as power of 2)
         */
        fun calculateInSampleSize(imageWidth: Int, imageHeight: Int, viewAreaMaxWidth: Int, viewAreaMaxHeight: Int): Int {
            var inSampleSize = 1
            if (imageHeight > viewAreaMaxHeight || imageWidth > viewAreaMaxWidth) {
                val halfHeight = imageHeight / 2
                val halfWidth = imageWidth / 2
                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
// height and width larger than the requested height and width.
                while (halfHeight / inSampleSize > viewAreaMaxHeight || (halfHeight / inSampleSize).toFloat() / viewAreaMaxHeight.toFloat() > 0.7f ||
                    halfWidth / inSampleSize > viewAreaMaxHeight || (halfWidth / inSampleSize).toFloat() / viewAreaMaxHeight.toFloat() > 0.7f) inSampleSize *= 2
            }
            return inSampleSize
        }
    }
}
