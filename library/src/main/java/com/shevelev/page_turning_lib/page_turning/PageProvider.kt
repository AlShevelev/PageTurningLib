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

/**
 * Provider for feeding 'book' with bitmaps which are used for rendering pages.
 */
interface PageProvider {
    /**
     * Return number of pages available.
     */
    val pageCount: Int

    /**
     * Called once new bitmaps/textures are needed. Width and height are in
     * pixels telling the size it will be drawn on screen and following them
     * ensures that aspect ratio remains. But it's possible to return bitmap
     * of any size though. You should use provided CurlPage for storing page
     * information for requested page number.<br></br>
     * <br></br>
     * Index is a number between 0 and getBitmapCount() - 1.
     */
    fun updatePage(page: CurlPage, width: Int, height: Int, index: Int)
}