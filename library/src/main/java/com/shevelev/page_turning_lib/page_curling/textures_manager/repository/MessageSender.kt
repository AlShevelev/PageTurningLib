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

class MessageSender(private val handler: Handler) {
    fun sendBitmapLoaded(bitmap: Bitmap) = send(BitmapRepositoryCallbackCodes.BITMAP, bitmap)

    fun sendLoadingStarted() = send(BitmapRepositoryCallbackCodes.LOADING_STARTED)

    fun sendLoadingCompleted() = send(BitmapRepositoryCallbackCodes.LOADING_COMPLETED)

    fun sendError(ex: Exception) = send(BitmapRepositoryCallbackCodes.ERROR, ex)

    fun sendRepositoryInitialized() = send(BitmapRepositoryCallbackCodes.REPOSITORY_INITIALIZED)

    private fun send(code: Int, value: Any? = null) {
        val message = Message().apply {
            what = code
            obj = value
        }

        handler.sendMessage(message)
    }
}