package com.shevelev.comics_viewer.common.helpers

import android.app.Activity
import android.graphics.Point
import android.util.DisplayMetrics
import com.shevelev.comics_viewer.common.structs.Size

/**
 * All about screen and pixels
 */
object ScreenHelper {
    /**
     * Get size of device screen in pixels
     * @param context
     * @return
     */
    fun getScreenSize(context: Activity): Size {
        val display = context.windowManager.defaultDisplay
        val size = Point() // Get size of screen
        display.getSize(size)
        return Size(size.x, size.y)
    }

    /**
     * Get size of activity client area
     * @param context
     * @return
     */
    fun getClientSize(context: Activity): Size {
        val screenSize = getScreenSize(context)
        val ab = context.actionBar ?: return screenSize
        return Size(screenSize.width, screenSize.height - ab.height)
    }

    /**
     * Convert DP units to PX
     * @param dp
     * @param context
     * @return
     */
    fun dpToPx(dp: Int, context: Activity): Int {
        val displayMetrics = context.resources.displayMetrics
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    /**
     * Convert PX units to DP
     * @param px
     * @param context
     * @return
     */
    fun pxToDp(px: Int, context: Activity): Int {
        val displayMetrics = context.resources.displayMetrics
        return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }
}