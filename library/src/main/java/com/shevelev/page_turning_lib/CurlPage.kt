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

package com.shevelev.page_turning_lib

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF

/**
 * Storage class for page textures, blend colors and possibly some other values
 * in the future.
 */
class CurlPage {
    private var colorBack = 0
    private var colorFront = 0
    private var textureBack: Bitmap? = null
    private var textureFront: Bitmap? = null
    /**
     * Returns true if textures have changed.
     */
    var texturesChanged = false
        private set

    /**
     * Getter for color.
     */
    fun getColor(side: PageSide?): Int {
        return when (side) {
            PageSide.Front -> colorFront
            else -> colorBack
        }
    }

    /**
     * Calculates the next highest power of 2(two) for a given integer.
     */
    private fun getNextHighestPO2(n: Int): Int {
        var n = n
        n -= 1
        n = n or (n shr 1)
        n = n or (n shr 2)
        n = n or (n shr 4)
        n = n or (n shr 8)
        n = n or (n shr 16)
        n = n or (n shr 32)
        return n + 1
    }

    /**
     * Create texture from given bitmap
     * -----------------------------------------------
     * Generates nearest power of two sized Bitmap for give Bitmap. Returns this
     * new Bitmap using default return statement + original texture coordinates
     * are stored into RectF.
     */
    private fun getTexture(bitmap: Bitmap?, textureRect: RectF): Bitmap { // Bitmap original size.
        val w = bitmap!!.width
        val h = bitmap.height
        // Bitmap size expanded to next power of two. This is done due to
// the requirement on many devices, texture width and height should
// be power of two.
        val newW = getNextHighestPO2(w)
        val newH = getNextHighestPO2(h)
        // TODO: Is there another way to setDiskItems a bigger Bitmap and copy
// original Bitmap to it more efficiently? Immutable bitmap anyone?
//		Bitmap bitmapTex = Bitmap.createBitmap(newW, newH, bitmap.getConfig());
        val bitmapTex = Bitmap.createBitmap(newW, newH, Bitmap.Config.RGB_565)
        val c = Canvas(bitmapTex)
        c.drawBitmap(bitmap, 0f, 0f, null)
        // Calculate final texture coordinates.
        val texX = w.toFloat() / newW
        val texY = h.toFloat() / newH
        textureRect[0f, 0f, texX] = texY
        return bitmapTex
    }

    /**
     * Getter for textures. Creates Bitmap sized to nearest power of two, copies
     * original Bitmap into it and returns it. RectF given as parameter is
     * filled with actual texture coordinates in this new upscaled texture
     * Bitmap.
     */
    fun getTexture(textureRect: RectF, side: PageSide?): Bitmap {
        return when (side) {
            PageSide.Front -> getTexture(textureFront, textureRect)
            else -> getTexture(textureBack, textureRect)
        }
    }

    /**
     * Returns true if back siding texture exists and it differs from front
     * facing one.
     */
    fun hasBackTexture(): Boolean {
        return textureFront != textureBack
    }

    /**
     * Recycles and frees underlying Bitmaps.
     */
    fun recycle() {
        if (textureFront != null) textureFront!!.recycle() // Free memory
        textureFront = createSolidTexture(colorFront) // Create bitmap as small as possible filled with solid color
        if (textureBack != null) textureBack!!.recycle()
        textureBack = createSolidTexture(colorBack)
        texturesChanged = false
    }

    /**
     * Create small texture filled by solid color
     * @param color
     * @return
     */
    private fun createSolidTexture(color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        bmp.eraseColor(color)
        return bmp
    }

    /**
     * Resets this CurlPage into its initial state.
     */
    fun reset() {
        colorBack = Color.WHITE
        colorFront = Color.WHITE
        recycle()
    }

    /**
     * Setter blend color.
     */
    fun setColor(color: Int, side: PageSide?) {
        when (side) {
            PageSide.Front -> colorFront = color
            PageSide.Back -> colorBack = color
            else -> {
                colorBack = color
                colorFront = colorBack
            }
        }
    }

    /**
     * Setter for textures.
     */
    fun setTexture(texture: Bitmap?, side: PageSide) {
        var texture = texture
        if (texture == null) texture = createSolidTexture(if (side == PageSide.Back) colorBack else colorFront)
        when (side) {
            PageSide.Front -> {
                if (textureFront != null) textureFront!!.recycle()
                textureFront = texture
            }
            PageSide.Back -> {
                if (textureBack != null) textureBack!!.recycle()
                textureBack = texture
            }
            PageSide.Both -> {
                setTexture(texture, PageSide.Front)
                setTexture(texture, PageSide.Back)
            }
        }
        texturesChanged = true
    }

    /**
     * Default constructor.
     */
    init {
        reset()
    }
}