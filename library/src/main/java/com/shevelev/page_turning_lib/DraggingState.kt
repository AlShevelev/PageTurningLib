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

import android.graphics.RectF
import android.util.Log
import com.shevelev.page_turning_lib.common.structs.Pair

class DraggingState(minMargin: Float, maxMargin: Float) {
    private var startDrag // Stating dragging factor by X and Y
        : Pair<Float>
    private var currentDragging // Current dragging factor by X and Y
        : Pair<Float>
    private var draggingBorders // Absolute
        : RectF? = null
    private var currentDraggingBorders // Based on the current scale
        : RectF? = null
    private val minMargin: Float
    private val maxMargin: Float
    private var currentMargins: Margins? = null
    private var unitsInPixels = 0f // How many units (OGL) in one pixel = 0f

    private fun convertPixelsToUnits(distanceInPixelsX: Float, distanceInPixelsY: Float): Pair<Float> {
        return Pair(distanceInPixelsX * unitsInPixels, -distanceInPixelsY * unitsInPixels)
    }

    fun getDraggingFactor(checkBorders: Boolean): Pair<Float> {
        val result = Pair(startDrag.value1 + currentDragging.value1, startDrag.value2 + currentDragging.value2)
        if (checkBorders) {
            Log.d("DRAGGING", String.format("draggingBorders (ltrb): %1f; %2f; %3f; %4f",
                currentDraggingBorders!!.left, currentDraggingBorders!!.top, currentDraggingBorders!!.right, currentDraggingBorders!!.bottom))
            if (result.value1 > currentDraggingBorders!!.right) // Checking borders
            {
                currentDragging.value1 -= result.value1 - currentDraggingBorders!!.right
                result.value1 = currentDraggingBorders!!.right
            } else if (result.value1 < currentDraggingBorders!!.left) {
                currentDragging.value1 += result.value1 - currentDraggingBorders!!.left
                result.value1 = currentDraggingBorders!!.left
            }
            if (result.value2 > currentDraggingBorders!!.top) {
                currentDragging.value2 -= result.value2 - currentDraggingBorders!!.top
                result.value2 = currentDraggingBorders!!.top
            } else if (result.value2 < currentDraggingBorders!!.bottom) {
                currentDragging.value2 += result.value2 - currentDraggingBorders!!.bottom
                result.value2 = currentDraggingBorders!!.bottom
            }
        }
        Log.d("DRAGGING", String.format("draggingBorders result (xy): %1f; %2f;", result.value1, result.value2))
        return result
    }

    fun startDragging() {
        val oneMargin = currentMargins!!.left
        val scaleFactor = (oneMargin - maxMargin) / (minMargin - maxMargin)
        currentDraggingBorders = RectF(
            draggingBorders!!.left * scaleFactor,
            draggingBorders!!.top * scaleFactor,
            draggingBorders!!.right * scaleFactor,
            draggingBorders!!.bottom * scaleFactor)
    }

    /**
     *
     * @return Dragging factor
     */
    fun processDragging(distanceInPixelsX: Float, distanceInPixelsY: Float): Pair<Float> {
        currentDragging = convertPixelsToUnits(distanceInPixelsX, distanceInPixelsY)
        return getDraggingFactor(true)
    }

    fun completeDragging() {
        startDrag = getDraggingFactor(true) // startDrag + currentDragging (getDraggingFactor)
        currentDragging = Pair(0f, 0f)
    }

    fun reset(): Pair<Float> {
        startDrag = Pair(0f, 0f)
        currentDragging = Pair(0f, 0f)
        return getDraggingFactor(false)
    }

    fun setViewInfo(viewInfo: RendererViewInfo) {
        val viewRect = viewInfo.viewRect
        val viewAreaSize = viewInfo.viewAreaSize
        unitsInPixels = (Math.abs(viewRect.width()) / viewAreaSize.width + Math.abs(viewRect.height()) / viewAreaSize.height) / 2f
        draggingBorders = viewRect
    }

    fun setCurrentMargins(currentMargins: Margins?) {
        this.currentMargins = currentMargins
    }

    init {
        startDrag = Pair(0f, 0f)
        currentDragging = Pair(0f, 0f)
        this.minMargin = minMargin
        this.maxMargin = maxMargin
    }
}