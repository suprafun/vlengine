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

import com.vlengine.math.FastMath;
import com.vlengine.math.Vector3f;
import com.vlengine.util.geom.BufferUtils;
import com.vlengine.util.geom.IndexBuffer;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import com.vlengine.util.geom.VertexFormat;
import java.nio.FloatBuffer;

/**
 * A <code>Cylinder</code> is defined by a height and radius. The center of the
 * Cylinder is the origin.
 * 
 * @author Mark Powell
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class Cylinder extends Geometry {

    private int axisSamples;

    private int radialSamples;

    private float radius;
    private float radius2;

    private float height;
    private boolean closed;

    public Cylinder() {}

/**
     * Creates a new Cylinder. By default its center is the origin. Usually, a
     * higher sample number creates a better looking cylinder, but at the cost
     * of more vertex information.
     * 
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     */
    public Cylinder(int axisSamples, int radialSamples,
                    float radius, float height) {
        this( axisSamples, radialSamples, radius, height, false );
    }

    /**
     * Creates a new Cylinder. By default its center is the origin. Usually, a
     * higher sample number creates a better looking cylinder, but at the cost
     * of more vertex information.
     * <br>
     * If the cylinder is closed the texture is split into axisSamples parts: top most and bottom most part is used for
     * top and bottom of the cylinder, rest of the texture for the cylinder wall. The middle of the top is mapped to
     * texture coordinates (0.5, 1), bottom to (0.5, 0). Thus you need a suited distorted texture.
     *
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     * @param closed
     *            true to create a cylinder with top and bottom surface
     */
    public Cylinder(int axisSamples, int radialSamples,
                    float radius, float height, boolean closed ) {

        super();

        this.axisSamples = axisSamples + (closed ? 2 : 0);
        this.radialSamples = radialSamples;
        setRadius( radius );
        this.height = height;
        this.closed = closed;

        allocateVertices();
    }

    /**
     * @return Returns the height.
     */
    public float getHeight() {
        return height;
    }

    /**
     * @param height
     *            The height to set.
     */
    public void setHeight(float height) {
        this.height = height;
        allocateVertices();
    }

    /**
     * @return Returns the radius.
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Change the radius of this cylinder. This resets any second radius.
     * @param radius
     *            The radius to set.
     */
    public void setRadius(float radius) {
        this.radius = radius;
        this.radius2 = radius;
        allocateVertices();
    }

    /**
     * Set the top radius of the 'cylinder' to differ from the bottom radius.
     * @param radius
     *            The first radius to set.
     * @see Cone
     */
    public void setRadius1(float radius) {
        this.radius = radius;
        allocateVertices();
    }

    /**
     * Set the bottom radius of the 'cylinder' to differ from the top radius. This makes the Geometry be a frustum of
     * pyramid, or if set to 0, a cone.
     * @param radius
     *            The second radius to set.
     * @see Cone
     */
    public void setRadius2(float radius) {
        this.radius2 = radius;
        allocateVertices();
    }

    private void allocateVertices() {
        
        // number of vertices
        int vertCount = axisSamples * (radialSamples + 1) + (closed ? 2 : 0);
        VertexBuffer verts = new VertexBuffer();
        verts.setFormat(VertexFormat.getDefaultFormat(VertexFormat.setRequested(0,VertexAttribute.USAGE_POSITION)));
        // allocate vertices
        verts.setVertexCount(vertCount);
        verts.createDataBuffer();
        this.addAttribBuffer(verts, 0);

        VertexBuffer norms = new VertexBuffer();
        norms.setFormat(VertexFormat.getDefaultFormat(VertexFormat.setRequested(0,VertexAttribute.USAGE_NORMAL)));
        norms.setVertexCount(vertCount);
        norms.createDataBuffer();
        this.addAttribBuffer(norms, 0);
        
        // allocate texture coordinates
        VertexBuffer texs = new VertexBuffer();
        texs.setFormat(VertexFormat.getDefaultFormat(VertexFormat.setRequested(0,VertexAttribute.USAGE_TEXTURE0)));
        texs.setVertexCount(vertCount);
        texs.createDataBuffer();
        this.addAttribBuffer(texs, 0);
        
        int tricount = ((closed ? 2 : 0) + 2 * (axisSamples - 1) ) * radialSamples;
        IndexBuffer idx = IndexBuffer.createBuffer(3 * tricount, vertCount, null);
        this.setIndexBuffer(idx);

        setGeometryData();
        setIndexData();
    }

    private void setGeometryData() {
        

        // generate geometry
        float inverseRadial = 1.0f / radialSamples;
        float inverseAxisLess = 1.0f / (closed ? axisSamples - 3 : axisSamples - 1);
        float inverseAxisLessTexture = 1.0f / (axisSamples - 1);
        float halfHeight = 0.5f * height;

        // Generate points on the unit circle to be used in computing the mesh
        // points on a cylinder slice.
        float[] sin = new float[radialSamples + 1];
        float[] cos = new float[radialSamples + 1];

        for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
            float angle = FastMath.TWO_PI * inverseRadial * radialCount;
            cos[radialCount] = FastMath.cos(angle);
            sin[radialCount] = FastMath.sin(angle);
        }
        sin[radialSamples] = sin[0];
        cos[radialSamples] = cos[0];

        FloatBuffer vertbuf = this.getAttribBuffer(VertexAttribute.USAGE_POSITION).getDataBuffer();
        FloatBuffer normbuf = this.getAttribBuffer(VertexAttribute.USAGE_NORMAL).getDataBuffer();
        FloatBuffer texbuf = this.getAttribBuffer(VertexAttribute.USAGE_TEXTURE0).getDataBuffer();
        
        // generate the cylinder itself
        Vector3f tempNormal = new Vector3f();
        for (int axisCount = 0, i = 0; axisCount < axisSamples; axisCount++) {
            float axisFraction;
            float axisFractionTexture;
            int topBottom = 0;
            if ( !closed ) {
                axisFraction = axisCount * inverseAxisLess; // in [0,1]
                axisFractionTexture = axisFraction;
            } else {
                if ( axisCount == 0 ) {
                    topBottom = -1; // bottom
                    axisFraction = 0;
                    axisFractionTexture = inverseAxisLessTexture;
                } else if ( axisCount == axisSamples-1 ) {
                    topBottom = 1; // top
                    axisFraction = 1;
                    axisFractionTexture = 1 - inverseAxisLessTexture;
                } else {
                    axisFraction = (axisCount-1)*inverseAxisLess;
                    axisFractionTexture = axisCount * inverseAxisLessTexture;
                }
            }
            float z = -halfHeight + height * axisFraction;

            // compute center of slice
            Vector3f sliceCenter = new Vector3f(0, 0, z);

            // compute slice vertices with duplication at end point
            int save = i;
            for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
                float radialFraction = radialCount * inverseRadial; // in [0,1)
                tempNormal.set(cos[radialCount], sin[radialCount], 0);
                if ( topBottom == 0 ) {
                    if (true) normbuf.put(tempNormal.x).put(tempNormal.y).put(tempNormal.z);
                    else normbuf.put(-tempNormal.x).put(-tempNormal.y).put(-tempNormal.z);
                } else {
                	normbuf.put( 0 ).put( 0 ).put( 1 );
                }

                tempNormal.multLocal( ( radius - radius2 ) * axisFraction + radius2 ).addLocal( sliceCenter );
                vertbuf.put(tempNormal.x).put(tempNormal.y).put(tempNormal.z);

                texbuf.put(radialFraction).put(axisFractionTexture);
                i++;
            }

            BufferUtils.copyInternalVector3(vertbuf, save, i);
            BufferUtils.copyInternalVector3(vertbuf, save, i);

            texbuf.put(1.0f).put(axisFractionTexture);

            i++;
        }

        if ( closed ) {
        	vertbuf.put( 0 ).put( 0 ).put( -halfHeight ); // bottom center
            normbuf.put( 0 ).put( 0 ).put( 1 );
            texbuf.put(0.5f).put(0);
            vertbuf.put( 0 ).put( 0 ).put( halfHeight ); // top center
            normbuf.put( 0 ).put( 0 ).put( 1 );
            texbuf.put(0.5f).put(1);
        }
    }

    private void setIndexData() {
        IndexBuffer idx = this.getIndexBuffer();

        int vertcount = this.getAttribBuffer(VertexAttribute.USAGE_POSITION).getVertexCount();
        
        // generate connectivity
        for (int axisCount = 0, axisStart = 0; axisCount < axisSamples - 1; axisCount++) {
            int i0 = axisStart;
            int i1 = i0 + 1;
            axisStart += radialSamples + 1;
            int i2 = axisStart;
            int i3 = i2 + 1;
            for (int i = 0; i < radialSamples; i++) {
                if ( closed && axisCount == 0 ) {
                	idx.put( i0++ );
                	idx.put( i1++ );
                	idx.put( vertcount - 2 );
                }
                else if ( closed && axisCount == axisSamples - 2 ) {
                	idx.put( i2++ );
                	idx.put( i3++ );
                	idx.put( vertcount - 1 );
                } else {
                    if (true) {
                    	idx.put(i0++);
                    	idx.put(i1);
                    	idx.put(i2);
                    	idx.put(i1++);
                    	idx.put(i3++);
                    	idx.put(i2++);
                    } else {
                    	idx.put(i0++);
                    	idx.put(i2);
                    	idx.put(i1);
                    	idx.put(i1++);
                    	idx.put(i2++);
                    	idx.put(i3++);
                    }
                }
            }
        }
    }
}



