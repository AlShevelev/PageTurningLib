package com.shevelev.comics_viewer.ui.activities.view_comics

class ResizingState(margins: Margins, scaleFactor: Float) {
    var margins // Margins of OGL-view - for smooth image
        : Margins? = null
        private set

    private var currentScaleFactor = 0f // Zoom of OGL-camera - for fast resizing (1: in-screen image; 2: zoom value1 2) = 0f

    var isResized = false
        private set

    var scaleFactor: Float
        get() = currentScaleFactor
        set(scaleFactor) {
            currentScaleFactor = rangeValue(scaleFactor, MIN_SCALE, MAX_SCALE)
            if (currentScaleFactor <= MIN_SCALE + (MAX_SCALE - MIN_SCALE) * 0.05f) currentScaleFactor = MIN_SCALE
            isResized = currentScaleFactor != MIN_SCALE
        }

    fun setMargins(margins: Margins) {
        this.margins = Margins(rangeValue(margins.left, MIN_MARGIN, MAX_MARGIN),
            rangeValue(margins.top, MIN_MARGIN, MAX_MARGIN),
            rangeValue(margins.right, MIN_MARGIN, MAX_MARGIN),
            rangeValue(margins.bottom, MIN_MARGIN, MAX_MARGIN))
    }

    fun recalculateMarginsByScaleFactor() {
        val oneMargin = MIN_MARGIN * ((currentScaleFactor - MIN_SCALE) / (MAX_SCALE - MIN_SCALE))
        margins = Margins(oneMargin, oneMargin, oneMargin, oneMargin)
    }

    private fun rangeValue(value: Float, minValue: Float, maxValue: Float): Float {
        if (value < minValue) return minValue
        return if (value > maxValue) maxValue else value
    }

    fun updateScaleFactor(value: Float) {
        scaleFactor = currentScaleFactor + value
    }

    companion object {
        const val MAX_MARGIN = 0f
        const val MIN_MARGIN = -0.5f
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 2f
    }

    init {
        setMargins(margins)
        this.scaleFactor = scaleFactor
    }
}