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
 * Holder for vertex information.
 */
internal class Vertex {
    var mColor = 0
    var mColorFactor: Float
    var mPenumbraX = 0.0
    var mPenumbraY = 0.0
    var mPosX: Double
    var mPosY: Double
    var mPosZ: Double
    var mTexX: Double
    var mTexY = 0.0
    fun rotateZ(theta: Double) {
        val cos = Math.cos(theta)
        val sin = Math.sin(theta)
        val x = mPosX * cos + mPosY * sin
        val y = mPosX * -sin + mPosY * cos
        mPosX = x
        mPosY = y
        val px = mPenumbraX * cos + mPenumbraY * sin
        val py = mPenumbraX * -sin + mPenumbraY * cos
        mPenumbraX = px
        mPenumbraY = py
    }

    fun set(vertex: Vertex) {
        mPosX = vertex.mPosX
        mPosY = vertex.mPosY
        mPosZ = vertex.mPosZ
        mTexX = vertex.mTexX
        mTexY = vertex.mTexY
        mPenumbraX = vertex.mPenumbraX
        mPenumbraY = vertex.mPenumbraY
        mColor = vertex.mColor
        mColorFactor = vertex.mColorFactor
    }

    fun translate(dx: Double, dy: Double) {
        mPosX += dx
        mPosY += dy
    }

    init {
        mTexX = mTexY
        mPosZ = mTexX
        mPosY = mPosZ
        mPosX = mPosY
        mColorFactor = 1.0f
    }
}