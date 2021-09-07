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

/**
 * Base event class for all events in the state machine
 */
sealed class Event

/**
 */
object None: Event()

/**
 * @property points touch points
 * @property pressure size of finger's spot
 */
class OneFingerDown(
    val points: List<PointF>,
    val pressure: Float
): Event()

/**
 * @property points touch points
 * @property pressure size of finger's spot
 */
class OneFingerDownInCurlingArea(
    val points: List<PointF>,
    val pressure: Float
): Event()

/**
 * @property hotAreaId Id of a hot area
 */
class OneFingerDownInHotArea(
    val hotAreaId: Int
): Event()

/**
 * @property points touch points
 * @property pressure size of finger's spot
 */
class NextFingerDown(
    val points: List<PointF>,
    val pressure: Float
): Event()

/**
 * @property points touch points
 * @property pressure size of finger's spot
 */
class Move(
    val points: List<PointF>,
    val pressure: Float
): Event()

/**
 * @property points touch points
 * @property fingerIndex index of finger in the very last action (for example - when we up one finger)
 */
class NextFingerUp(
    val points: List<PointF>,
    val fingerIndex: Int
): Event()

/**
 * @property points touch points
 * @property pressure size of finger's spot
 */
class OneFingerUp(
    val points: List<PointF>,
    val pressure: Float
): Event()

/**
 * @property points touch points
 * @property pressure size of finger's spot
 */
class Cancel(
    val points: List<PointF>,
    val pressure: Float
): Event()