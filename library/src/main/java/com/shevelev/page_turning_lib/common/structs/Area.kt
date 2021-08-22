package com.shevelev.comics_viewer.common.structs

/**
 * Created by shevelev on 06.10.2015.
 */
class Area(val leftTop: Point, val size: Size) {

    /**
     * Is point inside an area?
     */
    fun isHit(testedPoint: Point): Boolean {
        val rightBottom = Point(leftTop.left + size.width, leftTop.top + size.height)
        return testedPoint.left >= leftTop.left && testedPoint.left <= rightBottom.left && testedPoint.top >= leftTop.top && testedPoint.top <= rightBottom.top
    }

}