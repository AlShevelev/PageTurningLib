package com.shevelev.comics_viewer.ui.activities.view_comics.user_actions_managing

import android.graphics.PointF

/**
 * One event of state machine
 */
class Event(
    /** Code from EventsCodes  */
    val code: Int,
    /** Fingers points - may be null  */
    val points: List<PointF>?,
    /** Size of finger's spot  */
    val pressure: Float,
    /** Index of finger in last action (for example - when we up one finger)  */
    var fingerIndex: Int)