package com.shevelev.comics_viewer.ui.activities.view_comics.user_actions_managing

import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import com.shevelev.comics_viewer.common.structs.Size

class UserActionManager(private val managedObject: IUserActionsManaged, screenSize: Size?) {
    private var currentState: Int
    private val eventsTransformer: EventsTransformer
    var transitionsMatrix : Array<Array<TransitionFunc?>>

    private fun fromInitOnOneFingerDown(points: List<PointF>?, pressure: Float, viewStateCode: ViewStateCodes): Int {
        if (viewStateCode === ViewStateCodes.NotResized) {
            managedObject.startCurving(points!![0], pressure)
            return StatesCodes.Curving
        }
        managedObject.startDragging(points!![0])
        return StatesCodes.Dragging
    }

    private fun processCurving(points: List<PointF>?, pressure: Float): Int {
        managedObject.curving(points!![0], pressure)
        return StatesCodes.Curving
    }

    private fun completeCurving(points: List<PointF>?, pressure: Float): Int {
        managedObject.completeCurving(points!![0], pressure)
        return StatesCodes.Final
    }

    private fun cancelCurving(points: List<PointF>?, pressure: Float): Int {
        managedObject.cancelCurving(points!![0], pressure)
        Log.d("RESIZING", "Start")
        managedObject.startResizing()
        return StatesCodes.Resizing
    }

    private fun startResizing(points: List<PointF>?): Int {
        Log.d("RESIZING", "Start")
        managedObject.completeDragging(points!![0])
        managedObject.startResizing()
        return StatesCodes.Resizing
    }

    private fun processResizing(points: List<PointF>?): Int {
        Log.d("RESIZING", "In progress")
        managedObject.resizing(points!!)
        return StatesCodes.Resizing
    }

    private fun processResizingOneFingerUp(points: List<PointF>?, viewStateCode: ViewStateCodes, fingerIndex: Int): Int {
        Log.d("RESIZING", "One finger up")
        return if (points!!.size > 2) // There is an old point in 'points' array too
        {
            Log.d("RESIZING", "points.length > 2")
            managedObject.resizing(points)
            StatesCodes.Resizing
        } else {
            Log.d("RESIZING", "points.length <= 2")
            managedObject.completeResizing()
            if (viewStateCode === ViewStateCodes.NotResized) StatesCodes.Curving else {
                managedObject.startDragging(points[if (fingerIndex == 0) 1 else 0])
                StatesCodes.Dragging
            }
        }
    }

    private fun processDragging(points: List<PointF>?): Int {
        managedObject.dragging(points!![0])
        return StatesCodes.Dragging
    }

    private fun processFromDraggingToFinal(points: List<PointF>?): Int {
        managedObject.completeDragging(points!![0])
        return StatesCodes.Final
    }

    /**
     * Empty transition
     */
    private fun doNothing(state: Int): Int {
        return state
    }

    /**
     * Empty transition
     */
    private fun showMenu(): Int {
        managedObject.showMenu()
        return StatesCodes.Init
    }

    /**
     * Process next motion event
     * @param motionEvent
     * @param viewStateCode
     */
    fun Process(motionEvent: MotionEvent?, viewStateCode: ViewStateCodes?) {
        tryReset() // Try to reset machine's state if it's final
        eventsTransformer.logTouchEvent(motionEvent!!) // Debug only
        val event = eventsTransformer.transform(motionEvent)
        eventsTransformer.logTouchEvent(event)
        currentState = transitionsMatrix[currentState][event.code]!!.invoke(event, viewStateCode!!)
    }

    private fun tryReset() {
        if (currentState == StatesCodes.Final) {
            currentState = StatesCodes.Init
            eventsTransformer.reset()
        }
    }

    init {
        currentState = StatesCodes.Init
        eventsTransformer = EventsTransformer(screenSize!!)
        transitionsMatrix = Array(6) { arrayOfNulls<TransitionFunc>(8) }
        transitionsMatrix[StatesCodes.Init][EventsCodes.None] =  { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Init) }
        transitionsMatrix[StatesCodes.Init][EventsCodes.OneFingerDown] = { event: Event, viewStateCode: ViewStateCodes -> fromInitOnOneFingerDown(event.points, event.pressure, viewStateCode) }
        transitionsMatrix[StatesCodes.Init][EventsCodes.NextFingerDown] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Init) }
        transitionsMatrix[StatesCodes.Init][EventsCodes.Move] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Init) }
        transitionsMatrix[StatesCodes.Init][EventsCodes.NextFingerUp] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Init) }
        transitionsMatrix[StatesCodes.Init][EventsCodes.OneFingerUp] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Init) }
        transitionsMatrix[StatesCodes.Init][EventsCodes.Cancel] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Init) }
        transitionsMatrix[StatesCodes.Init][EventsCodes.OneFingerDownInMenuArea] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.MenuMode) }
        transitionsMatrix[StatesCodes.Final][EventsCodes.None] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Final) }
        transitionsMatrix[StatesCodes.Final][EventsCodes.OneFingerDown] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Final) }
        transitionsMatrix[StatesCodes.Final][EventsCodes.NextFingerDown] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Final) }
        transitionsMatrix[StatesCodes.Final][EventsCodes.Move] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Final) }
        transitionsMatrix[StatesCodes.Final][EventsCodes.NextFingerUp] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Final) }
        transitionsMatrix[StatesCodes.Final][EventsCodes.OneFingerUp] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Final) }
        transitionsMatrix[StatesCodes.Final][EventsCodes.Cancel] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Final) }
        transitionsMatrix[StatesCodes.Final][EventsCodes.OneFingerDownInMenuArea] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Final) }
        transitionsMatrix[StatesCodes.Curving][EventsCodes.None] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Curving) }
        transitionsMatrix[StatesCodes.Curving][EventsCodes.OneFingerDown] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Curving) }
        transitionsMatrix[StatesCodes.Curving][EventsCodes.NextFingerDown] = { event: Event, viewStateCode: ViewStateCodes? -> cancelCurving(event.points, event.pressure) }
        transitionsMatrix[StatesCodes.Curving][EventsCodes.Move] = { event: Event, viewStateCode: ViewStateCodes? -> processCurving(event.points, event.pressure) }
        transitionsMatrix[StatesCodes.Curving][EventsCodes.NextFingerUp] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Curving) }
        transitionsMatrix[StatesCodes.Curving][EventsCodes.OneFingerUp] = { event: Event, viewStateCode: ViewStateCodes? -> completeCurving(event.points, event.pressure) }
        transitionsMatrix[StatesCodes.Curving][EventsCodes.Cancel] = { event: Event, viewStateCode: ViewStateCodes? -> completeCurving(event.points, event.pressure) }
        transitionsMatrix[StatesCodes.Curving][EventsCodes.OneFingerDownInMenuArea] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Curving) }
        transitionsMatrix[StatesCodes.Resizing][EventsCodes.None] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Resizing) }
        transitionsMatrix[StatesCodes.Resizing][EventsCodes.OneFingerDown] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Resizing) }
        transitionsMatrix[StatesCodes.Resizing][EventsCodes.NextFingerDown] = { event: Event, viewStateCode: ViewStateCodes? -> processResizing(event.points) }
        transitionsMatrix[StatesCodes.Resizing][EventsCodes.Move] = { event: Event, viewStateCode: ViewStateCodes? -> processResizing(event.points) }
        transitionsMatrix[StatesCodes.Resizing][EventsCodes.NextFingerUp] = { event: Event, viewStateCode: ViewStateCodes -> processResizingOneFingerUp(event.points, viewStateCode, event.fingerIndex) }
        transitionsMatrix[StatesCodes.Resizing][EventsCodes.OneFingerUp] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Resizing) }
        transitionsMatrix[StatesCodes.Resizing][EventsCodes.Cancel] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Resizing) }
        transitionsMatrix[StatesCodes.Resizing][EventsCodes.OneFingerDownInMenuArea] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Resizing) }
        transitionsMatrix[StatesCodes.Dragging][EventsCodes.None] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Dragging) }
        transitionsMatrix[StatesCodes.Dragging][EventsCodes.OneFingerDown] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Dragging) }
        transitionsMatrix[StatesCodes.Dragging][EventsCodes.NextFingerDown] = { event: Event, viewStateCode: ViewStateCodes? -> startResizing(event.points) }
        transitionsMatrix[StatesCodes.Dragging][EventsCodes.Move] = { event: Event, viewStateCode: ViewStateCodes? -> processDragging(event.points) }
        transitionsMatrix[StatesCodes.Dragging][EventsCodes.NextFingerUp] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Dragging) }
        transitionsMatrix[StatesCodes.Dragging][EventsCodes.OneFingerUp] = { event: Event, viewStateCode: ViewStateCodes? -> processFromDraggingToFinal(event.points) }
        transitionsMatrix[StatesCodes.Dragging][EventsCodes.Cancel] = { event: Event, viewStateCode: ViewStateCodes? -> processFromDraggingToFinal(event.points) }
        transitionsMatrix[StatesCodes.Dragging][EventsCodes.OneFingerDownInMenuArea] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.Dragging) }
        transitionsMatrix[StatesCodes.MenuMode][EventsCodes.None] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.MenuMode) }
        transitionsMatrix[StatesCodes.MenuMode][EventsCodes.OneFingerDown] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.MenuMode) }
        transitionsMatrix[StatesCodes.MenuMode][EventsCodes.NextFingerDown] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.MenuMode) }
        transitionsMatrix[StatesCodes.MenuMode][EventsCodes.Move] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.MenuMode) }
        transitionsMatrix[StatesCodes.MenuMode][EventsCodes.NextFingerUp] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.MenuMode) }
        transitionsMatrix[StatesCodes.MenuMode][EventsCodes.OneFingerUp] = { event: Event?, viewStateCode: ViewStateCodes? -> showMenu() }
        transitionsMatrix[StatesCodes.MenuMode][EventsCodes.Cancel] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.MenuMode) }
        transitionsMatrix[StatesCodes.MenuMode][EventsCodes.OneFingerDownInMenuArea] = { event: Event?, viewStateCode: ViewStateCodes? -> doNothing(StatesCodes.MenuMode) }
    }
}