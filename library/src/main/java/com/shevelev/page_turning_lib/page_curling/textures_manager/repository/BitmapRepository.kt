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
import android.os.Handler
import android.os.Message
import java.util.*

/**
 * Repository for bitmaps
 * Loads bitmaps and stores them into cache
 * @property provider a provider of bitmaps
 * @property handler a handler for callbacks
 */
class BitmapRepository(
    private val provider: BitmapProvider,
    private val handler: Handler
) {
    private val loader = BitmapLoader(provider)

    private val cache = TreeMap<Int, Bitmap>()

    val pageCount: Int
        get() = provider.total

    fun tryGetByIndex(index: Int, viewAreaWidth: Int, viewAreaHeight: Int) {
        var bitmap = cache[index]

        if(bitmap == null) {
            bitmap = loader.loadBitmap(index, viewAreaWidth, viewAreaHeight)
            cache[index] = bitmap
        }

        sendBitmapLoaded(bitmap)
    }

    private fun sendBitmapLoaded(bitmap: Bitmap) {
        val message = Message().apply {
            what = BitmapRepositoryCallbackCodes.BITMAP
            obj = bitmap
        }

        handler.sendMessage(message)
    }
}
