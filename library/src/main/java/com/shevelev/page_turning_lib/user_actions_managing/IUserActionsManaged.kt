package com.shevelev.comics_viewer.ui.activities.view_comics.user_actions_managing

import android.graphics.PointF

/**
 * This interface is for class managed by user touches
 */
interface IUserActionsManaged {
    fun startCurving(point: PointF, pressure: Float)
    fun curving(point: PointF, pressure: Float)
    fun completeCurving(point: PointF, pressure: Float)
    fun cancelCurving(point: PointF, pressure: Float)
    fun startResizing()
    fun resizing(points: List<PointF>)
    fun completeResizing()
    fun startDragging(point: PointF)
    fun dragging(point: PointF)
    fun completeDragging(point: PointF)
    fun showMenu()
}