package com.shevelev.comics_viewer.common.structs

class PointF(var left: Float, var top: Float) : FloatStructBase() {
    fun toPoint(intSize: Size): Point {
        return Point(floatToInt(left, intSize.width), floatToInt(top, intSize.height))
    }

}