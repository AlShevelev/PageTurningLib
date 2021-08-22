package com.shevelev.comics_viewer.ui.activities.view_comics.bitmap_repository

import java.util.*

class QueueWithDisplacement(maxLen: Int) {
    private val queue: Queue<Int>
    private val maxLen: Int
    fun isExists(value: Int): Boolean {
        for (valueInList in queue) if (valueInList == value) return true
        return false
    }

    /**
     * Push the value to queue (if not exists yet)
     * @param value
     * @return - displacement value or null
     */
    fun push(value: Int): Int? {
        return if (isExists(value)) null else {
            if (queue.size < maxLen) {
                queue.add(value)
                null
            } else {
                val result = queue.remove()
                queue.add(value)
                result
            }
        }
    }

    init {
        queue = LinkedList()
        this.maxLen = maxLen
    }
}