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

import android.graphics.RectF

/**
 * Bounding rectangle for [CurlMesh].
 * it consists of 4 vertices:
 * [0] = top-left corner;
 * [1] = bottom-left;
 * [2] = top-right;
 * [3] = bottom-right.
 */
internal class MeshRectangle {
    private val vertexes = arrayOf(Vertex(), Vertex(), Vertex(), Vertex())

    operator fun get(index: Int) = vertexes[index]

    init {
        // Set up shadow penumbra direction to each vertex. We do fake 'self
        // shadow' calculations based on this information.
        vertexes[3].penumbraY = -1.0
        vertexes[1].penumbraY = vertexes[3].penumbraY
        vertexes[1].penumbraX = vertexes[1].penumbraY
        vertexes[0].penumbraX = vertexes[1].penumbraX
        vertexes[3].penumbraX = 1.0
        vertexes[2].penumbraY = vertexes[3].penumbraX
        vertexes[2].penumbraX = vertexes[2].penumbraY
        vertexes[0].penumbraY = vertexes[2].penumbraX
    }

    /**
     * Updates mesh bounds.
     */
    fun update(r: RectF) {
        vertexes[0].posX = r.left.toDouble()
        vertexes[0].posY = r.top.toDouble()
        vertexes[1].posX = r.left.toDouble()
        vertexes[1].posY = r.bottom.toDouble()
        vertexes[2].posX = r.right.toDouble()
        vertexes[2].posY = r.top.toDouble()
        vertexes[3].posX = r.right.toDouble()
        vertexes[3].posY = r.bottom.toDouble()
    }

    /**
     * Sets texture coordinates to vertexes vertices.
     */
    @Synchronized
    fun setTexCoordinates(left: Float, top: Float, right: Float, bottom: Float) {
        vertexes[0].texX = left.toDouble()
        vertexes[0].texY = top.toDouble()
        vertexes[1].texX = left.toDouble()
        vertexes[1].texY = bottom.toDouble()
        vertexes[2].texX = right.toDouble()
        vertexes[2].texY = top.toDouble()
        vertexes[3].texX = right.toDouble()
        vertexes[3].texY = bottom.toDouble()
    }
}