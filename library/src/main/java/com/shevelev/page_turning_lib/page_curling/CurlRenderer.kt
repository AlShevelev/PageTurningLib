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

import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.util.SizeF
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Actual renderer class.
 */
internal class CurlRenderer(
    private val observer: CurlRendererObserver
) : GLSurfaceView.Renderer {
    // Background fill color.
    private var backgroundColor = 0
    // Curl meshes used for static and dynamic rendering.

    private val curlMeshes: Vector<CurlMesh> = Vector()

    private val margins = RectF()

    // Page rectangles.
    private val pageRectLeft: RectF = RectF()
    private val pageRectRight: RectF = RectF()

    // Screen size.
    private var viewportWidth = 0
    private var viewportHeight = 0

    // Rect for render area.
    private val viewRect = RectF()

    // Size of view area [px]
    private var viewAreaSize: SizeF? = null

    // Scale factor by X and Y
    private var scale = 1f

    // Dragging factor by X and Y
    private var dragging = Pair(0f, 0f)

    val viewInfo: RendererViewInfo
        get() = RendererViewInfo(viewRect, viewAreaSize!!)

    @Synchronized
    override fun onDrawFrame(gl: GL10) {
        observer.onDrawFrame()
        gl.glClearColor(
            Color.red(backgroundColor) / 255f,
            Color.green(backgroundColor) / 255f,
            Color.blue(backgroundColor) / 255f,
            Color.alpha(backgroundColor) / 255f
        )

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
        gl.glLoadIdentity()
        gl.glScalef(scale, scale, 0f)
        gl.glTranslatef(dragging.value1, dragging.value2, 0f)

        for (i in curlMeshes.indices) {
            curlMeshes[i].onDrawFrame(gl)
        }
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        gl.glViewport(0, 0, width, height)

        viewportWidth = width
        viewportHeight = height

        viewAreaSize = SizeF(width.toFloat(), height.toFloat())

        val ratio = width.toFloat() / height
        viewRect.top = 1.0f
        viewRect.bottom = -1.0f
        viewRect.left = -ratio
        viewRect.right = ratio

        updatePageBounds()

        gl.glMatrixMode(GL10.GL_PROJECTION)
        gl.glLoadIdentity()
        GLU.gluOrtho2D(gl, viewRect.left, viewRect.right, viewRect.bottom, viewRect.top)
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glLoadIdentity()
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        gl.glClearColor(0f, 0f, 0f, 1f)
        gl.glShadeModel(GL10.GL_SMOOTH)
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST)
        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST)
        gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_NICEST)
        gl.glEnable(GL10.GL_LINE_SMOOTH)
        gl.glDisable(GL10.GL_DEPTH_TEST)
        gl.glDisable(GL10.GL_CULL_FACE)

        observer.onSurfaceCreated()
    }

    /**
     * Adds CurlMesh to this renderer.
     */
    @Synchronized
    fun addCurlMesh(mesh: CurlMesh) {
        removeCurlMesh(mesh)
        curlMeshes.add(mesh)
    }

    /**
     * Returns rect reserved for left or right page. Value page should be
     * PAGE_LEFT or PAGE_RIGHT.
     */
    fun getPageRect(page: CurlState): RectF? {
        if (page == CurlState.Left) {
            return pageRectLeft
        } else if (page == CurlState.Right) {
            return pageRectRight
        }
        return null
    }

    fun setScale(scale: Float) {
        this.scale = scale
    }

    fun setDragging(dragging: Pair<Float>) {
        this.dragging = dragging
    }

    /**
     * Removes CurlMesh from this renderer.
     */
    @Suppress("ControlFlowWithEmptyBody")
    @Synchronized
    fun removeCurlMesh(mesh: CurlMesh?) {
        while (curlMeshes.remove(mesh));
    }

    /**
     * Change background/clear color.
     */
    fun setBackgroundColor(color: Int) {
        backgroundColor = color
    }

    /**
     * Set margins or padding. Note: margins are proportional. Meaning a value
     * of .1f will produce a 10% margin.
     */
    @Synchronized
    fun setMargins(margins: Margins) {
        this.margins.left = margins.left
        this.margins.top = margins.top
        this.margins.right = margins.right
        this.margins.bottom = margins.bottom
        updatePageBounds()
    }

    /**
     * Sets visible page count to one or two. Should be either SHOW_ONE_PAGE or
     * SHOW_TWO_PAGES.
     */
    @Synchronized
    fun setViewMode() {
        updatePageBounds()
    }

    /**
     * Translates screen coordinates into view coordinates.
     */
    fun translate(pt: PointF) {
        pt.x = viewRect.left + viewRect.width() * pt.x / viewportWidth
        pt.y = viewRect.top - -viewRect.height() * pt.y / viewportHeight
    }

    /**
     * Recalculates page rectangles.
     */
    private fun updatePageBounds() {
        if (viewRect.width() == 0f || viewRect.height() == 0f) return else {
            pageRectRight.set(viewRect) // Resize and move viewRect for scale and slide image
            pageRectRight.left += viewRect.width() * margins.left
            pageRectRight.right -= viewRect.width() * margins.right
            pageRectRight.top += viewRect.height() * margins.top
            pageRectRight.bottom -= viewRect.height() * margins.bottom
            pageRectLeft.set(pageRectRight)
            pageRectLeft.offset(-pageRectRight.width(), 0f)
            val bitmapW = (pageRectRight.width() * viewportWidth / viewRect.width()).toInt()
            val bitmapH = (pageRectRight.height() * viewportHeight / viewRect.height()).toInt()
            observer.onPageSizeChanged(bitmapW, bitmapH)
        }
    }
}