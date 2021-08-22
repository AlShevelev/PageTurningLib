package com.shevelev.comics_viewer.common.structs

/**
 * Size in pixels
 */
class Size(val width: Int, val height: Int) {

    val isVertical: Boolean
        get() = height > width

    /**
     * Scaling with saving proportions
     */
    fun scale(scaleFactor: Float): Size {
        val tmpWidth = scaleFactor * width
        val tmpHeight = scaleFactor * height
        return Size(tmpWidth.toInt(), tmpHeight.toInt())
    }

    /**
     * inscribe one area to another
     */
    fun inscribe(sizeToInscribe: Size): Size {
        val currentWidth = width.toFloat()
        val currentHeight = height.toFloat()
        val inscribeWidth = sizeToInscribe.width.toFloat()
        val inscribeHeight = sizeToInscribe.height.toFloat()
        return if (isVertical) {
            var scaleFactor = currentHeight / inscribeHeight
            val tmpWidth = inscribeWidth * scaleFactor // try to inscribe by height
            if (tmpWidth <= currentWidth) Size(tmpWidth.toInt(), currentHeight.toInt()) else {
                scaleFactor = currentWidth / inscribeWidth // else try to inscribe by height
                Size(currentWidth.toInt(), (inscribeHeight * scaleFactor).toInt())
            }
        } else {
            var scaleFactor = currentWidth / inscribeWidth
            val tmpHeight = inscribeHeight * scaleFactor // try to inscribe by height
            if (tmpHeight <= currentHeight) Size(currentWidth.toInt(), tmpHeight.toInt()) else {
                scaleFactor = currentHeight / inscribeHeight // else try to inscribe by height
                Size((inscribeWidth * scaleFactor).toInt(), currentHeight.toInt())
            }
        }
    }

}