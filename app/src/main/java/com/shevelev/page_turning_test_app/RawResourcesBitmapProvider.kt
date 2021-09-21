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

package com.shevelev.page_turning_test_app

import android.content.Context
import com.shevelev.page_turning_lib.page_curling.textures_manager.repository.BitmapProvider
import java.io.InputStream

/**
 * Provides bitmaps from Raw resources of the app
 */
class RawResourcesBitmapProvider(private val context: Context) : BitmapProvider {
    private val bitmapIds = listOf(R.raw.p240035, R.raw.p7240031, R.raw.p7240039, R.raw.p8010067, R.raw.p8150085, R.raw.p8150090)

    /**
     * Total quantity of bitmap
     */
    override val total: Int
        get() = bitmapIds.size

    /**
     * Returns a stream of bitmap data for given bitmap index
     * The stream will be closed by caller
     */
    override fun getBitmapStream(index: Int): InputStream = context.resources.openRawResource(bitmapIds[index])
}