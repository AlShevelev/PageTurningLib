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
import com.shevelev.page_turning_lib.page_curling.*
import com.shevelev.page_turning_lib.page_curling.textures_manager.bitmaps.BitmapRepository
import com.shevelev.page_turning_lib.page_curling.textures_manager.bitmaps.caching.BitmapCache
import java.io.IOException

/**
 * Provide textures for pages and update pages
 */
class PageTexturesManager(private val repository: BitmapRepository)  {
    val pageCount: Int
        get() = repository.pageCount

    private val cache = BitmapCache(4)

    /**
     * Create bitmap for drawing
     * @param width
     * @param height
     * @param index
     * @return
     */
    @Throws(IOException::class)
    private fun loadBitmap(width: Int, height: Int, index: Int): Bitmap =
        cache.get(width xor index) { createBitmap(width, height, index) }

    /**
     * Set bitmap for page - front and back (may be texture or solid color)
     * @param page
     * @param width
     * @param height
     * @param index
     */
    fun updatePage(page: CurlPage, width: Int, height: Int, index: Int) {
        try {
            val bmp = loadBitmap(width, height, index).toSmart(false)

            page.setTexture(bmp, PageSide.Front)
            page.setTexture(bmp, PageSide.Back)
            page.setColor(Color.argb(50, 255, 255, 255), PageSide.Back)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createBitmap(width: Int, height: Int, index: Int): Bitmap {
        val bitmap = repository.getByIndex(index, width, height)
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
        var imageHeight = imageWidth * bitmap!!.height / bitmap.width

        // Inscribe image in draw
        if (imageHeight > r.height() - border * 2) {
            imageHeight = r.height() - border * 2
            imageWidth = imageHeight * bitmap.width / bitmap.height
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

        c.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), r, p)
        return b
    }
}
