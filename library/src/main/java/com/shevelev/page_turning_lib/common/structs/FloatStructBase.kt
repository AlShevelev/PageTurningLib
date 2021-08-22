package com.shevelev.comics_viewer.common.structs

abstract class FloatStructBase {
    protected fun floatToInt(value: Float, maxIntValue: Int): Int {
        var tmpValue: Int
        tmpValue = if (value.toDouble() == 0.0) 0 else if (value.toDouble() == 1.0) maxIntValue else (maxIntValue * value).toInt()
        if (tmpValue < 0.0) tmpValue = 0 else if (tmpValue > maxIntValue) tmpValue = maxIntValue
        return tmpValue
    }
}