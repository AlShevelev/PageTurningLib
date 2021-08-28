/*
 * Copyright (c) 2021 Alexander Shevelev
 *
 * Licensed under the MIT License;
 * ---------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.shevelev.page_turning_lib.user_actions_managing

import android.graphics.PointF
import android.view.MotionEvent
import com.shevelev.page_turning_lib.structs.Area
import com.shevelev.page_turning_lib.structs.Point

/**
 * Transform device's touch and motion events to state machine's events
 * @property hotAreas a set of "special" areas, touch in them fires OneFingerDownInHotArea event
 */
class EventsTransformer(private val hotAreas: List<Area>) {
    private var lastMoveEvent: Event? = null

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

        return when (action) {
            MotionEvent.ACTION_DOWN -> getActionDownEvent(points, deviceEvent.pressure, deviceEvent)
            MotionEvent.ACTION_POINTER_DOWN -> getNotMoveEvent(EventCodes.NextFingerDown, points, deviceEvent.pressure, deviceEvent)
            MotionEvent.ACTION_MOVE -> getMoveEvent(points, deviceEvent.pressure, deviceEvent)
            MotionEvent.ACTION_UP -> getNotMoveEvent(EventCodes.OneFingerUp, points, deviceEvent.pressure, deviceEvent)
            MotionEvent.ACTION_POINTER_UP -> getNotMoveEvent(EventCodes.NextFingerUp, points, deviceEvent.pressure, deviceEvent)
            MotionEvent.ACTION_OUTSIDE -> getNotMoveEvent(EventCodes.Cancel, points, deviceEvent.pressure, deviceEvent)
            MotionEvent.ACTION_CANCEL -> getNotMoveEvent(EventCodes.Cancel, points, deviceEvent.pressure, deviceEvent)
            else -> Event(EventCodes.None, points, deviceEvent.pressure, deviceEvent.actionIndex, null)
        }
    }

    private fun getNotMoveEvent(code: EventCodes, points: List<PointF>?, pressure: Float, deviceEvent: MotionEvent): Event {
        lastMoveEvent = null
        return Event(code, points, pressure, deviceEvent.actionIndex, null)
    }

    private fun getActionDownEvent(points: List<PointF>?, pressure: Float, deviceEvent: MotionEvent): Event {
        val lastPoint = Point(points!![0].x.toInt(), points[0].y.toInt())

        val hitAreaId = isHitHotArea(lastPoint)

        return if (hitAreaId != null) {
            Event(EventCodes.OneFingerDownInHotArea, points, pressure, deviceEvent.actionIndex, hitAreaId)
        } else {
            Event(EventCodes.OneFingerDown, points, pressure, deviceEvent.actionIndex, null)
        }
    }

    private fun getMoveEvent(points: List<PointF>?, pressure: Float, deviceEvent: MotionEvent): Event =
        Event(EventCodes.Move, points, pressure, deviceEvent.actionIndex, null)

    /**
     * Is the point inside one of the hot area?
     * @return the hit area id
     */
    private fun isHitHotArea(testedPoint: Point): Int? {
        hotAreas.forEach { area ->
            with(area) {
                val rightBottom = Point(leftTop.left + size.width, leftTop.top + size.height)

                val isHit = testedPoint.left >= leftTop.left &&
                    testedPoint.left <= rightBottom.left &&
                    testedPoint.top >= leftTop.top &&
                    testedPoint.top <= rightBottom.top

                if (isHit) {
                    return area.id
                }
            }
        }
        return null
    }
}