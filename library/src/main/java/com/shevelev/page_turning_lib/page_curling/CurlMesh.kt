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
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Class implementing actual curl/page rendering.
 * @param maxCurlSplits Maximum number curl can be divided into.
 * The bigger the value the smoother curl will be. With the cost of having more polygons for drawing.
 */
internal class CurlMesh(private val maxCurlSplits: Int) {
    // Colors for shadow. Inner one is the color drawn next to surface where
    // shadowed area starts and outer one is color shadow ends to.
    private val shadowInnerColor = floatArrayOf(0f, 0f, 0f, .5f)
    private val shadowOuterColor = floatArrayOf(0f, 0f, 0f, .0f)

    // For shadow rendering
    private val shadowVerticesNumber = (maxCurlSplits + 2) * 2
    private val dropShadowVertices = ArrayList<ShadowVertex>(shadowVerticesNumber)
    private val selfShadowVertices = ArrayList<ShadowVertex>(shadowVerticesNumber)
    private val tempShadowVertices = ArrayList<ShadowVertex>(shadowVerticesNumber)
    private var shadowColorsBuffer: FloatBuffer? = null
    private var shadowVerticesBuffer: FloatBuffer? = null
    private var dropShadowCount = 0
    private var selfShadowCount = 0

    private val intersections = ArrayList<Vertex>(INTERSECTIONS_NUMBER)
    private val outputVertices = ArrayList<Vertex>(OUTPUT_VERTICES_NUMBER)
    private val rotatedVertices = ArrayList<Vertex>(ROTATED_VERTICES_NUMBER)
    private val scanLines = ArrayList<Double>(maxCurlSplits + 2)
    private val tempVertices = ArrayList<Vertex>(TEMP_VERTICES_NUMBER)

    // Buffers for feeding rasterizer.
    private val colorsBuffer: FloatBuffer
    private var curlPositionLinesBuffer: FloatBuffer? = null
    private var texCoordinatesBuffer: FloatBuffer? = null
    private val verticesBuffer: FloatBuffer

    private val curlPositionLinesCount = 3

    // Boolean for 'flipping' texture sideways.
    private var flipTexture = false

    // Bounding rectangle for this mesh.
    private val rectangle = MeshRectangle()

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

    init {
        repeat(TEMP_VERTICES_NUMBER) {
            tempVertices.add(Vertex())
        }

        if (DEBUG_DRAW_CURL_POSITION) {
            curlPositionLinesBuffer = createFloatBuffer(curlPositionLinesCount * 2 * 2 * 4)
        }

        // There are 4 vertices from bounding rect, max 2 from adding split line
        // to two corners and curl consists of max maxCurlSplits lines each
        // outputting 2 vertices.
        val maxVerticesCount = 4 + 2 + 2 * this.maxCurlSplits
        verticesBuffer = createFloatBuffer(maxVerticesCount * 3 * 4)

        if (DEBUG_DRAW_TEXTURE) {
            texCoordinatesBuffer = createFloatBuffer(maxVerticesCount * 2 * 4)
        }

        colorsBuffer = createFloatBuffer(maxVerticesCount * 4 * 4)

        // Shadow parameters initialization
        repeat(shadowVerticesNumber) {
            tempShadowVertices.add(ShadowVertex())
        }

        val maxShadowVerticesCount = (this.maxCurlSplits + 2) * 2 * 2

        shadowColorsBuffer = createFloatBuffer(maxShadowVerticesCount * 4 * 4)
        shadowVerticesBuffer = createFloatBuffer(maxShadowVerticesCount * 3 * 4)

        selfShadowCount = 0
        dropShadowCount = selfShadowCount
    }

    /**
     * Sets curl for this mesh.
     * @param curlPos  Position for curl 'center'. Can be any point on line collinear to curl.
     * @param curlDir Curl direction, should be normalized.
     * @param radius Radius of curl.
     */
    @Synchronized
    fun curl(curlPos: PointF, curlDir: PointF, radius: Double) {
        if (DEBUG_DRAW_CURL_POSITION) { // First add some 'helper' lines used for development.
            curlPositionLinesBuffer?.apply {
                position(0)
                put(curlPos.x)
                put(curlPos.y - 1.0f)
                put(curlPos.x)
                put(curlPos.y + 1.0f)
                put(curlPos.x - 1.0f)
                put(curlPos.y)
                put(curlPos.x + 1.0f)
                put(curlPos.y)
                put(curlPos.x)
                put(curlPos.y)
                put(curlPos.x + curlDir.x * 2)
                put(curlPos.y + curlDir.y * 2)
                position(0)
            }
        }
        
        // Actual 'curl' implementation starts here.
        verticesBuffer.position(0)
        colorsBuffer.position(0)

        if (DEBUG_DRAW_TEXTURE) {
            texCoordinatesBuffer?.position(0)
        }

        // Calculate curl angle from direction.
        var curlAngle = acos(curlDir.x.toDouble())
        curlAngle = if (curlDir.y > 0) -curlAngle else curlAngle

        // Initiate rotated rectangle which's is translated to curlPos and
        // rotated so that curl direction heads to right (1,0). Vertices are
        // ordered in ascending order based on value1 -coordinate at the same time.
        // And using value2 -coordinate in very rare case in which two vertices have
        // same value1 -coordinate.
        tempVertices.addAll(rotatedVertices)
        rotatedVertices.clear()

        for (i in 0..3) {
            val v = tempVertices.removeAt(0)
            v.set(rectangle[i])
            v.translate(-curlPos.x.toDouble(), -curlPos.y.toDouble())
            v.rotateZ(-curlAngle)
            var j = 0

            while (j < rotatedVertices.size) {
                val v2 = rotatedVertices[j]

                if (v.posX > v2.posX) break
                if (v.posX == v2.posX && v.posY > v2.posY) break

                ++j
            }
            rotatedVertices.add(j, v)
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
            val v0 = rotatedVertices[0]
            val v2 = rotatedVertices[2]
            val v3 = rotatedVertices[3]

            val dist2 = sqrt((v0.posX - v2.posX)
                * (v0.posX - v2.posX) + (v0.posY - v2.posY)
                * (v0.posY - v2.posY))

            val dist3 = sqrt((v0.posX - v3.posX)
                * (v0.posX - v3.posX) + (v0.posY - v3.posY)
                * (v0.posY - v3.posY))

            if (dist2 > dist3) {
                lines[1][1] = 3
                lines[2][1] = 2
            }
        }

        verticesCountBack = 0
        verticesCountFront = verticesCountBack

        tempShadowVertices.addAll(dropShadowVertices)
        tempShadowVertices.addAll(selfShadowVertices)
        dropShadowVertices.clear()
        selfShadowVertices.clear()

        // Length of 'curl' curve.
        val curlLength = Math.PI * radius

        // Calculate scan lines.
        scanLines.clear()
        if (maxCurlSplits > 0) {
            scanLines.add(0.toDouble())
        }

        for (i in 1 until maxCurlSplits) {
            scanLines.add(-curlLength * i / (maxCurlSplits - 1))
        }

        // As mRotatedVertices is ordered regarding value1 -coordinate, adding
        // this scan line produces scan area picking up vertices which are
        // rotated completely. One could say 'until infinity'.
        scanLines.add(rotatedVertices[3].posX - 1)

        // Start from right most vertex. Pretty much the same as first scan area
        // is starting from 'infinity'.
        var scanXmax = rotatedVertices[0].posX + 1

        // Once we have scanXmin and scanXmax we have a scan area to start working with.
        for (i in 0 until scanLines.size) {
            val scanXmin = scanLines[i]
            // First iterate 'original' rectangle vertices within scan area.
            for (j in 0 until rotatedVertices.size) {
                val v = rotatedVertices[j]
                // Test if vertex lies within this scan area.
                if (v.posX in scanXmin..scanXmax) { // Pop out a vertex from temp vertices.
                    val n = tempVertices.removeAt(0)
                    n.set(v)

                    // This is done solely for triangulation reasons. Given a
                    // rotated rectangle it has max 2 vertices having
                    // intersection.
                    val intersections = getIntersections(rotatedVertices, lines, n.posX)

                    // In a sense one could say we're adding vertices always in
                    // two, positioned at the ends of intersecting line. And for
                    // triangulation to work properly they are added based on value2
                    // -coordinate. And this if-else is doing it for us.
                    when {
                        intersections.size == 1 && intersections[0].posY > v.posY -> {
                            // In case intersecting vertex is higher add it first.
                            outputVertices.addAll(intersections)
                            outputVertices.add(n)
                        }

                        intersections.size <= 1 -> {
                            // Otherwise add original vertex first.
                            outputVertices.add(n)
                            outputVertices.addAll(intersections)
                        }

                        else -> {
                            // There should never be more than 1 intersecting vertex.
                            // But if it happens as a fallback simply skip everything.
                            tempVertices.add(n)
                            tempVertices.addAll(intersections)
                        }
                    }
                }
            }

            // Search for scan line intersections.
            val intersections = getIntersections(rotatedVertices, lines, scanXmin)

            // We expect to get 0 or 2 vertices. In rare cases there's only one
            // but in general given a scan line intersecting rectangle there
            // should be 2 intersecting vertices.
            if (intersections.size == 2) {
                // There were two intersections, add them based on value2-coordinate, higher first, lower last.
                val v1 = intersections[0]
                val v2 = intersections[1]

                if (v1.posY < v2.posY) {
                    outputVertices.add(v2)
                    outputVertices.add(v1)
                } else {
                    outputVertices.addAll(intersections)
                }
            } else if (intersections.isNotEmpty()) {
                // This happens in a case in which there is a original vertex
                // exactly at scan line or something went very much wrong if
                // there are 3+ vertices. What ever the reason just return the
                // vertices to temp vertices for later use. In former case it
                // was handled already earlier once iterating through
                // mRotatedVertices, in latter case it's better to avoid doing
                // anything with them.
                tempVertices.addAll(intersections)
            }

            // Add vertices found during this iteration to vertex etc buffers.
            while (outputVertices.size > 0) {
                val v = outputVertices.removeAt(0)
                tempVertices.add(v)

                // Local texture front-facing flag.
                var textureFront: Boolean

                // Untouched vertices.
                when {
                    i == 0 -> {
                        textureFront = true
                        verticesCountFront++
                    }

                    i == scanLines.size - 1 || curlLength == 0.0 -> {
                        v.posX = -(curlLength + v.posX)
                        v.posZ = 2 * radius
                        v.penumbraX = -v.penumbraX
                        textureFront = false
                        verticesCountBack++
                    }

                    else -> {
                        // Even though it's not obvious from the if-else clause,
                        // here v.mPosX is between [-curlLength, 0]. And we can do
                        // calculations around a half cylinder.
                        val rotY = Math.PI * (v.posX / curlLength)
                        v.posX = radius * sin(rotY)
                        v.posZ = radius - radius * cos(rotY)
                        v.penumbraX *= cos(rotY)
                        // Map color multiplier to [.1f, 1f] range.
                        v.colorFactor = (.1f + .9f * sqrt(sin(rotY) + 1)).toFloat()
                        if (v.posZ >= radius) {
                            textureFront = false
                            verticesCountBack++
                        } else {
                            textureFront = true
                            verticesCountFront++
                        }
                    }
                }

                // We use local textureFront for flipping backside texture
                // locally. Plus additionally if mesh is in flip texture mode,
                // we'll make the procedure "backwards". Also, until this point,
                // texture coordinates are within [0, 1] range so we'll adjust
                // them to final texture coordinates too.
                if (textureFront != flipTexture) {
                    v.texX *= textureRectFront.right.toDouble()
                    v.texY *= textureRectFront.bottom.toDouble()
                    v.color = texturePage.getColor(PageSide.Front)
                } else {
                    v.texX *= textureRectBack.right.toDouble()
                    v.texY *= textureRectBack.bottom.toDouble()
                    v.color = texturePage.getColor(PageSide.Back)
                }

                // Move vertex back to 'world' coordinates.
                v.rotateZ(curlAngle)
                v.translate(curlPos.x.toDouble(), curlPos.y.toDouble())
                addVertex(v)

                // Drop shadow is cast 'behind' the curl.
                if (v.posZ > 0 && v.posZ <= radius) {
                    val sv = tempShadowVertices.removeAt(0)
                    sv.posX = v.posX
                    sv.posY = v.posY
                    sv.posZ = v.posZ
                    sv.penumbraX = v.posZ / 2 * -curlDir.x
                    sv.penumbraY = v.posZ / 2 * -curlDir.y
                    sv.penumbraColor = v.posZ / radius
                    val idx = (dropShadowVertices.size + 1) / 2
                    dropShadowVertices.add(idx, sv)
                }

                // Self shadow is cast partly over mesh.
                if (v.posZ > radius) {
                    val sv = tempShadowVertices.removeAt(0)
                    sv.posX = v.posX
                    sv.posY = v.posY
                    sv.posZ = v.posZ
                    sv.penumbraX = (v.posZ - radius) / 3 * v.penumbraX
                    sv.penumbraY = (v.posZ - radius) / 3 * v.penumbraY
                    sv.penumbraColor = (v.posZ - radius) / (2 * radius)
                    val idx = (selfShadowVertices.size + 1) / 2
                    selfShadowVertices.add(idx, sv)
                }
            }

            // Switch scanXmin as scanXmax for next iteration.
            scanXmax = scanXmin
        }

        verticesBuffer.position(0)
        colorsBuffer.position(0)
        if (DEBUG_DRAW_TEXTURE) {
            texCoordinatesBuffer!!.position(0)
        }

        // Add shadow Vertices.
        shadowColorsBuffer!!.position(0)
        shadowVerticesBuffer!!.position(0)
        dropShadowCount = 0
        for (i in 0 until dropShadowVertices.size) {
            val sv = dropShadowVertices[i]
            shadowVerticesBuffer!!.put(sv.posX.toFloat())
            shadowVerticesBuffer!!.put(sv.posY.toFloat())
            shadowVerticesBuffer!!.put(sv.posZ.toFloat())
            shadowVerticesBuffer!!.put((sv.posX + sv.penumbraX).toFloat())
            shadowVerticesBuffer!!.put((sv.posY + sv.penumbraY).toFloat())
            shadowVerticesBuffer!!.put(sv.posZ.toFloat())
            for (j in 0..3) {
                val color = shadowOuterColor[j] + (shadowInnerColor[j] - shadowOuterColor[j]) * sv.penumbraColor
                shadowColorsBuffer!!.put(color.toFloat())
            }
            shadowColorsBuffer!!.put(shadowOuterColor)
            dropShadowCount += 2
        }

        selfShadowCount = 0
        for (i in 0 until selfShadowVertices.size) {
            val sv = selfShadowVertices[i]
            shadowVerticesBuffer!!.put(sv.posX.toFloat())
            shadowVerticesBuffer!!.put(sv.posY.toFloat())
            shadowVerticesBuffer!!.put(sv.posZ.toFloat())
            shadowVerticesBuffer!!.put((sv.posX + sv.penumbraX).toFloat())
            shadowVerticesBuffer!!.put((sv.posY + sv.penumbraY).toFloat())
            shadowVerticesBuffer!!.put(sv.posZ.toFloat())
            for (j in 0..3) {
                val color = shadowOuterColor[j] + (shadowInnerColor[j] - shadowOuterColor[j]) * sv.penumbraColor
                shadowColorsBuffer!!.put(color.toFloat())
            }
            shadowColorsBuffer!!.put(shadowOuterColor)
            selfShadowCount += 2
        }
        shadowColorsBuffer!!.position(0)
        shadowVerticesBuffer!!.position(0)
    }

    /**
     * Renders our page curl mesh.
     */
    @Synchronized
    fun onDrawFrame(gl: GL10) {
        // First allocate texture if there is not one yet.
        if (DEBUG_DRAW_TEXTURE && textureIds == null) {
            // Generate texture.
            textureIds = IntArray(2)
            gl.glGenTextures(2, textureIds, 0)

            for (textureId in textureIds!!) {
                // Set texture attributes.
                gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId)
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat())
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST.toFloat())
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE.toFloat())
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE.toFloat())
            }
        }

        if (DEBUG_DRAW_TEXTURE && texturePage.texturesChanged) {
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds!![0])
            var texture = texturePage.getTexture(textureRectFront, PageSide.Front)
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, texture.bitmap, 0)
            texture.recycle()
            textureBack = texturePage.hasBackTexture
            if (textureBack) {
                gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds!![1])
                texture = texturePage.getTexture(textureRectBack, PageSide.Back)
                GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, texture.bitmap, 0)
                texture.recycle()
            } else textureRectBack.set(textureRectFront)
            texturePage.recycle()
            reset()
        }

        // Some 'global' settings.
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glDisable(GL10.GL_TEXTURE_2D)
        gl.glEnable(GL10.GL_BLEND)
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, shadowColorsBuffer)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, shadowVerticesBuffer)
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, dropShadowCount)
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY)
        gl.glDisable(GL10.GL_BLEND)

        if (DEBUG_DRAW_TEXTURE) {
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texCoordinatesBuffer)
        }
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, verticesBuffer)

        // Enable color array.
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorsBuffer)

        // Draw front facing blank vertices.
        gl.glDisable(GL10.GL_TEXTURE_2D)
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, verticesCountFront)

        // Draw front facing texture.
        if (DEBUG_DRAW_TEXTURE) {
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

        val backStartIdx = 0.coerceAtLeast(verticesCountFront - 2)
        val backCount = verticesCountFront + verticesCountBack - backStartIdx

        // Draw back facing blank vertices.
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, backStartIdx, backCount)

        // Draw back facing texture.
        if (DEBUG_DRAW_TEXTURE) {
            gl.glEnable(GL10.GL_BLEND)
            gl.glEnable(GL10.GL_TEXTURE_2D)

            if (flipTexture || !textureBack) {
                gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds!![0])
            } else {
                gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds!![1])
            }

            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, backStartIdx, backCount)
            gl.glDisable(GL10.GL_BLEND)
            gl.glDisable(GL10.GL_TEXTURE_2D)
        }

        // Disable textures and color array.
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY)

        if (DEBUG_DRAW_POLYGON_OUTLINES) {
            gl.glEnable(GL10.GL_BLEND)
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glLineWidth(1.0f)
            gl.glColor4f(0.5f, 0.5f, 1.0f, 1.0f)
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, verticesBuffer)
            gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, verticesCountFront)
            gl.glDisable(GL10.GL_BLEND)
        }

        if (DEBUG_DRAW_CURL_POSITION) {
            gl.glEnable(GL10.GL_BLEND)
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glLineWidth(1.0f)
            gl.glColor4f(1.0f, 0.5f, 0.5f, 1.0f)
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, curlPositionLinesBuffer)
            gl.glDrawArrays(GL10.GL_LINES, 0, curlPositionLinesCount * 2)
            gl.glDisable(GL10.GL_BLEND)
        }

        gl.glEnable(GL10.GL_BLEND)
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, shadowColorsBuffer)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, shadowVerticesBuffer)
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, dropShadowCount, selfShadowCount)
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY)
        gl.glDisable(GL10.GL_BLEND)

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
    }

    /**
     * Resets mesh to 'initial' state. Meaning this mesh will draw a plain
     * textured rectangle after call to this method.
     */
    @Synchronized
    fun reset() {
        verticesBuffer.position(0)
        colorsBuffer.position(0)
        if (DEBUG_DRAW_TEXTURE) texCoordinatesBuffer!!.position(0)

        for (i in 0..3) {
            val tmp = tempVertices[0]
            tmp.set(rectangle[i])

            if (flipTexture) {
                tmp.texX *= textureRectBack.right.toDouble()
                tmp.texY *= textureRectBack.bottom.toDouble()
                tmp.color = texturePage.getColor(PageSide.Back)
            } else {
                tmp.texX *= textureRectFront.right.toDouble()
                tmp.texY *= textureRectFront.bottom.toDouble()
                tmp.color = texturePage.getColor(PageSide.Front)
            }
            addVertex(tmp)
        }
        verticesCountFront = 4
        verticesCountBack = 0
        verticesBuffer.position(0)
        colorsBuffer.position(0)
        if (DEBUG_DRAW_TEXTURE) {
            texCoordinatesBuffer!!.position(0)
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
            rectangle.setTexCoordinates(1f, 0f, 0f, 1f)
        } else {
            rectangle.setTexCoordinates(0f, 0f, 1f, 1f)
        }
    }

    /**
     * Update mesh bounds.
     */
    fun setRect(r: RectF) = rectangle.update(r)

    private fun createFloatBuffer(size: Int): FloatBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(size).apply { order(ByteOrder.nativeOrder()) }
        return byteBuffer.asFloatBuffer().apply { position(0) }
    }

    /**
     * Adds vertex to buffers.
     */
    private fun addVertex(vertex: Vertex) {
        verticesBuffer.put(vertex.posX.toFloat())
        verticesBuffer.put(vertex.posY.toFloat())
        verticesBuffer.put(vertex.posZ.toFloat())

        colorsBuffer.put(vertex.colorFactor * Color.red(vertex.color) / 255f)
        colorsBuffer.put(vertex.colorFactor * Color.green(vertex.color) / 255f)
        colorsBuffer.put(vertex.colorFactor * Color.blue(vertex.color) / 255f)
        colorsBuffer.put(Color.alpha(vertex.color) / 255f)

        if (DEBUG_DRAW_TEXTURE) {
            texCoordinatesBuffer?.apply {
                put(vertex.texX.toFloat())
                put(vertex.texY.toFloat())
            }
        }
    }

    /**
     * Calculates intersections for given scan line.
     */
    private fun getIntersections(vertices: List<Vertex>, lineIndices: Array<IntArray>, scanX: Double): List<Vertex> {
        intersections.clear()

        // Iterate through rectangle lines each re-presented as a pair of vertices.
        for (j in lineIndices.indices) {
            val v1 = vertices[lineIndices[j][0]]
            val v2 = vertices[lineIndices[j][1]]

            // Here we expect that v1.mPosX >= v2.mPosX and wont do intersection test the opposite way.
            if (v1.posX > scanX && v2.posX < scanX) {
                // There is an intersection, calculate coefficient telling 'how far' scanX is from v2.
                val c = (scanX - v2.posX) / (v1.posX - v2.posX)
                val n = tempVertices.removeAt(0)
                n.set(v2)
                n.posX = scanX
                n.posY += (v1.posY - v2.posY) * c

                if (DEBUG_DRAW_TEXTURE) {
                    n.texX += (v1.texX - v2.texX) * c
                    n.texY += (v1.texY - v2.texY) * c
                }

                n.penumbraX += (v1.penumbraX - v2.penumbraX) * c
                n.penumbraY += (v1.penumbraY - v2.penumbraY) * c

                intersections.add(n)
            }
        }
        return intersections
    }

    companion object {
        // This flag shows curl position and one for the direction from the position given.
        private const val DEBUG_DRAW_CURL_POSITION = false

        // Flag for drawing polygon outlines.
        private const val DEBUG_DRAW_POLYGON_OUTLINES = false

        // Flag for texture rendering.
        private const val DEBUG_DRAW_TEXTURE = true

        private const val INTERSECTIONS_NUMBER = 2
        private const val OUTPUT_VERTICES_NUMBER = 7
        private const val ROTATED_VERTICES_NUMBER = 4
        private const val TEMP_VERTICES_NUMBER = OUTPUT_VERTICES_NUMBER + ROTATED_VERTICES_NUMBER
    }
}