package com.shevelev.comics_viewer.ui.activities.view_comics.helpers

import android.graphics.PointF
import kotlin.math.sqrt

object PointsHelper {
    fun getDistance(p1: PointF, p2: PointF): Float {
        return sqrt(Math.pow(p1.x - p2.x.toDouble(), 2.0) + Math.pow(p1.y - p2.y.toDouble(), 2.0)).toFloat()
    }

    fun getDistance(points: List<PointF>?): Float {
        require(!(points == null || points.isEmpty())) { "'points' can't be empty" }
        if (points.size == 1) return 0f
        if (points.size == 2) return getDistance(points[0], points[1])
        var sum = 0f
        val totalPoints = points.size
        for (i in 0 until totalPoints) for (j in 0 until totalPoints) if (i != j) sum += getDistance(points[i], points[j]) // Average distance
        return sum / (totalPoints * totalPoints - totalPoints)
    }
}