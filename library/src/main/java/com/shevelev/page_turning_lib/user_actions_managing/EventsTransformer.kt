package com.shevelev.comics_viewer.ui.activities.view_comics.user_actions_managing

import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import com.shevelev.comics_viewer.ui.activities.view_comics.helpers.PointsHelper.getDistance
import com.shevelev.comics_viewer.common.structs.Area
import com.shevelev.comics_viewer.common.structs.Point
import com.shevelev.comics_viewer.common.structs.Size

/**
 * Transform device's events to state machine's events
 */
class EventsTransformer(screenSize: Size) {
    private var lastMoveEvent: Event? = null

    // // Menu hit area
    private val menuArea : Area = Area(
        Point((screenSize.width * 0.9f).toInt(), 0),
        Size((screenSize.width * 0.1f).toInt(), (screenSize.height * 0.1f).toInt()))

    /**
     * Reset internal state
     */
    fun reset() {
        lastMoveEvent = null
    }

    fun transform(deviceEvent: MotionEvent): Event {
        val action = deviceEvent.actionMasked
        val pointersTotal = deviceEvent.pointerCount
        var points: MutableList<PointF>? = null

        if (pointersTotal > 0) {
            points = mutableListOf()
            for (i in 0 until pointersTotal) {
                points.add(PointF(deviceEvent.getX(i), deviceEvent.getY(i)))
            }
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> return getActionDownEvent(points, deviceEvent.pressure, deviceEvent)
            MotionEvent.ACTION_POINTER_DOWN -> return getNotMoveEvent(EventsCodes.NextFingerDown, points, deviceEvent.pressure, deviceEvent)
            MotionEvent.ACTION_MOVE -> return getMoveEvent(points, deviceEvent.pressure, deviceEvent)
            MotionEvent.ACTION_UP -> return getNotMoveEvent(EventsCodes.OneFingerUp, points, deviceEvent.pressure, deviceEvent)
            MotionEvent.ACTION_POINTER_UP -> return getNotMoveEvent(EventsCodes.NextFingerUp, points, deviceEvent.pressure, deviceEvent)
            MotionEvent.ACTION_OUTSIDE -> return getNotMoveEvent(EventsCodes.Cancel, points, deviceEvent.pressure, deviceEvent)
            MotionEvent.ACTION_CANCEL -> return getNotMoveEvent(EventsCodes.Cancel, points, deviceEvent.pressure, deviceEvent)
        }
        return Event(EventsCodes.None, points, deviceEvent.pressure, deviceEvent.actionIndex)
    }

    private fun getNotMoveEvent(code: Int, points: List<PointF>?, pressure: Float, deviceEvent: MotionEvent): Event {
        lastMoveEvent = null
        return Event(code, points, pressure, deviceEvent.actionIndex)
    }

    private fun getActionDownEvent(points: List<PointF>?, pressure: Float, deviceEvent: MotionEvent): Event {
        val lastPoint = Point(points!![0]!!.x.toInt(), points[0]!!.y.toInt())
        val code = if (menuArea.isHit(lastPoint)) EventsCodes.OneFingerDownInMenuArea else EventsCodes.OneFingerDown
        return Event(code, points, pressure, deviceEvent.actionIndex)
    }

    private fun getMoveEvent(points: List<PointF>?, pressure: Float, deviceEvent: MotionEvent): Event {
        /*
        if(areEquals(currentMoveEvent, lastMoveEvent))
            return new Event(EventsCodes.None, points, pressure);         // Cut equals Move-events to avoid noising
        else
        {
            lastMoveEvent = currentMoveEvent;
            return currentMoveEvent;
        }
        */
        return Event(EventsCodes.Move, points, pressure, deviceEvent.actionIndex)
    }

    private fun areEquals(e1: Event?, e2: Event?): Boolean {
        if (e1 == null || e2 == null) return false
        if (e1.code != e2.code) return false
        val e1Points = e1.points
        val e2Points = e2.points
        if (e1Points == null && e2Points == null) return true
        if (e1Points != null && e2Points != null) {
            if (e1Points.size != e2Points.size) return false
            val nearThreshold = 2.5f
            if (e1Points.size == 1) // One-point event - calculate distance between events points
            {
                if (getDistance(e1Points[0], e2Points[0]) < nearThreshold) return true
            } else  // Multi-points event - calculate distance between points in every event
            {
                val e1PointsDistance = getDistance(e1Points)
                val e2PointsDistance = getDistance(e2Points)
                if (Math.abs(e1PointsDistance - e2PointsDistance) < nearThreshold) // Merge events with near distances
                    return true
            }
        }
        return false
    }

    fun logTouchEvent(me: MotionEvent) {
        val action = me.actionMasked
        val pointersTotal = me.pointerCount
        val x = FloatArray(pointersTotal)
        val y = FloatArray(pointersTotal)
        val id = FloatArray(pointersTotal)
        for (i in 0 until pointersTotal) {
            x[i] = me.getX(i)
            y[i] = me.getY(i)
            id[i] = me.getPointerId(i).toFloat()
        }
        var logData = "Action: "
        when (action) {
            MotionEvent.ACTION_DOWN -> logData += "Down"
            MotionEvent.ACTION_MOVE -> logData += "Move"
            MotionEvent.ACTION_POINTER_DOWN -> logData += "Pointer Down"
            MotionEvent.ACTION_UP -> logData += "Up"
            MotionEvent.ACTION_POINTER_UP -> logData += "Pointer Up"
            MotionEvent.ACTION_OUTSIDE -> logData += "Outside"
            MotionEvent.ACTION_CANCEL -> logData += "Cancel"
        }
        logData += "; pointersTotal: $pointersTotal"
        for (i in 0 until pointersTotal) {
            logData += "; [pointerIndex: $i"
            logData += "; id: " + id[i]
            logData += "; value1: " + x[i]
            logData += "; value2: " + y[i] + "]"
        }
        Log.d("TOUCH_EV", logData)
    }

    fun logTouchEvent(e: Event) {
        var logData = "Action: "
        when (e.code) {
            EventsCodes.NextFingerUp -> logData += "NextFingerUp"
            EventsCodes.OneFingerDown -> logData += "OneFingerDown"
            EventsCodes.None -> logData += "None"
            EventsCodes.Move -> logData += "Move"
            EventsCodes.OneFingerUp -> logData += "OneFingerUp"
            EventsCodes.Cancel -> logData += "Cancel"
            EventsCodes.NextFingerDown -> logData += "NextFingerDown"
        }
        val points = e.points
        logData += if ("; pointersTotal: $points" != null) points!!.size else 0
        for (i in points!!.indices) {
            logData += "; [value1: " + points[i].x
            logData += "; value2: " + points[i].y + "]"
        }
        Log.d("PROCESSED_EVENT", logData)
    }

}