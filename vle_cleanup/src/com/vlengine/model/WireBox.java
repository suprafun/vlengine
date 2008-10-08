/*
 * Copyright (c) 2008 VL Engine
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
 * Represents a wire box, composod of lines
 * @author vear (Arpad Vekas)
 */
public class WireBox extends Geometry {

    public float xExtent, yExtent, zExtent;

    public final Vector3f center = new Vector3f(0f, 0f, 0f);
    
    public WireBox() {
            super();
    }
    
    public WireBox(Vector3f min, Vector3f max) {
            super();
            setData(min, max);
    }
    
    public WireBox(Vector3f center, float xExtent, float yExtent, float zExtent) {
            super();
            setData(center, xExtent, yExtent, zExtent);
    }
    
    public void setData(Vector3f minPoint, Vector3f maxPoint) {
        center.set(maxPoint).addLocal(minPoint).multLocal(0.5f);

        float x = maxPoint.x - center.x;
        float y = maxPoint.y - center.y;
        float z = maxPoint.z - center.z;
        setData(center, x, y, z);
    }
    
    public void setData(Vector3f center, float xExtent, float yExtent, float zExtent) {
        if (center != null)
                this.center.set(center);

        this.xExtent = xExtent;
        this.yExtent = yExtent;
        this.zExtent = zExtent;

        setVertexData();
        setIndexData();
    }

    private void setVertexData() {
        this.setNumVertex(8);
        VertexBuffer vb=this.getAttribBuffer(VertexAttribute.USAGE_POSITION);
        if(vb==null) {
            vb = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_POSITION, getNumVertex());
            this.addAttribBuffer(vb, 0);
        }
        FloatBuffer fb = vb.getDataBuffer();
        fb.clear();
        Vector3f[] vert = computeVertices(); // returns 8

        fb.put(vert[0].x).put(vert[0].y).put(vert[0].z);
        fb.put(vert[1].x).put(vert[1].y).put(vert[1].z);
        fb.put(vert[2].x).put(vert[2].y).put(vert[2].z);
        fb.put(vert[3].x).put(vert[3].y).put(vert[3].z);
        fb.put(vert[4].x).put(vert[4].y).put(vert[4].z);
        fb.put(vert[5].x).put(vert[5].y).put(vert[5].z);
        fb.put(vert[6].x).put(vert[6].y).put(vert[6].z);
        fb.put(vert[7].x).put(vert[7].y).put(vert[7].z);
        fb.rewind();
    }
    
    public Vector3f[] computeVertices() {
        Vector3f akEAxis[] = { Vector3f.UNIT_X.mult(xExtent), Vector3f.UNIT_Y.mult(yExtent),
                Vector3f.UNIT_Z.mult(zExtent) };

        Vector3f rVal[] = new Vector3f[8];
        // left, bottom, front
        rVal[0] = center.subtract(akEAxis[0]).subtractLocal(akEAxis[1])
                        .subtractLocal(akEAxis[2]);
        // right, bottom, front
        rVal[1] = center.add(akEAxis[0]).subtractLocal(akEAxis[1])
                        .subtractLocal(akEAxis[2]);
        // right, top, front
        rVal[2] = center.add(akEAxis[0]).addLocal(akEAxis[1]).subtractLocal(
                        akEAxis[2]);
        // left, top, front
        rVal[3] = center.subtract(akEAxis[0]).addLocal(akEAxis[1])
                        .subtractLocal(akEAxis[2]);
        // right, bottom, back
        rVal[4] = center.add(akEAxis[0]).subtractLocal(akEAxis[1]).addLocal(
                        akEAxis[2]);
        // left, bottom, back
        rVal[5] = center.subtract(akEAxis[0]).subtractLocal(akEAxis[1])
                        .addLocal(akEAxis[2]);
        // right, top, back
        rVal[6] = center.add(akEAxis[0]).addLocal(akEAxis[1]).addLocal(
                        akEAxis[2]);
        // left, top, back
        rVal[7] = center.subtract(akEAxis[0]).addLocal(akEAxis[1]).addLocal(
                        akEAxis[2]);
        return rVal;
    }
    
    private void setIndexData() {
        this.setMode(LINE_SEGMENTS);
        int[] indicesdata = { 
            // front
            0, 1, 1, 2, 2, 3, 3, 0,
            // left
            0, 5, 7, 3,
            // front
            4, 5, 5, 7, 7, 6, 6, 4,
            // right
            1, 4, 2, 6
        };
        
        IndexBuffer idx = getIndexBuffer();
        if(idx==null) {
            idx = IndexBuffer.createBuffer(indicesdata, getNumVertex(), idx);
            this.setIndexBuffer(idx);
        }
    }
    
}
