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
import android.util.Log
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
    private var currentState = StateCodes.Init

    private val eventsTransformer = EventsTransformer(hotAreas)

    /**
     * Process next motion event
     * @param motionEvent
     * @param viewStateCode
     */
    fun process(motionEvent: MotionEvent, viewStateCode: ViewStateCodes?) {
        tryReset() // Try to reset machine's state if it's final
        val event = eventsTransformer.transform(motionEvent)
        currentState = processTransition(event, viewStateCode!!) //transitionsMatrix[currentState.index][event.code.index]!!.invoke(event, viewStateCode!!)
    }

    private fun processTransition(event: Event, viewStateCode: ViewStateCodes): StateCodes =
        when(currentState) {
            StateCodes.Init -> {
                when(event.code) {
                    EventCodes.OneFingerDown -> fromInitOnOneFingerDown(event.points, event.pressure, viewStateCode)
                    
                    EventCodes.OneFingerDownInHotArea -> doNothing(StateCodes.MenuMode)
                    
                    else -> doNothing(currentState)
                }
            }

            StateCodes.Final -> doNothing(StateCodes.Final)

            StateCodes.Curving -> {
                when(event.code) {
                    EventCodes.NextFingerDown -> cancelCurving(event.points, event.pressure)

                    EventCodes.Move -> processCurving(event.points, event.pressure)

                    EventCodes.OneFingerUp, 
                    EventCodes.Cancel -> completeCurving(event.points, event.pressure)

                    else -> doNothing(currentState)
                }
            }

            StateCodes.Resizing -> {
                when(event.code) {
                    EventCodes.NextFingerDown,
                    EventCodes.Move -> processResizing(event.points)

                    EventCodes.NextFingerUp -> processResizingOneFingerUp(event.points, viewStateCode, event.fingerIndex)

                    else -> doNothing(currentState)
                }
            }

            StateCodes.Dragging -> {
                when(event.code) {
                    EventCodes.NextFingerDown -> startResizing(event.points)

                    EventCodes.Move -> processDragging(event.points)

                    EventCodes.OneFingerUp,
                    EventCodes.Cancel -> processFromDraggingToFinal(event.points)
                    
                    else -> doNothing(currentState)
                }
            }

            StateCodes.MenuMode -> {
                when(event.code) {
                    EventCodes.OneFingerUp -> showMenu()

                    else -> doNothing(currentState)
                }
            }
        }

    private fun fromInitOnOneFingerDown(points: List<PointF>?, pressure: Float, viewStateCode: ViewStateCodes): StateCodes {
        if (viewStateCode === ViewStateCodes.NotResized) {
            managedObject.startCurving(points!![0], pressure)
            return StateCodes.Curving
        }
        managedObject.startDragging(points!![0])
        return StateCodes.Dragging
    }

    private fun processCurving(points: List<PointF>?, pressure: Float): StateCodes {
        managedObject.curving(points!![0], pressure)
        return StateCodes.Curving
    }

    private fun completeCurving(points: List<PointF>?, pressure: Float): StateCodes {
        managedObject.completeCurving(points!![0], pressure)
        return StateCodes.Final
    }

    private fun cancelCurving(points: List<PointF>?, pressure: Float): StateCodes {
        managedObject.cancelCurving(points!![0], pressure)
        Log.d("RESIZING", "Start")
        managedObject.startResizing()
        return StateCodes.Resizing
    }

    private fun startResizing(points: List<PointF>?): StateCodes {
        Log.d("RESIZING", "Start")
        managedObject.completeDragging(points!![0])
        managedObject.startResizing()
        return StateCodes.Resizing
    }

    private fun processResizing(points: List<PointF>?): StateCodes {
        Log.d("RESIZING", "In progress")
        managedObject.resizing(points!!)
        return StateCodes.Resizing
    }

    private fun processResizingOneFingerUp(points: List<PointF>?, viewStateCode: ViewStateCodes, fingerIndex: Int): StateCodes {
        Log.d("RESIZING", "One finger up")
        return if (points!!.size > 2) // There is an old point in 'points' array too
        {
            Log.d("RESIZING", "points.length > 2")
            managedObject.resizing(points)
            StateCodes.Resizing
        } else {
            Log.d("RESIZING", "points.length <= 2")
            managedObject.completeResizing()
            if (viewStateCode === ViewStateCodes.NotResized) StateCodes.Curving else {
                managedObject.startDragging(points[if (fingerIndex == 0) 1 else 0])
                StateCodes.Dragging
            }
        }
    }

    private fun processDragging(points: List<PointF>?): StateCodes {
        managedObject.dragging(points!![0])
        return StateCodes.Dragging
    }

    private fun processFromDraggingToFinal(points: List<PointF>?): StateCodes {
        managedObject.completeDragging(points!![0])
        return StateCodes.Final
    }

    /**
     * Empty transition
     */
    private fun doNothing(state: StateCodes): StateCodes {
        return state
    }

    /**
     * Empty transition
     */
    private fun showMenu(): StateCodes {
        managedObject.showMenu()
        return StateCodes.Init
    }
    private fun tryReset() {
        if (currentState == StateCodes.Final) {
            currentState = StateCodes.Init
            eventsTransformer.reset()
        }
    }
}