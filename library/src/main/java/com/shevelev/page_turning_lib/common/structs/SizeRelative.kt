package com.shevelev.comics_viewer.common.structs

/**
 * Size as a portion of total size
 * 0:0 - is a zero size;
 * 1:1 - is a full size
 */
class SizeRelative(val width: Float, val height: Float) : FloatStructBase() {

    fun toSize(intSize: Size): Size {
        return Size(floatToInt(width, intSize.width), floatToInt(height, intSize.height))
    }

    fun scale(scaleFactor: Float): SizeRelative {
        return SizeRelative(width * scaleFactor, height * scaleFactor)
    }

}