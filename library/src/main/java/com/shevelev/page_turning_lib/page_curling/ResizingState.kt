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

package com.shevelev.page_turning_lib.page_curling

class ResizingState(margins: Margins, scaleFactor: Float) {
    // Margins of OGL-view - for smooth image
    var margins: Margins? = null
        private set

    // Zoom of OGL-camera - for fast resizing (1: in-screen image; 2: zoom value1 2)
    private var currentScaleFactor = MIN_SCALE

    var isResized = false
        private set

    var scaleFactor: Float
        get() = currentScaleFactor
        set(scaleFactor) {
            currentScaleFactor = rangeValue(scaleFactor, MIN_SCALE, MAX_SCALE)
            if (currentScaleFactor <= MIN_SCALE + (MAX_SCALE - MIN_SCALE) * 0.05f) {
                currentScaleFactor = MIN_SCALE
            }
            isResized = currentScaleFactor != MIN_SCALE
        }

    init {
        initMargins(margins)
        this.scaleFactor = scaleFactor
    }

    fun recalculateMarginsByScaleFactor() {
        val oneMargin = MIN_MARGIN * ((currentScaleFactor - MIN_SCALE) / (MAX_SCALE - MIN_SCALE))
        margins = Margins(oneMargin, oneMargin, oneMargin, oneMargin)
    }

    private fun rangeValue(value: Float, minValue: Float, maxValue: Float): Float =
        when {
            value < minValue -> minValue
            value > maxValue -> maxValue
            else -> value
        }

    fun updateScaleFactor(value: Float) {
        scaleFactor = currentScaleFactor + value
    }

    private fun initMargins(margins: Margins) {
        this.margins = Margins(rangeValue(margins.left, MIN_MARGIN, MAX_MARGIN),
            rangeValue(margins.top, MIN_MARGIN, MAX_MARGIN),
            rangeValue(margins.right, MIN_MARGIN, MAX_MARGIN),
            rangeValue(margins.bottom, MIN_MARGIN, MAX_MARGIN))
    }

    companion object {
        const val MAX_MARGIN = 0f
        const val MIN_MARGIN = -0.5f
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 2f
    }
}