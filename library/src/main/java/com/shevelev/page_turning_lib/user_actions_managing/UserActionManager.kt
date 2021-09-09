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
import android.util.Size
import android.view.MotionEvent

/**
 * Transforms touch events to commands for [managedObject]
 * @property managedObject an object to manage
 */
class UserActionManager(
    private val managedObject: IUserActionsManaged
) {
    private var currentState = States.Init

    private val eventsTransformer = EventsTransformer()

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

    fun setScreenSize(size: Size) = eventsTransformer.setScreenSize(size)

    /**
     * Sets "hot" areas
     * @param areas a set of "special" areas, touch in them fires OneFingerDownInHotArea event
     */
    fun setHotAreas(areas: List<Area>) {
        eventsTransformer.setHotAreas(areas)
    }

    private fun processTransition(event: Event, viewStateCode: ViewStateCodes): States =
        when(currentState) {
            States.Init -> {
                when(event) {
                    is OneFingerDownInCurlingArea -> {
                        if (viewStateCode === ViewStateCodes.NotResized) {
                            managedObject.startCurving(event.points[0], event.pressure)
                            States.Curving
                        } else {
                            managedObject.startDragging(event.points[0])
                            States.Dragging
                        }
                    }

                    is OneFingerDown -> {
                        if (viewStateCode === ViewStateCodes.NotResized) {
                            States.Init
                        } else {
                            managedObject.startDragging(event.points[0])
                            States.Dragging
                        }
                    }

                    is OneFingerDownInHotArea -> {
                        lastHotAreaId = event.hotAreaId
                        States.HotAreaHitMode
                    }

                    is NextFingerDown -> {
                        managedObject.startResizing()
                        States.Resizing
                    }

                    else -> currentState
                }
            }

            States.Final -> States.Final

            States.Curving -> {
                when(event) {
                    is NextFingerDown -> {
                        managedObject.cancelCurving(event.points[0], event.pressure)
                        managedObject.startResizing()
                        States.Resizing
                    }

                    is Move -> {
                        managedObject.curving(event.points[0], event.pressure)
                        States.Curving
                    }

                    is OneFingerUp -> completeCurving(event.points, event.pressure)

                    is Cancel -> completeCurving(event.points, event.pressure)

                    else -> currentState
                }
            }

            States.Resizing -> {
                when(event) {
                    is NextFingerDown -> processResizing(event.points)

                    is Move -> processResizing(event.points)

                    is NextFingerUp -> {
                        if (event.points.size > 2) { // There is an old point in 'points' array too
                            managedObject.resizing(event.points)
                            States.Resizing
                        } else {
                            managedObject.completeResizing()
                            if (viewStateCode === ViewStateCodes.NotResized) {
                                States.Curving
                            } else {
                                managedObject.startDragging(event.points[if (event.fingerIndex == 0) 1 else 0])
                                States.Dragging
                            }
                        }
                    }

                    else -> currentState
                }
            }

            States.Dragging -> {
                when(event) {
                    is NextFingerDown -> {
                        managedObject.completeDragging(event.points[0])
                        managedObject.startResizing()
                        States.Resizing
                    }

                    is Move -> {
                        managedObject.dragging(event.points[0])
                        States.Dragging
                    }

                    is OneFingerUp -> processFromDraggingToFinal(event.points)

                    is Cancel -> processFromDraggingToFinal(event.points)

                    else -> currentState
                }
            }

            States.HotAreaHitMode -> {
                when(event) {
                    is OneFingerUp -> {
                        lastHotAreaId?.let { managedObject.onHotAreaHit(it) }
                        lastHotAreaId = null
                        States.Init
                    }

                    else -> currentState
                }
            }
        }

    private fun completeCurving(points: List<PointF>, pressure: Float): States {
        managedObject.completeCurving(points[0], pressure)
        return States.Final
    }

    private fun processResizing(points: List<PointF>): States {
        managedObject.resizing(points)
        return States.Resizing
    }

    private fun processFromDraggingToFinal(points: List<PointF>): States {
        managedObject.completeDragging(points[0])
        return States.Final
    }

    private fun tryReset() {
        if (currentState == States.Final) {
            currentState = States.Init
            eventsTransformer.reset()
        }
    }
}