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

package com.shevelev.page_turning_lib.page_curling

import android.content.Context
import android.graphics.PointF
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import com.shevelev.page_turning_lib.page_curling.PointsHelper.getDistance
import com.shevelev.page_turning_lib.page_curling.textures_manager.PageLoadingEventsHandler
import com.shevelev.page_turning_lib.page_curling.textures_manager.repository.BitmapProvider
import com.shevelev.page_turning_lib.page_curling.textures_manager.PageTexturesManager
import com.shevelev.page_turning_lib.user_actions_managing.Area
import com.shevelev.page_turning_lib.user_actions_managing.IUserActionsManaged
import com.shevelev.page_turning_lib.user_actions_managing.UserActionManager
import com.shevelev.page_turning_lib.user_actions_managing.ViewStateCodes
import java.lang.UnsupportedOperationException
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * OpenGL ES View for curling.
 */
class CurlView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs),
    OnTouchListener,
    CurlRendererObserver,
    IUserActionsManaged,
    CurlViewInvalidator {

    // Curl state. We are flipping none, left or right page.
    private var curlState = CurlState.None

    private var animationTargetEvent = CurlTarget.None

    // Can we curl last page
    private var canCurlLastPage = false

    private var animate = false
    private val animationDurationTime: Long = 300
    private val animationSource = PointF()
    private var animationStartTime: Long = 0
    private val animationTarget = PointF()

    private val curlDir = PointF()
    private val curlPos = PointF()

    private var currentPageIndex = 0 // Current bitmap index. This is always showed as front of right page. = 0

    // Start position for dragging.
    private val dragStartPos = PointF()

    private val enableTouchPressure = false
    // Bitmap size. These are updated from renderer once it's initialized.
    private var pageBitmapHeight = -1
    private var pageBitmapWidth = -1

    // Page meshes. Left and right meshes are 'static' while curl is used to show page flipping.
    private var pageCurl = CurlMesh(MAX_CURL_SPLITS)
    private var pageLeft = CurlMesh(MAX_CURL_SPLITS)
    private var pageRight = CurlMesh(MAX_CURL_SPLITS)

    private var texturesManager: PageTexturesManager? = null

    private val pointerPos = PointerPosition(PointF(), 0f)

    private var renderer = CurlRenderer(this)
    private val renderLeftPage = true

    private var userActionManager = UserActionManager(this)
    private var resizingState: ResizingState? = null

    // Distance between points while resizing
    private var resizingPointsDistance : Float? = null

    private var viewStateCodes = ViewStateCodes.NotResized

    // Size of screen diagonal in pixels = 0f
    private var screenDiagonal = 0f
    // Size of screen diagonal in pixels
    private var firstDraggingPoint : PointF? = null

    private var draggingState = DraggingState(ResizingState.MIN_MARGIN, ResizingState.MAX_MARGIN)

    private var externalEventsHandler: CurlViewEventsHandler? = null

    init {
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY

        setOnTouchListener(this)

        pageLeft.setFlipTexture(true)
        pageRight.setFlipTexture(false)
    }

    override fun onDrawFrame() {
        if (!animate) {
            return
        }

        val currentTime = System.currentTimeMillis()

        // If animation is done.
        if (currentTime >= animationStartTime + animationDurationTime) {
            if (animationTargetEvent === CurlTarget.ToRight) { // Switch curled page to right.
                val right = pageCurl
                val curl = pageRight
                right.setRect(renderer.getPageRect(CurlState.Right)!!)
                right.setFlipTexture(false)
                right.reset()
                renderer.removeCurlMesh(curl)
                pageCurl = curl
                pageRight = right
                // If we were curling left page update current index.
                if (curlState === CurlState.Left) --currentPageIndex
            } else if (animationTargetEvent === CurlTarget.ToLeft) { // Switch curled page to left.
                val left = pageCurl
                val curl = pageLeft
                left.setRect(renderer.getPageRect(CurlState.Left)!!)
                left.setFlipTexture(true)
                left.reset()
                renderer.removeCurlMesh(curl)
                if (!renderLeftPage) {
                    renderer.removeCurlMesh(left)
                }
                pageCurl = curl
                pageLeft = left
                // If we were curling right page update current index.
                if (curlState === CurlState.Right) ++currentPageIndex
            }

            curlState = CurlState.None
            animate = false

            externalEventsHandler?.onPageChanged(currentPageIndex)

            renderNow()
        } else {
            // Processing an animation
            pointerPos.pos.set(animationSource)

            var time = 1f - (currentTime - animationStartTime).toFloat() / animationDurationTime
            time = 1f - time * time * time * (3 - 2 * time)

            pointerPos.pos.x += (animationTarget.x - animationSource.x) * time
            pointerPos.pos.y += (animationTarget.y - animationSource.y) * time
            updateCurlPos(pointerPos)
        }
    }

    override fun onPageSizeChanged(width: Int, height: Int) {
        pageBitmapWidth = width
        pageBitmapHeight = height
        initPages(false)
    }

    override fun onSurfaceCreated() {
        // In case surface is recreated, let page meshes drop allocated texture
        // ids and ask for new ones. There's no need to set textures here as
        // onPageSizeChanged should be called later on.
        pageLeft.resetTexture()
        pageRight.resetTexture()
        pageCurl.resetTexture()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)

        reset()
        screenDiagonal = sqrt(w.toDouble().pow(2.0) + h.toDouble().pow(2.0)).toFloat()
        userActionManager.setScreenSize(Size(w, h))
    }

    override fun startCurving(point: PointF, pressure: Float) {
        val rightRect = renderer.getPageRect(CurlState.Right)
        memorizePoint(point.x, point.y, pressure)

        // Once we receive pointer down event its position is mapped to
        // right or left edge of page and that'll be the position from where
        // user is holding the paper to make curl happen.
        dragStartPos.set(pointerPos.pos)

        // First we make sure it's not over or below page. Pages are
        // supposed to be same height so it really doesn't matter do we use
        // left or right one.
        if (dragStartPos.y > rightRect!!.top) {
            dragStartPos.y = rightRect.top
        } else if (dragStartPos.y < rightRect.bottom) {
            dragStartPos.y = rightRect.bottom
        }

        // Then we have to make decisions for the user whether curl is going
        // to happen from left or right, and on which page.
        val halfX = (rightRect.right + rightRect.left) / 2
        if (dragStartPos.x < halfX && currentPageIndex > 0) {
            dragStartPos.x = rightRect.left
            startCurl(CurlState.Left)
        } else if (dragStartPos.x >= halfX && currentPageIndex < texturesManager!!.pageCount) {
            dragStartPos.x = rightRect.right

            if (!canCurlLastPage && currentPageIndex >= texturesManager!!.pageCount - 1) {
                return
            }
            startCurl(CurlState.Right)
        }

        // If we have are in curl state, let this case clause flow through
        // to next one. We have pointer position and drag position defined
        // and this will setDiskItems first render request given these points.
        if (curlState === CurlState.None) {
            return
        }
    }

    override fun curving(point: PointF, pressure: Float) {
        memorizePoint(point.x, point.y, pressure)
        updateCurlPos(pointerPos)
    }

    override fun completeCurving(point: PointF, pressure: Float) {
        val rightRect = renderer.getPageRect(CurlState.Right)
        val leftRect = renderer.getPageRect(CurlState.Left)
        memorizePoint(point.x, point.y, pressure)

        // Animation source is the point from where animation starts.
        if (curlState === CurlState.Left || curlState === CurlState.Right) {
            // Also it's handled in a way we actually simulate touch events
            // meaning the output is exactly the same as if user drags the
            // page to other side. While not producing the best looking
            // result (which is easier done by altering curl position and/or
            // direction directly), this is done in a hope it made code a
            // bit more readable and easier to maintain.
            animationSource.set(pointerPos.pos)
            animationStartTime = System.currentTimeMillis()

            // Given the explanation, here we decide whether to simulate drag to left or right end.
            if (pointerPos.pos.x > (rightRect!!.left + rightRect.right) / 2) {
                // On right side target is always right page's right border.
                animationTarget.set(dragStartPos)
                animationTarget.x = renderer.getPageRect(CurlState.Right)!!.right
                animationTargetEvent = CurlTarget.ToRight
            } else {
                // On left side target depends on visible pages.
                animationTarget.set(dragStartPos)

                if (curlState === CurlState.Right) {
                    animationTarget.x = leftRect!!.left
                } else {
                    animationTarget.x = rightRect.left
                }
                animationTargetEvent = CurlTarget.ToLeft
            }
            animate = true
            renderer.setDragging(draggingState.reset())
            renderNow()
        }
    }

    override fun cancelCurving(point: PointF, pressure: Float) {
        if (curlState === CurlState.None) {
            return
        }

        memorizePoint(point.x, point.y, pressure)
        animationTarget.set(dragStartPos)

        when(curlState) {
            CurlState.Left -> {
                animationTarget.x = renderer.getPageRect(CurlState.Left)!!.left
                animationTargetEvent = CurlTarget.ToLeft
            }

            CurlState.Right -> {
                animationTarget.x = renderer.getPageRect(CurlState.Right)!!.right
                animationTargetEvent = CurlTarget.ToRight
            }

            else -> throw UnsupportedOperationException("This value is not supported: $curlState")
        }

        animate = true
        renderer.setDragging(draggingState.reset())
        renderNow()
    }

    override fun startResizing() {
        resizingPointsDistance = null
        viewStateCodes = if (resizingState!!.isResized) ViewStateCodes.Resized else ViewStateCodes.NotResized
        renderer.setMargins(Margins(0f, 0f, 0f, 0f))
        renderer.setScale(resizingState!!.scaleFactor)

        // Place to center
        if (viewStateCodes === ViewStateCodes.Resized) {
            renderer.setDragging(draggingState.reset())
        }
    }

    override fun resizing(points: List<PointF>) {
        val newResizingPointsDistance = getDistance(points)

        if (resizingPointsDistance == null && newResizingPointsDistance > 0f) {
            resizingPointsDistance = newResizingPointsDistance
        } else {
            val resizingFactor = calculateResizingFactor(resizingPointsDistance!!, newResizingPointsDistance)
            resizingState!!.updateScaleFactor(resizingFactor)
            viewStateCodes = if (resizingState!!.isResized) ViewStateCodes.Resized else ViewStateCodes.NotResized
            resizingPointsDistance = newResizingPointsDistance
            renderer.setScale(resizingState!!.scaleFactor)
            renderNow() // Update frame
        }
    }

    override fun completeResizing() {
        resizingPointsDistance = null
        viewStateCodes = if (resizingState!!.isResized) ViewStateCodes.Resized else ViewStateCodes.NotResized
        resizingState!!.recalculateMarginsByScaleFactor()

        val margins = resizingState!!.margins

        renderer.setMargins(margins!!) // too heavy
        draggingState.setCurrentMargins(margins)
        renderer.setScale(1f)

        renderNow()
        if (viewStateCodes === ViewStateCodes.NotResized) {
            renderer.setDragging(draggingState.reset()) // Place to center
            renderNow()
        }
    }

    override fun startDragging(point: PointF) {
        firstDraggingPoint = point
        draggingState.setViewInfo(renderer.viewInfo)
        draggingState.startDragging()
    }

    override fun dragging(point: PointF) {
        val deltaX = point.x - firstDraggingPoint!!.x
        val deltaY = point.y - firstDraggingPoint!!.y

        renderer.setDragging(draggingState.processDragging(deltaX, deltaY))
        renderNow() // Update frame
    }

    override fun completeDragging(point: PointF) {
        draggingState.completeDragging()
    }

    /**
     * A user lift his finger in some hot area
     * @param id id of the area
     */
    override fun onHotAreaHit(id: Int) {
        externalEventsHandler?.onHotAreaPressed(id)
    }

    override fun onTouch(view: View, me: MotionEvent): Boolean {
        if (animate || texturesManager == null) {
            return false
        }

        userActionManager.process(me, viewStateCodes)
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        texturesManager?.closeManager()
    }

    /**
     * Sets background color - or OpenGL clear color to be more precise. Color
     * is a 32bit value consisting of 0xAARRGGBB and is extracted using
     * android.graphics.Color eventually.
     */
    override fun setBackgroundColor(color: Int) {
        renderer.setBackgroundColor(color)
        renderNow()
    }

    /**
     * Invalidate view by demand
     */
    override fun renderNow() = requestRender()

    /**
     * Sets "hot" areas
     * @param areas a set of "special" areas, touch in them fires OneFingerDownInHotArea event
     */
    fun setHotAreas(areas: List<Area>) {
        userActionManager.setHotAreas(areas)
    }

    /**
     * Set current page index first time
     */
    fun initCurrentPageIndex(currentPageIndex: Int) {
        this.currentPageIndex = currentPageIndex
    }

    /**
     * Change index of current page and switch to this page
     */
    fun setCurrentPageIndex(currentPageIndex: Int) {
        this.currentPageIndex = currentPageIndex
        initPages(true)

        externalEventsHandler?.onPageChanged(currentPageIndex)
    }

    /**
     * Update/set bitmaps provider
     */
    fun setBitmapProvider(provider: BitmapProvider) {
        this.texturesManager = PageTexturesManager(provider, this)
    }

    fun setOnPageLoadingListener(handler: PageLoadingEventsHandler?) {
        texturesManager?.setOnLoadingListener(handler)
    }

    /**
     * Set callback handlers
     */
    fun setExternalEventsHandler(handler: CurlViewEventsHandler?) {
        externalEventsHandler = handler
    }

    /**
     * Calculate factor for changing margins while resizing;
     */
    private fun calculateResizingFactor(oldPointsDistance: Float, newPointsDistance: Float): Float {
        val resizingMultiplier = 6f
        val delta = newPointsDistance - oldPointsDistance
        return resizingMultiplier * (delta / screenDiagonal)
    }

    /**
     * Reset state of current page - cancel resizing and so on
     */
    private fun reset() {
        renderer.setDragging(draggingState.reset()) // Reset dragging

        renderNow()
        pageLeft.setFlipTexture(true)
        renderer.setViewMode()

        renderNow()

        resizingState = ResizingState(Margins(0f, 0f, 0f, 0f), 1f) // Original size
        renderer.setMargins(resizingState!!.margins!!)
        renderer.setScale(resizingState!!.scaleFactor)
        viewStateCodes = if (resizingState!!.isResized) ViewStateCodes.Resized else ViewStateCodes.NotResized
    }

    private fun memorizePoint(x: Float, y: Float, pressure: Float) {
        pointerPos.pos[x] = y
        renderer.translate(pointerPos.pos)
        if (enableTouchPressure) pointerPos.pressure = pressure else pointerPos.pressure = 0.8f
    }

    /**
     * Sets pageCurl curl position.
     */
    private fun setCurlPos(curlPos: PointF, curlDir: PointF, radius: Double) {
        // First reposition curl so that page doesn't 'rip off' from book.
        if (curlState === CurlState.Right || curlState === CurlState.Left) {
            val pageRect = renderer.getPageRect(CurlState.Right)

            if (curlPos.x >= pageRect!!.right) {
                pageCurl.reset()
                renderNow()
                return
            }

            if (curlPos.x < pageRect.left) {
                curlPos.x = pageRect.left
            }

            if (curlDir.y != 0f) {
                val diffX = curlPos.x - pageRect.left
                val leftY = curlPos.y + diffX * curlDir.x / curlDir.y

                if (curlDir.y < 0 && leftY < pageRect.top) {
                    curlDir.x = curlPos.y - pageRect.top
                    curlDir.y = pageRect.left - curlPos.x
                } else if (curlDir.y > 0 && leftY > pageRect.bottom) {
                    curlDir.x = pageRect.bottom - curlPos.y
                    curlDir.y = curlPos.x - pageRect.left
                }
            }
        } else if (curlState === CurlState.Left) {
            val pageRect = renderer.getPageRect(CurlState.Left)

            if (curlPos.x <= pageRect!!.left) {
                pageCurl.reset()
                renderNow()
                return
            }

            if (curlPos.x > pageRect.right) {
                curlPos.x = pageRect.right
            }

            if (curlDir.y != 0f) {
                val diffX = curlPos.x - pageRect.right
                val rightY = curlPos.y + diffX * curlDir.x / curlDir.y

                if (curlDir.y < 0 && rightY < pageRect.top) {
                    curlDir.x = pageRect.top - curlPos.y
                    curlDir.y = curlPos.x - pageRect.right
                } else if (curlDir.y > 0 && rightY > pageRect.bottom) {
                    curlDir.x = curlPos.y - pageRect.bottom
                    curlDir.y = pageRect.right - curlPos.x
                }
            }
        }

        // Finally normalize direction vector and do rendering.
        val dist = sqrt(curlDir.x * curlDir.x + curlDir.y * curlDir.y.toDouble())

        if (dist != 0.0) {
            curlDir.x /= dist.toFloat()
            curlDir.y /= dist.toFloat()
            pageCurl.curl(curlPos, curlDir, radius)
        } else {
            pageCurl.reset()
        }
        renderNow()
    }

    /**
     * Switches meshes and loads new bitmaps if available. Updated to support 2
     * pages in landscape
     */
    private fun startCurl(curlState: CurlState) {
        when (curlState) {
            CurlState.Right -> {
                // Remove meshes from renderer.
                renderer.removeCurlMesh(pageLeft)
                renderer.removeCurlMesh(pageRight)
                renderer.removeCurlMesh(pageCurl)

                // We are curling right page.
                val curl = pageRight
                pageRight = pageCurl
                pageCurl = curl

                if (currentPageIndex > 0) {
                    pageLeft.setFlipTexture(true)
                    pageLeft.setRect(renderer.getPageRect(CurlState.Left)!!)
                    pageLeft.reset()

                    if (renderLeftPage) {
                        renderer.addCurlMesh(pageLeft)
                    }
                }

                if (currentPageIndex < texturesManager!!.pageCount - 1) {
                    updatePage(pageRight.texturePage, currentPageIndex + 1)
                    pageRight.setRect(renderer.getPageRect(CurlState.Right)!!)
                    pageRight.setFlipTexture(false)
                    pageRight.reset()
                    renderer.addCurlMesh(pageRight)
                }

                // Add curled page to renderer.
                pageCurl.setRect(renderer.getPageRect(CurlState.Right)!!)
                pageCurl.setFlipTexture(false)
                pageCurl.reset()
                renderer.addCurlMesh(pageCurl)
                this.curlState = CurlState.Right
            }
            CurlState.Left -> {
                // Remove meshes from renderer.
                renderer.removeCurlMesh(pageLeft)
                renderer.removeCurlMesh(pageRight)
                renderer.removeCurlMesh(pageCurl)

                // We are curling left page.
                val curl = pageLeft

                pageLeft = pageCurl
                pageCurl = curl

                if (currentPageIndex > 1) {
                    updatePage(pageLeft.texturePage, currentPageIndex - 2)
                    pageLeft.setFlipTexture(true)
                    pageLeft.setRect(renderer.getPageRect(CurlState.Left)!!)
                    pageLeft.reset()

                    if (renderLeftPage) {
                        renderer.addCurlMesh(pageLeft)
                    }
                }

                // If there is something to show on right page add it to renderer.
                if (currentPageIndex < texturesManager!!.pageCount) {
                    pageRight.setFlipTexture(false)
                    pageRight.setRect(renderer.getPageRect(CurlState.Right)!!)
                    pageRight.reset()
                    renderer.addCurlMesh(pageRight)
                }

                // How dragging previous page happens depends on view mode.
                pageCurl.setRect(renderer.getPageRect(CurlState.Right)!!)
                pageCurl.setFlipTexture(false)
                pageCurl.reset()
                renderer.addCurlMesh(pageCurl)
                this.curlState = CurlState.Left
            }

            else -> throw UnsupportedOperationException("This value is not supported: $curlState")
        }
    }

    /**
     * Updates curl position.
     */
    private fun updateCurlPos(pointerPos: PointerPosition) { // Default curl radius.
        var radius = renderer.getPageRect(CurlState.Right)!!.width() / 3.toDouble()
        // TODO: This is not an optimal solution. Based on feedback received so
        // far; pressure is not very accurate, it may be better not to map
        // coefficient to range [0f, 1f] but something like [.2f, 1f] instead.
        // Leaving it as is until get my hands on a real device. On emulator
        // this doesn't work anyway.

        radius *= (1f - pointerPos.pressure).coerceAtLeast(0f).toDouble()
        // NOTE: Here we set pointerPos to curlPos. It might be a bit confusing
        // later to see e.g "curlPos.value1 - dragStartPos.value1" used. But it's
        // actually pointerPos we are doing calculations against. Why? Simply to
        // optimize code a bit with the cost of making it unreadable. Otherwise
        // we had to this in both of the next if-else branches.

        curlPos.set(pointerPos.pos)
        // If curl happens on right page, or on left page on two page mode,
        // we'll calculate curl position from pointerPos.

        if (curlState === CurlState.Right) {
            curlDir.x = curlPos.x - dragStartPos.x
            curlDir.y = curlPos.y - dragStartPos.y

            val dist = sqrt(curlDir.x * curlDir.x + curlDir.y * curlDir.y.toDouble()).toFloat()

            // Adjust curl radius so that if page is dragged far enough on
            // opposite side, radius gets closer to zero.
            val pageWidth = renderer.getPageRect(CurlState.Right)!!.width()
            var curlLen = radius * Math.PI

            if (dist > pageWidth * 2 - curlLen) {
                curlLen = (pageWidth * 2 - dist).coerceAtLeast(0f).toDouble()
                radius = curlLen / Math.PI
            }
            // Actual curl position calculation.
            if (dist >= curlLen) {
                val translate = (dist - curlLen) / 2
                val pageLeftX = renderer.getPageRect(CurlState.Right)!!.left
                radius = (curlPos.x - pageLeftX.toDouble()).coerceAtMost(radius).coerceAtLeast(0.0)
                curlPos.y -= (curlDir.y * translate / dist).toFloat()
            } else {
                val angle = Math.PI * sqrt(dist / curlLen)
                val translate = radius * sin(angle)
                curlPos.x += (curlDir.x * translate / dist).toFloat()
                curlPos.y += (curlDir.y * translate / dist).toFloat()
            }
        } else if (curlState === CurlState.Left) {
            // Adjust radius regarding how close to page edge we are.
            val pageLeftX = renderer.getPageRect(CurlState.Right)!!.left
            radius = (curlPos.x - pageLeftX.toDouble()).coerceAtMost(radius).coerceAtLeast(0.0)
            val pageRightX = renderer.getPageRect(CurlState.Right)!!.right
            curlPos.x -= (pageRightX - curlPos.x.toDouble()).coerceAtMost(radius).toFloat()
            curlDir.x = curlPos.x + dragStartPos.x
            curlDir.y = curlPos.y - dragStartPos.y
        }

        setCurlPos(curlPos, curlDir, radius)
    }

    /**
     * Updates given CurlPage via PageProvider for page located at index.
     */
    private fun updatePage(page: CurlPage, index: Int) {
        // First reset page to initial state.
        page.reset()

        // Ask page provider to fill it up with bitmaps and colors.
        texturesManager!!.updatePage(page, Size(pageBitmapWidth, pageBitmapHeight), index)
    }

    /**
     * Updates bitmaps for page meshes.
     */
    private fun initPages(resetTextureManager: Boolean) {
        if (texturesManager == null || pageBitmapWidth <= 0 || pageBitmapHeight <= 0) {
            return
        }

        val leftIdx = currentPageIndex - 1
        val rightIdx = currentPageIndex

        val needToUpdateRightPage = rightIdx >= 0 && rightIdx < texturesManager!!.pageCount
        val needToUpdateLeftPage = leftIdx >= 0 && leftIdx < texturesManager!!.pageCount && renderLeftPage

        texturesManager?.init(
            Size(pageBitmapWidth, pageBitmapHeight),
            currentPageIndex,
            reset = resetTextureManager,
            updateTwoPagesNeeded = needToUpdateRightPage && needToUpdateLeftPage
        ) {
            // Remove meshes from renderer.
            renderer.removeCurlMesh(pageLeft)
            renderer.removeCurlMesh(pageRight)
            renderer.removeCurlMesh(pageCurl)

            if (needToUpdateRightPage) {
                updatePage(pageRight.texturePage, rightIdx)
                pageRight.setFlipTexture(false)
                pageRight.setRect(renderer.getPageRect(CurlState.Right)!!)
                pageRight.reset()
                renderer.addCurlMesh(pageRight)
            }

            // It's a dirty hack, I know. But we need to draw the second page with a slight delay in this concrete case
            if (needToUpdateLeftPage) {
                postDelayed({
                    updatePage(pageLeft.texturePage, leftIdx)
                    pageLeft.setFlipTexture(true)
                    pageLeft.setRect(renderer.getPageRect(CurlState.Left)!!)
                    pageLeft.reset()
                    renderer.addCurlMesh(pageLeft)
                }, 250L)
            }
        }
    }

    companion object {
        /**
         * Maximum number curl can be divided into
         */
        private const val MAX_CURL_SPLITS = 10
    }
}