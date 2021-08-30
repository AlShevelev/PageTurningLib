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

package com.shevelev.page_turning_lib.page_turning

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import androidx.annotation.ColorInt
import java.lang.UnsupportedOperationException

/**
 * Storage class for page textures and blend colors
 */
class CurlPage {
    @ColorInt
    private var colorFront = Color.WHITE

    @ColorInt
    private var colorBack = Color.WHITE

    private var textureFront: Bitmap = createSolidTexture(colorBack)
    private var textureBack: Bitmap = createSolidTexture(colorFront)

    /**
     * Returns true if textures have changed.
     */
    var texturesChanged = false
        private set

    /**
     * Returns true if back siding texture exists and it differs from front
     * facing one.
     */
    val hasBackTexture: Boolean
        get() = textureFront != textureBack

    /**
     * Getter for color.
     */
    @ColorInt
    fun getColor(side: PageSide): Int {
        return when (side) {
            PageSide.Front -> colorFront
            else -> colorBack
        }
    }

    /**
     * Setter blend color.
     */
    fun setColor(color: Int, side: PageSide) {
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
     * Getter for textures. Creates Bitmap sized to nearest power of two, copies
     * original Bitmap into it and returns it. RectF given as parameter is
     * filled with actual texture coordinates in this new upscaled texture
     * Bitmap.
     */
    fun getTexture(textureRect: RectF, side: PageSide): Bitmap {
        return when (side) {
            PageSide.Front -> getTexture(textureFront, textureRect)
            PageSide.Back -> getTexture(textureBack, textureRect)
            else -> throw UnsupportedOperationException("This value is not supported: $side")
        }
    }

    /**
     * Setter for textures.
     */
    fun setTexture(sourceTexture: Bitmap?, side: PageSide) {
        val texture = sourceTexture ?: createSolidTexture(if (side == PageSide.Back) colorBack else colorFront)

        when (side) {
            PageSide.Front -> {
                textureFront.recycle()
                textureFront = texture
            }
            PageSide.Back -> {
                textureBack.recycle()
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
     * Resets this CurlPage into its initial state.
     */
    fun reset() {
        colorBack = Color.WHITE
        colorFront = Color.WHITE
        recycle()
    }

    /**
     * Recycles and frees underlying Bitmaps.
     */
    fun recycle() {
        textureFront.recycle()
        textureBack.recycle()

        // Creates bitmap as small as possible filled with solid color
        textureFront = createSolidTexture(colorFront)
        textureBack = createSolidTexture(colorBack)

        texturesChanged = false
    }

    /**
     * Calculates the next highest power of 2(two) for a given integer.
     */
    private fun getNextHighestPO2(pow: Int): Int {
        var n = pow
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
     * Generates nearest power of two sized Bitmap for give Bitmap. Returns this
     * new Bitmap using default return statement + original texture coordinates
     * are stored into RectF.
     */
    private fun getTexture(bitmap: Bitmap, textureRect: RectF): Bitmap {
        val w = bitmap.width
        val h = bitmap.height

        // Bitmap size expanded to next power of two.
        val newW = getNextHighestPO2(w)
        val newH = getNextHighestPO2(h)

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
     * Create small texture filled by solid color
     */
    private fun createSolidTexture(@ColorInt color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        bmp.eraseColor(color)
        return bmp
    }
}