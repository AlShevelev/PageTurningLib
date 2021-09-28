/*
 * Copyright (c) 2021 Alexander Shevelev
 *
 * Licensed under the MIT License;
 * ---------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.shevelev.page_turning_lib.page_curling.textures_manager.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import com.shevelev.page_turning_lib.page_curling.ResizingState

/**
 * Loads bitmaps by index
 * @property provider a provider of bitmaps
 */
class BitmapLoader(private val provider: BitmapProvider) {
    fun loadBitmap(index: Int, viewAreaSize: Size): Bitmap {
        provider.getBitmapStream(index).use { stream ->
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            BitmapFactory.decodeStream(stream, null, options) // Read image size only
            val imageWidth = options.outWidth
            val imageHeight = options.outHeight

            stream.reset()

            // Image is in-screen - decode it without scaling
            if (imageHeight <= viewAreaSize.height || imageWidth <= viewAreaSize.width) {
                return BitmapFactory.decodeStream(stream)
            }

            val viewAreaMaxWidth = (viewAreaSize.width * ResizingState.MAX_SCALE).toInt()
            val viewAreaMaxHeight = (viewAreaSize.height * ResizingState.MAX_SCALE).toInt() // Max possible image size
            options.inJustDecodeBounds = false
            options.inSampleSize = calculateInSampleSize(imageWidth, imageHeight, viewAreaMaxWidth, viewAreaMaxHeight)

            return BitmapFactory.decodeStream(stream, null, options)!!
        }
    }

    /**
     * Calculate scaling factor (as power of 2)
     */
    private fun calculateInSampleSize(imageWidth: Int, imageHeight: Int, viewAreaMaxWidth: Int, viewAreaMaxHeight: Int): Int {
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