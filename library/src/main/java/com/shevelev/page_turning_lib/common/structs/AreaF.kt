package com.shevelev.comics_viewer.common.structs

/**
 * Area as a portion of total
 */
class AreaF(val leftTop: PointF, val size: SizeRelative) {

    fun toArea(intSize: Size): Area {
        return Area(leftTop.toPoint(intSize), size.toSize(intSize))
    }

}