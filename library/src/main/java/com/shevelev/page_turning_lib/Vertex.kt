package com.shevelev.comics_viewer.ui.activities.view_comics

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