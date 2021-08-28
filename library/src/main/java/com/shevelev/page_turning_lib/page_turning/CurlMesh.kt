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

package com.shevelev.page_turning_lib.page_turning

import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

/**
 * Class implementing actual curl/page rendering.
 */
class CurlMesh(maxCurlSplits: Int) {
    // Let's avoid using 'new' as much as possible. Meaning we introduce arrays
// once here and reuse them on runtime. Doesn't really have very much effect
// but avoids some garbage collections from happening.
    private var arrDropShadowVertices: Array<ShadowVertex>? = null
    private val arrIntersections: Array<Vertex>
    private val arrOutputVertices: Array<Vertex>
    private val arrRotatedVertices: Array<Vertex>
    private val arrScanLines: Array<Double>
    private var arrSelfShadowVertices: Array<ShadowVertex>? = null
    private var arrTempShadowVertices: Array<ShadowVertex>? = null
    private val arrTempVertices: Array<Vertex>
    // Buffers for feeding rasterizer.
    private val bufColors: FloatBuffer
    private var bufCurlPositionLines: FloatBuffer? = null
    private var bufShadowColors: FloatBuffer? = null
    private var bufShadowVertices: FloatBuffer? = null
    private var bufTexCoords: FloatBuffer? = null
    private val bufVertices: FloatBuffer
    private var curlPositionLinesCount = 0
    private var dropShadowCount = 0
    // Boolean for 'flipping' texture sideways.
    private var flipTexture = false
    // Maximum number of split lines used for creating a curl.
    private val maxCurlSplits: Int
    // Bounding rectangle for this mesh. mRectagle[0] = top-left corner,
// rectangle[1] = bottom-left, rectangle[2] = top-right and rectangle[3]
// bottom-right.
    private val rectangle = arrayOfNulls<Vertex>(4)
    private var selfShadowCount = 0
    private var textureBack = false
    // Texture ids and other variables.
    private var textureIds: IntArray? = null
    /**
     * Getter for textures page for this mesh.
     */
    @get:Synchronized
    val texturePage = CurlPage()
    private val textureRectBack = RectF()
    private val textureRectFront = RectF()
    private var verticesCountBack = 0
    private var verticesCountFront = 0
    /**
     * Adds vertex to buffers.
     */
    private fun addVertex(vertex: Vertex) {
        bufVertices.put(vertex.mPosX.toFloat())
        bufVertices.put(vertex.mPosY.toFloat())
        bufVertices.put(vertex.mPosZ.toFloat())
        bufColors.put(vertex.mColorFactor * Color.red(vertex.mColor) / 255f)
        bufColors.put(vertex.mColorFactor * Color.green(vertex.mColor) / 255f)
        bufColors.put(vertex.mColorFactor * Color.blue(vertex.mColor) / 255f)
        bufColors.put(Color.alpha(vertex.mColor) / 255f)
        if (DRAW_TEXTURE) {
            bufTexCoords!!.put(vertex.mTexX.toFloat())
            bufTexCoords!!.put(vertex.mTexY.toFloat())
        }
    }

    /**
     * Sets curl for this mesh.
     *
     * @param curlPos  Position for curl 'center'. Can be any point on line collinear to curl.
     * @param curlDir Curl direction, should be normalized.
     * @param radius Radius of curl.
     */
    @Synchronized
    fun curl(curlPos: PointF, curlDir: PointF, radius: Double) {
        if (DRAW_CURL_POSITION) // First add some 'helper' lines used for development.
        {
            bufCurlPositionLines!!.position(0)
            bufCurlPositionLines!!.put(curlPos.x)
            bufCurlPositionLines!!.put(curlPos.y - 1.0f)
            bufCurlPositionLines!!.put(curlPos.x)
            bufCurlPositionLines!!.put(curlPos.y + 1.0f)
            bufCurlPositionLines!!.put(curlPos.x - 1.0f)
            bufCurlPositionLines!!.put(curlPos.y)
            bufCurlPositionLines!!.put(curlPos.x + 1.0f)
            bufCurlPositionLines!!.put(curlPos.y)
            bufCurlPositionLines!!.put(curlPos.x)
            bufCurlPositionLines!!.put(curlPos.y)
            bufCurlPositionLines!!.put(curlPos.x + curlDir.x * 2)
            bufCurlPositionLines!!.put(curlPos.y + curlDir.y * 2)
            bufCurlPositionLines!!.position(0)
        }
        // Actual 'curl' implementation starts here.
        bufVertices.position(0)
        bufColors.position(0)
        if (DRAW_TEXTURE) bufTexCoords!!.position(0)
        // Calculate curl angle from direction.
        var curlAngle = Math.acos(curlDir.x.toDouble())
        curlAngle = if (curlDir.y > 0) -curlAngle else curlAngle
        // Initiate rotated rectangle which's is translated to curlPos and
// rotated so that curl direction heads to right (1,0). Vertices are
// ordered in ascending order based on value1 -coordinate at the same time.
// And using value2 -coordinate in very rare case in which two vertices have
// same value1 -coordinate.
        arrTempVertices.addAll(arrRotatedVertices)
        arrRotatedVertices.clear()
        for (i in 0..3) {
            val v = arrTempVertices.remove(0)!!
            v.set(rectangle[i]!!)
            v.translate(-curlPos.x.toDouble(), -curlPos.y.toDouble())
            v.rotateZ(-curlAngle)
            var j = 0
            while (j < arrRotatedVertices.size()) {
                val v2 = arrRotatedVertices[j]!!
                if (v.mPosX > v2.mPosX) break
                if (v.mPosX == v2.mPosX && v.mPosY > v2.mPosY) break
                ++j
            }
            arrRotatedVertices.add(j, v)
        }
        // Rotated rectangle lines/vertex indices. We need to find bounding
// lines for rotated rectangle. After sorting vertices according to
// their value1 -coordinate we don't have to worry about vertices at indices
// 0 and 1. But due to inaccuracy it's possible vertex 3 is not the
// opposing corner from vertex 0. So we are calculating distance from
// vertex 0 to vertices 2 and 3 - and altering line indices if needed.
// Also vertices/lines are given in an order first one has value1 -coordinate
// at least the latter one. This property is used in getIntersections to
// see if there is an intersection.
        val lines = arrayOf(intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(1, 3), intArrayOf(2, 3))
        run {
            // TODO: There really has to be more 'easier' way of doing this -
// not including extensive use of sqrt.
            val v0 = arrRotatedVertices[0]!!
            val v2 = arrRotatedVertices[2]!!
            val v3 = arrRotatedVertices[3]!!
            val dist2 = Math.sqrt((v0.mPosX - v2.mPosX)
                * (v0.mPosX - v2.mPosX) + (v0.mPosY - v2.mPosY)
                * (v0.mPosY - v2.mPosY))
            val dist3 = Math.sqrt((v0.mPosX - v3.mPosX)
                * (v0.mPosX - v3.mPosX) + (v0.mPosY - v3.mPosY)
                * (v0.mPosY - v3.mPosY))
            if (dist2 > dist3) {
                lines[1][1] = 3
                lines[2][1] = 2
            }
        }
        verticesCountBack = 0
        verticesCountFront = verticesCountBack
        if (DRAW_SHADOW) {
            arrTempShadowVertices!!.addAll(arrDropShadowVertices!!)
            arrTempShadowVertices!!.addAll(arrSelfShadowVertices!!)
            arrDropShadowVertices!!.clear()
            arrSelfShadowVertices!!.clear()
        }
        // Length of 'curl' curve.
        val curlLength = Math.PI * radius
        // Calculate scan lines.
// TODO: Revisit this code one day. There is room for optimization here.
        arrScanLines.clear()
        if (maxCurlSplits > 0) arrScanLines.add(0.toDouble())
        for (i in 1 until maxCurlSplits) arrScanLines.add(-curlLength * i / (maxCurlSplits - 1))
        // As mRotatedVertices is ordered regarding value1 -coordinate, adding
// this scan line produces scan area picking up vertices which are
// rotated completely. One could say 'until infinity'.
        arrScanLines.add(arrRotatedVertices[3]!!.mPosX - 1)
        // Start from right most vertex. Pretty much the same as first scan area
// is starting from 'infinity'.
        var scanXmax = arrRotatedVertices[0]!!.mPosX + 1
        for (i in 0 until arrScanLines.size()) { // Once we have scanXmin and scanXmax we have a scan area to start
// working with.
            val scanXmin = arrScanLines[i]!!
            // First iterate 'original' rectangle vertices within scan area.
            for (j in 0 until arrRotatedVertices.size()) {
                val v = arrRotatedVertices[j]!!
                // Test if vertex lies within this scan area.
// TODO: Frankly speaking, can't remember why equality check was
// added to both ends. Guessing it was somehow related to case
// where radius=0f, which, given current implementation, could
// be handled much more effectively anyway.
                if (v.mPosX >= scanXmin && v.mPosX <= scanXmax) { // Pop out a vertex from temp vertices.
                    val n = arrTempVertices.remove(0)!!
                    n.set(v)
                    // This is done solely for triangulation reasons. Given a
// rotated rectangle it has max 2 vertices having
// intersection.
                    val intersections = getIntersections(
                        arrRotatedVertices, lines, n.mPosX)
                    // In a sense one could say we're adding vertices always in
// two, positioned at the ends of intersecting line. And for
// triangulation to work properly they are added based on value2
// -coordinate. And this if-else is doing it for us.
                    if (intersections.size() == 1 && intersections[0]!!.mPosY > v.mPosY) { // In case intersecting vertex is higher add it first.
                        arrOutputVertices.addAll(intersections)
                        arrOutputVertices.add(n)
                    } else if (intersections.size() <= 1) { // Otherwise add original vertex first.
                        arrOutputVertices.add(n)
                        arrOutputVertices.addAll(intersections)
                    } else { // There should never be more than 1 intersecting
// vertex. But if it happens as a fallback simply skip
// everything.
                        arrTempVertices.add(n)
                        arrTempVertices.addAll(intersections)
                    }
                }
            }
            // Search for scan line intersections.
            val intersections = getIntersections(arrRotatedVertices,
                lines, scanXmin)
            // We expect to get 0 or 2 vertices. In rare cases there's only one
// but in general given a scan line intersecting rectangle there
// should be 2 intersecting vertices.
            if (intersections.size() == 2) { // There were two intersections, add them based on value2
// -coordinate, higher first, lower last.
                val v1 = intersections[0]!!
                val v2 = intersections[1]!!
                if (v1.mPosY < v2.mPosY) {
                    arrOutputVertices.add(v2)
                    arrOutputVertices.add(v1)
                } else {
                    arrOutputVertices.addAll(intersections)
                }
            } else if (intersections.size() != 0) { // This happens in a case in which there is a original vertex
// exactly at scan line or something went very much wrong if
// there are 3+ vertices. What ever the reason just return the
// vertices to temp vertices for later use. In former case it
// was handled already earlier once iterating through
// mRotatedVertices, in latter case it's better to avoid doing
// anything with them.
                arrTempVertices.addAll(intersections)
            }
            // Add vertices found during this iteration to vertex etc buffers.
            while (arrOutputVertices.size() > 0) {
                val v = arrOutputVertices.remove(0)!!
                arrTempVertices.add(v)
                // Local texture front-facing flag.
                var textureFront: Boolean
                // Untouched vertices.
                if (i == 0) {
                    textureFront = true
                    verticesCountFront++
                } else if (i == arrScanLines.size() - 1 || curlLength == 0.0) {
                    v.mPosX = -(curlLength + v.mPosX)
                    v.mPosZ = 2 * radius
                    v.mPenumbraX = -v.mPenumbraX
                    textureFront = false
                    verticesCountBack++
                } else { // Even though it's not obvious from the if-else clause,
// here v.mPosX is between [-curlLength, 0]. And we can do
// calculations around a half cylinder.
                    val rotY = Math.PI * (v.mPosX / curlLength)
                    v.mPosX = radius * Math.sin(rotY)
                    v.mPosZ = radius - radius * Math.cos(rotY)
                    v.mPenumbraX *= Math.cos(rotY)
                    // Map color multiplier to [.1f, 1f] range.
                    v.mColorFactor = (.1f + .9f * Math.sqrt(Math
                        .sin(rotY) + 1)).toFloat()
                    if (v.mPosZ >= radius) {
                        textureFront = false
                        verticesCountBack++
                    } else {
                        textureFront = true
                        verticesCountFront++
                    }
                }
                // We use local textureFront for flipping backside texture
// locally. Plus additionally if mesh is in flip texture mode,
// we'll make the procedure "backwards". Also, until this point,
// texture coordinates are within [0, 1] range so we'll adjust
// them to final texture coordinates too.
                if (textureFront != flipTexture) {
                    v.mTexX *= textureRectFront.right.toDouble()
                    v.mTexY *= textureRectFront.bottom.toDouble()
                    v.mColor = texturePage.getColor(PageSide.Front)
                } else {
                    v.mTexX *= textureRectBack.right.toDouble()
                    v.mTexY *= textureRectBack.bottom.toDouble()
                    v.mColor = texturePage.getColor(PageSide.Back)
                }
                // Move vertex back to 'world' coordinates.
                v.rotateZ(curlAngle)
                v.translate(curlPos.x.toDouble(), curlPos.y.toDouble())
                addVertex(v)
                // Drop shadow is cast 'behind' the curl.
                if (DRAW_SHADOW && v.mPosZ > 0 && v.mPosZ <= radius) {
                    val sv = arrTempShadowVertices!!.remove(0)!!
                    sv.mPosX = v.mPosX
                    sv.mPosY = v.mPosY
                    sv.mPosZ = v.mPosZ
                    sv.mPenumbraX = v.mPosZ / 2 * -curlDir.x
                    sv.mPenumbraY = v.mPosZ / 2 * -curlDir.y
                    sv.mPenumbraColor = v.mPosZ / radius
                    val idx = (arrDropShadowVertices!!.size() + 1) / 2
                    arrDropShadowVertices!!.add(idx, sv)
                }
                // Self shadow is cast partly over mesh.
                if (DRAW_SHADOW && v.mPosZ > radius) {
                    val sv = arrTempShadowVertices!!.remove(0)!!
                    sv.mPosX = v.mPosX
                    sv.mPosY = v.mPosY
                    sv.mPosZ = v.mPosZ
                    sv.mPenumbraX = (v.mPosZ - radius) / 3 * v.mPenumbraX
                    sv.mPenumbraY = (v.mPosZ - radius) / 3 * v.mPenumbraY
                    sv.mPenumbraColor = (v.mPosZ - radius) / (2 * radius)
                    val idx = (arrSelfShadowVertices!!.size() + 1) / 2
                    arrSelfShadowVertices!!.add(idx, sv)
                }
            }
            // Switch scanXmin as scanXmax for next iteration.
            scanXmax = scanXmin
        }
        bufVertices.position(0)
        bufColors.position(0)
        if (DRAW_TEXTURE) bufTexCoords!!.position(0)
        // Add shadow Vertices.
        if (DRAW_SHADOW) {
            bufShadowColors!!.position(0)
            bufShadowVertices!!.position(0)
            dropShadowCount = 0
            for (i in 0 until arrDropShadowVertices!!.size()) {
                val sv = arrDropShadowVertices!![i]!!
                bufShadowVertices!!.put(sv.mPosX.toFloat())
                bufShadowVertices!!.put(sv.mPosY.toFloat())
                bufShadowVertices!!.put(sv.mPosZ.toFloat())
                bufShadowVertices!!.put((sv.mPosX + sv.mPenumbraX).toFloat())
                bufShadowVertices!!.put((sv.mPosY + sv.mPenumbraY).toFloat())
                bufShadowVertices!!.put(sv.mPosZ.toFloat())
                for (j in 0..3) {
                    val color = SHADOW_OUTER_COLOR[j] + (SHADOW_INNER_COLOR[j] - SHADOW_OUTER_COLOR[j]) * sv.mPenumbraColor
                    bufShadowColors!!.put(color.toFloat())
                }
                bufShadowColors!!.put(SHADOW_OUTER_COLOR)
                dropShadowCount += 2
            }
            selfShadowCount = 0
            for (i in 0 until arrSelfShadowVertices!!.size()) {
                val sv = arrSelfShadowVertices!![i]!!
                bufShadowVertices!!.put(sv.mPosX.toFloat())
                bufShadowVertices!!.put(sv.mPosY.toFloat())
                bufShadowVertices!!.put(sv.mPosZ.toFloat())
                bufShadowVertices!!.put((sv.mPosX + sv.mPenumbraX).toFloat())
                bufShadowVertices!!.put((sv.mPosY + sv.mPenumbraY).toFloat())
                bufShadowVertices!!.put(sv.mPosZ.toFloat())
                for (j in 0..3) {
                    val color = SHADOW_OUTER_COLOR[j] + (SHADOW_INNER_COLOR[j] - SHADOW_OUTER_COLOR[j]) * sv.mPenumbraColor
                    bufShadowColors!!.put(color.toFloat())
                }
                bufShadowColors!!.put(SHADOW_OUTER_COLOR)
                selfShadowCount += 2
            }
            bufShadowColors!!.position(0)
            bufShadowVertices!!.position(0)
        }
    }

    /**
     * Calculates intersections for given scan line.
     */
    private fun getIntersections(vertices: Array<Vertex>, lineIndices: kotlin.Array<IntArray>, scanX: Double): Array<Vertex> {
        arrIntersections.clear()
        // Iterate through rectangle lines each re-presented as a pair of
// vertices.
        for (j in lineIndices.indices) {
            val v1 = vertices[lineIndices[j][0]]!!
            val v2 = vertices[lineIndices[j][1]]!!
            // Here we expect that v1.mPosX >= v2.mPosX and wont do intersection
// test the opposite way.
            if (v1.mPosX > scanX && v2.mPosX < scanX) { // There is an intersection, calculate coefficient telling 'how
// far' scanX is from v2.
                val c = (scanX - v2.mPosX) / (v1.mPosX - v2.mPosX)
                val n = arrTempVertices.remove(0)!!
                n.set(v2)
                n.mPosX = scanX
                n.mPosY += (v1.mPosY - v2.mPosY) * c
                if (DRAW_TEXTURE) {
                    n.mTexX += (v1.mTexX - v2.mTexX) * c
                    n.mTexY += (v1.mTexY - v2.mTexY) * c
                }
                if (DRAW_SHADOW) {
                    n.mPenumbraX += (v1.mPenumbraX - v2.mPenumbraX) * c
                    n.mPenumbraY += (v1.mPenumbraY - v2.mPenumbraY) * c
                }
                arrIntersections.add(n)
            }
        }
        return arrIntersections
    }

    /**
     * Renders our page curl mesh.
     */
    @Synchronized
    fun onDrawFrame(gl: GL10) { // First allocate texture if there is not one yet.
        if (DRAW_TEXTURE && textureIds == null) { // Generate texture.
            textureIds = IntArray(2)
            gl.glGenTextures(2, textureIds, 0)
            for (textureId in textureIds!!) { // Set texture attributes.
                gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId)
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat())
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST.toFloat())
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE.toFloat())
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE.toFloat())
            }
        }
        if (DRAW_TEXTURE && texturePage.texturesChanged) {
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds!![0])
            var texture = texturePage.getTexture(textureRectFront, PageSide.Front)
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, texture, 0)
            texture.recycle()
            textureBack = texturePage.hasBackTexture()
            if (textureBack) {
                gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds!![1])
                texture = texturePage.getTexture(textureRectBack, PageSide.Back)
                GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, texture, 0)
                texture.recycle()
            } else textureRectBack.set(textureRectFront)
            texturePage.recycle()
            reset()
        }
        // Some 'global' settings.
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        // TODO: Drop shadow drawing is done temporarily here to hide some
// problems with its calculation.
        if (DRAW_SHADOW) {
            gl.glDisable(GL10.GL_TEXTURE_2D)
            gl.glEnable(GL10.GL_BLEND)
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
            gl.glColorPointer(4, GL10.GL_FLOAT, 0, bufShadowColors)
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufShadowVertices)
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, dropShadowCount)
            gl.glDisableClientState(GL10.GL_COLOR_ARRAY)
            gl.glDisable(GL10.GL_BLEND)
        }
        if (DRAW_TEXTURE) {
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, bufTexCoords)
        }
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufVertices)
        // Enable color array.
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, bufColors)
        // Draw front facing blank vertices.
        gl.glDisable(GL10.GL_TEXTURE_2D)
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, verticesCountFront)
        // Draw front facing texture.
        if (DRAW_TEXTURE) {
            gl.glEnable(GL10.GL_BLEND)
            gl.glEnable(GL10.GL_TEXTURE_2D)
            if (!flipTexture || !textureBack) {
                gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds!![0])
            } else {
                gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds!![1])
            }
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, verticesCountFront)
            gl.glDisable(GL10.GL_BLEND)
            gl.glDisable(GL10.GL_TEXTURE_2D)
        }
        val backStartIdx = Math.max(0, verticesCountFront - 2)
        val backCount = verticesCountFront + verticesCountBack - backStartIdx
        // Draw back facing blank vertices.
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, backStartIdx, backCount)
        // Draw back facing texture.
        if (DRAW_TEXTURE) {
            gl.glEnable(GL10.GL_BLEND)
            gl.glEnable(GL10.GL_TEXTURE_2D)
            if (flipTexture || !textureBack) gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds!![0]) else gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds!![1])
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, backStartIdx, backCount)
            gl.glDisable(GL10.GL_BLEND)
            gl.glDisable(GL10.GL_TEXTURE_2D)
        }
        // Disable textures and color array.
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY)
        if (DRAW_POLYGON_OUTLINES) {
            gl.glEnable(GL10.GL_BLEND)
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glLineWidth(1.0f)
            gl.glColor4f(0.5f, 0.5f, 1.0f, 1.0f)
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufVertices)
            gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, verticesCountFront)
            gl.glDisable(GL10.GL_BLEND)
        }
        if (DRAW_CURL_POSITION) {
            gl.glEnable(GL10.GL_BLEND)
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glLineWidth(1.0f)
            gl.glColor4f(1.0f, 0.5f, 0.5f, 1.0f)
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, bufCurlPositionLines)
            gl.glDrawArrays(GL10.GL_LINES, 0, curlPositionLinesCount * 2)
            gl.glDisable(GL10.GL_BLEND)
        }
        if (DRAW_SHADOW) {
            gl.glEnable(GL10.GL_BLEND)
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
            gl.glColorPointer(4, GL10.GL_FLOAT, 0, bufShadowColors)
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufShadowVertices)
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, dropShadowCount,
                selfShadowCount)
            gl.glDisableClientState(GL10.GL_COLOR_ARRAY)
            gl.glDisable(GL10.GL_BLEND)
        }
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
    }

    /**
     * Resets mesh to 'initial' state. Meaning this mesh will draw a plain
     * textured rectangle after call to this method.
     */
    @Synchronized
    fun reset() {
        bufVertices.position(0)
        bufColors.position(0)
        if (DRAW_TEXTURE) bufTexCoords!!.position(0)
        for (i in 0..3) {
            val tmp = arrTempVertices[0]!!
            tmp.set(rectangle[i]!!)
            if (flipTexture) {
                tmp.mTexX *= textureRectBack.right.toDouble()
                tmp.mTexY *= textureRectBack.bottom.toDouble()
                tmp.mColor = texturePage.getColor(PageSide.Back)
            } else {
                tmp.mTexX *= textureRectFront.right.toDouble()
                tmp.mTexY *= textureRectFront.bottom.toDouble()
                tmp.mColor = texturePage.getColor(PageSide.Front)
            }
            addVertex(tmp)
        }
        verticesCountFront = 4
        verticesCountBack = 0
        bufVertices.position(0)
        bufColors.position(0)
        if (DRAW_TEXTURE) {
            bufTexCoords!!.position(0)
        }
        selfShadowCount = 0
        dropShadowCount = selfShadowCount
    }

    /**
     * Resets allocated texture id forcing creation of new one. After calling
     * this method you most likely want to set bitmap too as it's lost. This
     * method should be called only once e.g GL context is re-created as this
     * method does not release previous texture id, only makes sure new one is
     * requested on next render.
     */
    @Synchronized
    fun resetTexture() {
        textureIds = null
    }

    /**
     * If true, flips texture sideways.
     */
    @Synchronized
    fun setFlipTexture(flipTexture: Boolean) {
        this.flipTexture = flipTexture
        if (flipTexture) {
            setTexCoords(1f, 0f, 0f, 1f)
        } else {
            setTexCoords(0f, 0f, 1f, 1f)
        }
    }

    /**
     * Update mesh bounds.
     */
    fun setRect(r: RectF) {
        rectangle[0]!!.mPosX = r.left.toDouble()
        rectangle[0]!!.mPosY = r.top.toDouble()
        rectangle[1]!!.mPosX = r.left.toDouble()
        rectangle[1]!!.mPosY = r.bottom.toDouble()
        rectangle[2]!!.mPosX = r.right.toDouble()
        rectangle[2]!!.mPosY = r.top.toDouble()
        rectangle[3]!!.mPosX = r.right.toDouble()
        rectangle[3]!!.mPosY = r.bottom.toDouble()
    }

    /**
     * Sets texture coordinates to rectangle vertices.
     */
    @Synchronized
    private fun setTexCoords(left: Float, top: Float, right: Float, bottom: Float) {
        rectangle[0]!!.mTexX = left.toDouble()
        rectangle[0]!!.mTexY = top.toDouble()
        rectangle[1]!!.mTexX = left.toDouble()
        rectangle[1]!!.mTexY = bottom.toDouble()
        rectangle[2]!!.mTexX = right.toDouble()
        rectangle[2]!!.mTexY = top.toDouble()
        rectangle[3]!!.mTexX = right.toDouble()
        rectangle[3]!!.mTexY = bottom.toDouble()
    }

    /**
     * Simple fixed size array implementation.
     */
    private inner class Array<T>(private val mCapacity: Int) {
        private val mArray: kotlin.Array<Any?>
        private var mSize = 0
        fun add(index: Int, item: T) {
            if (index < 0 || index > mSize || mSize >= mCapacity) throw IndexOutOfBoundsException()
            for (i in mSize downTo index + 1) mArray[i] = mArray[i - 1]
            mArray[index] = item
            ++mSize
        }

        fun add(item: T) {
            if (mSize >= mCapacity) throw IndexOutOfBoundsException()
            mArray[mSize++] = item
        }

        fun addAll(array: Array<T>) {
            if (mSize + array.size() > mCapacity) throw IndexOutOfBoundsException()
            for (i in 0 until array.size()) mArray[mSize++] = array[i]
        }

        fun clear() {
            mSize = 0
        }

        operator fun get(index: Int): T? {
            if (index < 0 || index >= mSize) throw IndexOutOfBoundsException()
            return mArray[index] as T?
        }

        fun remove(index: Int): T? {
            if (index < 0 || index >= mSize) throw IndexOutOfBoundsException()
            val item = mArray[index] as T?
            for (i in index until mSize - 1) mArray[i] = mArray[i + 1]
            --mSize
            return item
        }

        fun size(): Int {
            return mSize
        }

        init {
            mArray = arrayOfNulls(mCapacity)
        }
    }

    companion object {
        // Flag for rendering some lines used for developing. Shows
// curl position and one for the direction from the
// position given. Comes handy once playing around with different
// ways for following pointer.
        private const val DRAW_CURL_POSITION = false
        // Flag for drawing polygon outlines. Using this flag crashes on emulator
// due to reason unknown to me. Leaving it here anyway as seeing polygon
// outlines gives good insight how original rectangle is divided.
        private const val DRAW_POLYGON_OUTLINES = false
        // Flag for enabling shadow rendering.
        private const val DRAW_SHADOW = true
        // Flag for texture rendering. While this is likely something you
// don't want to do it's been used for development purposes as texture
// rendering is rather slow on emulator.
        private const val DRAW_TEXTURE = true
        // Colors for shadow. Inner one is the color drawn next to surface where
// shadowed area starts and outer one is color shadow ends to.
        private val SHADOW_INNER_COLOR = floatArrayOf(0f, 0f, 0f, .5f)
        private val SHADOW_OUTER_COLOR = floatArrayOf(0f, 0f, 0f, .0f)
    }

    /**
     * Constructor for mesh object.
     *
     * @param maxCurlSplits
     * Maximum number curl can be divided into. The bigger the value
     * the smoother curl will be. With the cost of having more
     * polygons for drawing.
     */
    init { // There really is no use for 0 splits.
        this.maxCurlSplits = if (maxCurlSplits < 1) 1 else maxCurlSplits
        arrScanLines = Array(maxCurlSplits + 2)
        arrOutputVertices = Array(7)
        arrRotatedVertices = Array(4)
        arrIntersections = Array(2)
        arrTempVertices = Array(7 + 4)
        for (i in 0 until 7 + 4) arrTempVertices.add(Vertex())
        if (DRAW_SHADOW) {
            arrSelfShadowVertices = Array((this.maxCurlSplits + 2) * 2)
            arrDropShadowVertices = Array((this.maxCurlSplits + 2) * 2)
            arrTempShadowVertices = Array((this.maxCurlSplits + 2) * 2)
            for (i in 0 until (this.maxCurlSplits + 2) * 2) arrTempShadowVertices!!.add(ShadowVertex())
        }
        // Rectangle consists of 4 vertices. Index 0 = top-left, index 1 =
// bottom-left, index 2 = top-right and index 3 = bottom-right.
        for (i in 0..3) rectangle[i] = Vertex()
        // Set up shadow penumbra direction to each vertex. We do fake 'self
// shadow' calculations based on this information.
        rectangle[3]!!.mPenumbraY = -1.0
        rectangle[1]!!.mPenumbraY = rectangle[3]!!.mPenumbraY
        rectangle[1]!!.mPenumbraX = rectangle[1]!!.mPenumbraY
        rectangle[0]!!.mPenumbraX = rectangle[1]!!.mPenumbraX
        rectangle[3]!!.mPenumbraX = 1.0
        rectangle[2]!!.mPenumbraY = rectangle[3]!!.mPenumbraX
        rectangle[2]!!.mPenumbraX = rectangle[2]!!.mPenumbraY
        rectangle[0]!!.mPenumbraY = rectangle[2]!!.mPenumbraX
        if (DRAW_CURL_POSITION) {
            curlPositionLinesCount = 3
            val hvbb = ByteBuffer.allocateDirect(curlPositionLinesCount * 2 * 2 * 4)
            hvbb.order(ByteOrder.nativeOrder())
            bufCurlPositionLines = hvbb.asFloatBuffer()
            bufCurlPositionLines!!.position(0)
        }
        // There are 4 vertices from bounding rect, max 2 from adding split line
// to two corners and curl consists of max maxCurlSplits lines each
// outputting 2 vertices.
        val maxVerticesCount = 4 + 2 + 2 * this.maxCurlSplits
        val vbb = ByteBuffer.allocateDirect(maxVerticesCount * 3 * 4)
        vbb.order(ByteOrder.nativeOrder())
        bufVertices = vbb.asFloatBuffer()
        bufVertices.position(0)
        if (DRAW_TEXTURE) {
            val tbb = ByteBuffer.allocateDirect(maxVerticesCount * 2 * 4)
            tbb.order(ByteOrder.nativeOrder())
            bufTexCoords = tbb.asFloatBuffer()
            bufTexCoords!!.position(0)
        }
        val cbb = ByteBuffer.allocateDirect(maxVerticesCount * 4 * 4)
        cbb.order(ByteOrder.nativeOrder())
        bufColors = cbb.asFloatBuffer()
        bufColors.position(0)
        if (DRAW_SHADOW) {
            val maxShadowVerticesCount = (this.maxCurlSplits + 2) * 2 * 2
            val scbb = ByteBuffer.allocateDirect(maxShadowVerticesCount * 4 * 4)
            scbb.order(ByteOrder.nativeOrder())
            bufShadowColors = scbb.asFloatBuffer()
            bufShadowColors!!.position(0)
            val sibb = ByteBuffer.allocateDirect(maxShadowVerticesCount * 3 * 4)
            sibb.order(ByteOrder.nativeOrder())
            bufShadowVertices = sibb.asFloatBuffer()
            bufShadowVertices!!.position(0)
            selfShadowCount = 0
            dropShadowCount = selfShadowCount
        }
    }
}