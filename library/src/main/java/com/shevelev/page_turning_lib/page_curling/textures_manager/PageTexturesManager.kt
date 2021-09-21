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

package com.shevelev.page_turning_lib.page_curling.textures_manager

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.LruCache
import com.shevelev.page_turning_lib.page_curling.*
import com.shevelev.page_turning_lib.page_curling.textures_manager.repository.BitmapProvider
import com.shevelev.page_turning_lib.page_curling.textures_manager.repository.BitmapRepository
import com.shevelev.page_turning_lib.page_curling.textures_manager.repository.BitmapRepositoryCallbackCodes

/**
 * Provide textures for pages and update pages
 */
class PageTexturesManager(
    provider: BitmapProvider,
    private val viewInvalidator: CurlViewInvalidator
) {
    val pageCount: Int
        get() = repository.pageCount

    private val cache = object: LruCache<Int, Bitmap>(4) {
        override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            oldValue?.recycle()
        }
    }

    private val repository = BitmapRepository(provider, Handler(Looper.getMainLooper(), this::processRepositoryMessage))

    private var updatingState: PageTexturesManagerState? = null

    private var loadingEventsHandler: PageLoadingEventsHandler? = null

    /**
     * Set bitmap for page - front and back (may be texture or solid color)
     * @param page updated page
     * @param width page texture width
     * @param height page texture height
     * @param index page index
     */
    fun updatePage(page: CurlPage, width: Int, height: Int, index: Int) {
        try {
            val texture = cache.get(width xor index)

            if(texture != null) {
                updatePage(page, texture)
            } else {
                updatingState = PageTexturesManagerState(page, width, height, index)
                repository.tryGetByIndex(index, width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setOnLoadingListener(handler: PageLoadingEventsHandler?) {
        loadingEventsHandler = handler
    }

    private fun updatePage(page: CurlPage, texture: Bitmap) {
        texture.toSmart(false).let {
            page.setTexture(it, PageSide.Front)
            page.setTexture(it, PageSide.Back)
            page.setColor(Color.argb(50, 255, 255, 255), PageSide.Back)

            // We need to re-render view manually
            viewInvalidator.renderNow()
        }
    }

    private fun createTexture(width: Int, height: Int, sourceBitmap: Bitmap): Bitmap {
        val b = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565) // For memory saving
        b.eraseColor(-0x1)
        val c = Canvas(b)
        val margin = 7

        // Thin frame around image
        val border = 3

        // Image's frame
        val r = Rect(margin, margin, width - margin, height - margin)

        // Scale image with saving proportions
        var imageWidth = r.width() - border * 2
        var imageHeight = imageWidth * sourceBitmap.height / sourceBitmap.width

        // Inscribe image in draw
        if (imageHeight > r.height() - border * 2) {
            imageHeight = r.height() - border * 2
            imageWidth = imageHeight * sourceBitmap.width / sourceBitmap.height
        }

        // Place image's rect on center
        r.left += (r.width() - imageWidth) / 2 - border
        r.right = r.left + imageWidth + border + border
        r.top += (r.height() - imageHeight) / 2 - border
        r.bottom = r.top + imageHeight + border + border

        val p = Paint()
        // Draw violet frame around image
        p.color = -0x3f3f40
        c.drawRect(r, p)
        r.left += border
        r.right -= border
        r.top += border
        r.bottom -= border

        c.drawBitmap(sourceBitmap, Rect(0, 0, sourceBitmap.width, sourceBitmap.height), r, p)
        return b
    }

    private fun processRepositoryMessage(message: Message): Boolean {
        when(message.what) {
            BitmapRepositoryCallbackCodes.BITMAP -> {
                val sourceBitmap = message.obj as Bitmap

                updatingState?.let { state ->
                    val texture = createTexture(state.width, state.height, sourceBitmap)
                    cache.put(state.width xor state.index, texture)
                    updatePage(state.page, texture)
                }

                updatingState = null
            }

            BitmapRepositoryCallbackCodes.LOADING_STARTED -> loadingEventsHandler?.onLoadingStarted()

            BitmapRepositoryCallbackCodes.LOADING_COMPLETED -> loadingEventsHandler?.onLoadingCompleted()

            BitmapRepositoryCallbackCodes.ERROR -> loadingEventsHandler?.onLoadingError()
        }
        return true
    }
}
