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

/**
 * Transforms touch events to commands for [managedObject]
 * @property managedObject an object to manage
 * @property hotAreas a set of "special" areas, touch in them fires OneFingerDownInHotArea event
 */
class UserActionManager(
    private val managedObject: IUserActionsManaged,
    private val hotAreas: List<Area>
) {
    private var currentState = States.Init

    private val eventsTransformer = EventsTransformer(hotAreas)

    private var lastHotAreaId : Int? = null

    /**
     * Process next motion event
     * @param motionEvent
     * @param viewStateCode
     */
    fun process(motionEvent: MotionEvent, viewStateCode: ViewStateCodes) {
        tryReset() // Try to reset machine's state if it's final
        val event = eventsTransformer.transform(motionEvent)
        currentState = processTransition(event, viewStateCode)
    }

    private fun processTransition(event: Event, viewStateCode: ViewStateCodes): States =
        when(currentState) {
            States.Init -> {
                when(event) {
                    is OneFingerDown -> fromInitOnOneFingerDown(event.points, event.pressure, viewStateCode)
                    is OneFingerDownInHotArea -> {
                        lastHotAreaId = event.hotAreaId
                        doNothing(States.HotAreaHitMode)
                    }
                    else -> doNothing(currentState)
                }
            }

            States.Final -> doNothing(States.Final)

            States.Curving -> {
                when(event) {
                    is NextFingerDown -> cancelCurving(event.points, event.pressure)
                    is Move -> processCurving(event.points, event.pressure)
                    is OneFingerUp -> completeCurving(event.points, event.pressure)
                    is Cancel -> completeCurving(event.points, event.pressure)
                    else -> doNothing(currentState)
                }
            }

            States.Resizing -> {
                when(event) {
                    is NextFingerDown -> processResizing(event.points)
                    is Move -> processResizing(event.points)
                    is NextFingerUp -> processResizingOneFingerUp(event.points, viewStateCode, event.fingerIndex)
                    else -> doNothing(currentState)
                }
            }

            States.Dragging -> {
                when(event) {
                    is NextFingerDown -> startResizing(event.points)
                    is Move -> processDragging(event.points)
                    is OneFingerUp -> processFromDraggingToFinal(event.points)
                    is Cancel -> processFromDraggingToFinal(event.points)
                    else -> doNothing(currentState)
                }
            }

            States.HotAreaHitMode -> {
                when(event) {
                    is OneFingerUp -> processHotAreaHit()
                    else -> doNothing(currentState)
                }
            }
        }

    private fun fromInitOnOneFingerDown(points: List<PointF>, pressure: Float, viewStateCode: ViewStateCodes): States {
        if (viewStateCode === ViewStateCodes.NotResized) {
            managedObject.startCurving(points[0], pressure)
            return States.Curving
        }
        managedObject.startDragging(points[0])
        return States.Dragging
    }

    private fun processCurving(points: List<PointF>, pressure: Float): States {
        managedObject.curving(points[0], pressure)
        return States.Curving
    }

    private fun completeCurving(points: List<PointF>, pressure: Float): States {
        managedObject.completeCurving(points[0], pressure)
        return States.Final
    }

    private fun cancelCurving(points: List<PointF>, pressure: Float): States {
        managedObject.cancelCurving(points[0], pressure)
        managedObject.startResizing()
        return States.Resizing
    }

    private fun startResizing(points: List<PointF>): States {
        managedObject.completeDragging(points[0])
        managedObject.startResizing()
        return States.Resizing
    }

    private fun processResizing(points: List<PointF>): States {
        managedObject.resizing(points)
        return States.Resizing
    }

    private fun processResizingOneFingerUp(points: List<PointF>, viewStateCode: ViewStateCodes, fingerIndex: Int): States {
        return if (points.size > 2) { // There is an old point in 'points' array too
            managedObject.resizing(points)
            States.Resizing
        } else {
            managedObject.completeResizing()
            if (viewStateCode === ViewStateCodes.NotResized) States.Curving else {
                managedObject.startDragging(points[if (fingerIndex == 0) 1 else 0])
                States.Dragging
            }
        }
    }

    private fun processDragging(points: List<PointF>): States {
        managedObject.dragging(points[0])
        return States.Dragging
    }

    private fun processFromDraggingToFinal(points: List<PointF>): States {
        managedObject.completeDragging(points[0])
        return States.Final
    }

    private fun doNothing(state: States): States {
        return state
    }

    private fun processHotAreaHit(): States {
        lastHotAreaId?.let { managedObject.onHotAreaHit(it) }
        lastHotAreaId = null

        return States.Init
    }
    private fun tryReset() {
        if (currentState == States.Final) {
            currentState = States.Init
            eventsTransformer.reset()
        }
    }
}