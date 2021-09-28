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
import android.util.Size
import java.util.*

class ConcurrentCache(
    private val bitmapLoader: BitmapLoader,
    private val totalBitmaps: Int
) {
    private val cache = TreeMap<Int, Bitmap>()

    val isEmpty: Boolean
        @Synchronized
        get() = cache.isEmpty()

    /**
     * Extract bitmap by its page index
     */
    @Synchronized
    operator fun get(index: Int): Bitmap? = cache[index]

    @Synchronized
    fun clear() {
        val values = cache.values.toList()
        cache.clear()
        values.forEach { it.takeIf { !it.isRecycled }?.recycle() }
    }

    /**
     * Update the cache if it's needed
     * @param index loaded bitmap's index
     * @param viewAreaSize bitmap's size
     */
    fun update(index: Int, viewAreaSize: Size) {
        if(needStop()) return

        val currentIndexes = getAllKeys()
        val targetIndexes = getTargetInCacheIndexes(index)

        val indexesToRemove = currentIndexes - targetIndexes
        val indexesToAdd = targetIndexes - currentIndexes

        if(needStop()) return

        val bitmapsToAdd = mutableListOf<Bitmap>()
        indexesToAdd.forEach {
            bitmapsToAdd.add(bitmapLoader.loadBitmap(it, viewAreaSize))

            if(needStop()) return
        }

        updateCache(indexesToRemove, indexesToAdd, bitmapsToAdd)
    }

    private fun getTargetInCacheIndexes(index: Int): IntRange {
        val min = (index - TOTAL_EXTRA_BITMAPS).takeIf { it >= 0 } ?: 0
        val max = (index + TOTAL_EXTRA_BITMAPS).takeIf { it <= totalBitmaps - 1 } ?: totalBitmaps - 1

        return IntRange(min, max)
    }

    @Synchronized
    private fun getAllKeys() = cache.keys.toList()

    @Synchronized
    private fun updateCache(indexesToRemove: List<Int>, indexesToAdd: List<Int>, bitmapsToAdd: List<Bitmap>) {
        indexesToRemove.forEach {
            cache.remove(it)?.takeIf { bitmap -> !bitmap.isRecycled }?.recycle()
        }

        indexesToAdd.forEachIndexed { i, index -> cache[index] = bitmapsToAdd[i] }
    }

    private fun needStop() = Thread.currentThread().isInterrupted

    companion object {
        /**
         * Total bitmaps in the cache (must be odd)
         */
        private const val TOTAL_BITMAPS = 5

        /**
         * Number of bitmaps before and after a current one
         */
        private const val TOTAL_EXTRA_BITMAPS = TOTAL_BITMAPS / 2
    }
}