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

import kotlin.math.cos
import kotlin.math.sin

/**
 * Holder for vertex information.
 */
internal class Vertex {
    var color = 0
    var colorFactor = 1.0f

    var penumbraX = 0.0
    var penumbraY = 0.0

    var posX: Double
    var posY: Double
    var posZ: Double

    var texX: Double
    var texY = 0.0

    init {
        texX = texY

        posZ = texX
        posY = posZ
        posX = posY
    }

    fun rotateZ(theta: Double) {
        val cos = cos(theta)
        val sin = sin(theta)

        val x = posX * cos + posY * sin
        val y = posX * -sin + posY * cos

        posX = x
        posY = y

        val px = penumbraX * cos + penumbraY * sin
        val py = penumbraX * -sin + penumbraY * cos

        penumbraX = px
        penumbraY = py
    }

    fun set(vertex: Vertex) {
        posX = vertex.posX
        posY = vertex.posY
        posZ = vertex.posZ

        texX = vertex.texX
        texY = vertex.texY

        penumbraX = vertex.penumbraX
        penumbraY = vertex.penumbraY

        color = vertex.color
        colorFactor = vertex.colorFactor
    }

    fun translate(dx: Double, dy: Double) {
        posX += dx
        posY += dy
    }
}