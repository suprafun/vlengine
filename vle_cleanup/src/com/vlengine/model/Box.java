/*
 * Copyright (c) 2003-2007 jMonkeyEngine, 2008 VL Engine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 * 
 * * Neither the name of 'VL Engine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.vlengine.model;

import com.vlengine.math.Vector3f;
import com.vlengine.util.geom.IndexBuffer;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import java.nio.FloatBuffer;


/**
 * <code>Box</code> provides an extension of <code>Geometry</code>. A
 * <code>Box</code> is defined by a minimal point and a maximum point. The
 * eight vertices that make the box are then computed. They are computed in such
 * a way as to generate an axis-aligned box.
 * 
 * @author Mark Powell
 * @author vear (Arpad Vekas) reworked for VL Engine
 */
public class Box extends Geometry {

    public float xExtent, yExtent, zExtent;

    public final Vector3f center = new Vector3f(0f, 0f, 0f);

    /**
     * instantiates a new <code>Box</code> object. All information must be
     * applies later. For internal usage only
     */
    public Box() {
            super();
    }

    /**
     * Constructor instantiates a new <code>Box</code> object. The minimum and
     * maximum point are provided. These two points define the shape and size of
     * the box, but not it's orientation or position. You should use the
     * <code>setLocalTranslation</code> and <code>setLocalRotation</code>
     * for those attributes.
     * 
     * @param name
     *            the name of the scene element. This is required for
     *            identification and comparision purposes.
     * @param min
     *            the minimum point that defines the box.
     * @param max
     *            the maximum point that defines the box.
     */
    public Box(Vector3f min, Vector3f max) {
            super();
            setData(min, max);
    }

    /**
     * Constructs a new box. The box has the given center and extends in the x,
     * y, and z out from the center (+ and -) by the given amounts. So, for
     * example, a box with extent of .5 would be the unit cube.
     * 
     * @param name
     *            Name of the box.
     * @param center
     *            Center of the box.
     * @param xExtent
     *            x extent of the box, in both directions.
     * @param yExtent
     *            y extent of the box, in both directions.
     * @param zExtent
     *            z extent of the box, in both directions.
     */
    public Box(Vector3f center, float xExtent, float yExtent,
                    float zExtent) {
            super();
            setData(center, xExtent, yExtent, zExtent);
    }

    /**
     * Changes the data of the box so that the two opposite corners are minPoint
     * and maxPoint. The other corners are created from those two poitns. If
     * update buffers is flagged as true, the vertex/normal/texture/color/index
     * buffers are updated when the data is changed.
     * 
     * @param minPoint
     *            The new minPoint of the box.
     * @param maxPoint
     *            The new maxPoint of the box.
     */
    public void setData(Vector3f minPoint, Vector3f maxPoint) {
            center.set(maxPoint).addLocal(minPoint).multLocal(0.5f);

            float x = maxPoint.x - center.x;
            float y = maxPoint.y - center.y;
            float z = maxPoint.z - center.z;
            setData(center, x, y, z);
    }

    /**
     * Changes the data of the box so that its center is <code>center</code>
     * and it extends in the x, y, and z directions by the given extent. Note
     * that the actual sides will be 2x the given extent values because the box
     * extends in + & - from the center for each extent.
     * 
     * @param center
     *            The center of the box.
     * @param xExtent
     *            x extent of the box, in both directions.
     * @param yExtent
     *            y extent of the box, in both directions.
     * @param zExtent
     *            z extent of the box, in both directions.
     */
    public void setData(Vector3f center, float xExtent, float yExtent,
                    float zExtent) {
            if (center != null)
                    this.center.set(center);

            this.xExtent = xExtent;
            this.yExtent = yExtent;
            this.zExtent = zExtent;

            setVertexData();
            setNormalData();
            setTextureData();
            setIndexData();

    }

    /**
     * 
     * <code>setVertexData</code> sets the vertex positions that define the
     * box. These eight points are determined from the minimum and maximum
     * point.
     *  
     */
    private void setVertexData() {
        this.setNumVertex(24);
        VertexBuffer vb = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_POSITION, getNumVertex());
        this.addAttribBuffer(vb, 0);
        FloatBuffer fb = vb.getDataBuffer();
        fb.clear();
            Vector3f[] vert = computeVertices(); // returns 8

            //Back
            fb.put(vert[0].x).put(vert[0].y).put(vert[0].z);
            fb.put(vert[1].x).put(vert[1].y).put(vert[1].z);
            fb.put(vert[2].x).put(vert[2].y).put(vert[2].z);
            fb.put(vert[3].x).put(vert[3].y).put(vert[3].z);

            //Right
            fb.put(vert[1].x).put(vert[1].y).put(vert[1].z);
            fb.put(vert[4].x).put(vert[4].y).put(vert[4].z);
            fb.put(vert[6].x).put(vert[6].y).put(vert[6].z);
            fb.put(vert[2].x).put(vert[2].y).put(vert[2].z);

            //Front
            fb.put(vert[4].x).put(vert[4].y).put(vert[4].z);
            fb.put(vert[5].x).put(vert[5].y).put(vert[5].z);
            fb.put(vert[7].x).put(vert[7].y).put(vert[7].z);
            fb.put(vert[6].x).put(vert[6].y).put(vert[6].z);

            //Left
            fb.put(vert[5].x).put(vert[5].y).put(vert[5].z);
            fb.put(vert[0].x).put(vert[0].y).put(vert[0].z);
            fb.put(vert[3].x).put(vert[3].y).put(vert[3].z);
            fb.put(vert[7].x).put(vert[7].y).put(vert[7].z);

            //Top
            fb.put(vert[2].x).put(vert[2].y).put(vert[2].z);
            fb.put(vert[6].x).put(vert[6].y).put(vert[6].z);
            fb.put(vert[7].x).put(vert[7].y).put(vert[7].z);
            fb.put(vert[3].x).put(vert[3].y).put(vert[3].z);

            //Bottom
            fb.put(vert[0].x).put(vert[0].y).put(vert[0].z);
            fb.put(vert[5].x).put(vert[5].y).put(vert[5].z);
            fb.put(vert[4].x).put(vert[4].y).put(vert[4].z);
            fb.put(vert[1].x).put(vert[1].y).put(vert[1].z);

    }

    /**
     * 
     * <code>setNormalData</code> sets the normals of each of the box's
     * planes.
     * 
     *  
     */
    private void setNormalData() {
        VertexBuffer vb = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_NORMAL, getNumVertex());
        this.addAttribBuffer(vb, 0);
        FloatBuffer fb = vb.getDataBuffer();
        fb.clear();

        //back
        for (int i = 0; i < 4; i++)
            fb.put(0).put(0).put(-1);

        //right
        for (int i = 0; i < 4; i++)
            fb.put(1).put(0).put(0);

        //front
        for (int i = 0; i < 4; i++)
            fb.put(0).put(0).put(1);

        //left
        for (int i = 0; i < 4; i++)
            fb.put(-1).put(0).put(0);

        //top
        for (int i = 0; i < 4; i++)
            fb.put(0).put(1).put(0);

        //bottom
        for (int i = 0; i < 4; i++)
            fb.put(0).put(-1).put(0);
    }

    /**
     * 
     * <code>setTextureData</code> sets the points that define the texture of
     * the box. It's a one-to-one ratio, where each plane of the box has it's
     * own copy of the texture. That is, the texture is repeated one time for
     * each six faces.
     *  
     */
    private void setTextureData() {
        VertexBuffer vb = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_TEXTURE0, getNumVertex());
        this.addAttribBuffer(vb, 0);
        FloatBuffer fb = vb.getDataBuffer();
        fb.clear();

        for (int i = 0; i < 6; i++) {
            fb.put(1).put(0);
            fb.put(0).put(0);
            fb.put(0).put(1);
            fb.put(1).put(1);
        }
    }

    /**
     * 
     * <code>setIndexData</code> sets the indices into the list of vertices,
     * defining all triangles that constitute the box.
     *  
     */
    private void setIndexData() {
        int[] indicesdata = { 2, 1, 0, 3, 2, 0, 6, 5, 4, 7, 6, 4, 10, 9, 8, 11, 10,
                        8, 14, 13, 12, 15, 14, 12, 18, 17, 16, 19, 18, 16, 22, 21, 20,
                        23, 22, 20 };
        IndexBuffer idx = IndexBuffer.createBuffer(indicesdata, getNumVertex(), null);
        this.setIndexBuffer(idx);
    }

    /**
     * 
     * @return a size 8 array of Vectors representing the 8 points of the box.
     */
    public Vector3f[] computeVertices() {

            Vector3f akEAxis[] = { Vector3f.UNIT_X.mult(xExtent), Vector3f.UNIT_Y.mult(yExtent),
                    Vector3f.UNIT_Z.mult(zExtent) };

            Vector3f rVal[] = new Vector3f[8];
            rVal[0] = center.subtract(akEAxis[0]).subtractLocal(akEAxis[1])
                            .subtractLocal(akEAxis[2]);
            rVal[1] = center.add(akEAxis[0]).subtractLocal(akEAxis[1])
                            .subtractLocal(akEAxis[2]);
            rVal[2] = center.add(akEAxis[0]).addLocal(akEAxis[1]).subtractLocal(
                            akEAxis[2]);
            rVal[3] = center.subtract(akEAxis[0]).addLocal(akEAxis[1])
                            .subtractLocal(akEAxis[2]);
            rVal[4] = center.add(akEAxis[0]).subtractLocal(akEAxis[1]).addLocal(
                            akEAxis[2]);
            rVal[5] = center.subtract(akEAxis[0]).subtractLocal(akEAxis[1])
                            .addLocal(akEAxis[2]);
            rVal[6] = center.add(akEAxis[0]).addLocal(akEAxis[1]).addLocal(
                            akEAxis[2]);
            rVal[7] = center.subtract(akEAxis[0]).addLocal(akEAxis[1]).addLocal(
                            akEAxis[2]);
            return rVal;
    }

    /**
     * Returns the current center of the box.
     * 
     * @return The box's center.
     */
    public Vector3f getCenter() {
            return center;
    }

    /**
     * Sets the center of the box. Note that even though the center is set,
     * Geometry information is not updated. In most cases, you'll want to use
     * setData()
     * 
     * @param aCenter
     *            The new center.
     */
    public void setCenter(Vector3f aCenter) {
            center.set(aCenter);
    }
}