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
 * @author Peter Andersson
 * @author Joshua Slack (Original sphere code that was adapted)
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class Dome extends Geometry {
    
private int planes;

    private int radialSamples;

    /** The radius of the dome */
    public float radius;

    /** The center of the dome */
    public Vector3f center;

    private static Vector3f tempVa = new Vector3f();

    private static Vector3f tempVb = new Vector3f();

    private static Vector3f tempVc = new Vector3f();

    public Dome() {}

    /**
     * Constructs a dome with center at the origin. For details, see the other
     * constructor.
     * 
     * @param planes
     *            The number of planes along the Z-axis.
     * @param radialSamples
     *            The samples along the radial.
     * @param radius
     *            Radius of the dome.
     * @see #Dome(java.lang.String, com.jme.math.Vector3f, int, int, float)
     */
    public Dome(int planes, int radialSamples, float radius) {
        this(new Vector3f(0, 0, 0), planes, radialSamples, radius);
    }

    /**
     * Constructs a dome. All geometry data buffers are updated automatically.
     * Both planes and radialSamples increase the quality of the generated dome.
     * 
     * @param center
     *            Center of the dome.
     * @param planes
     *            The number of planes along the Z-axis.
     * @param radialSamples
     *            The number of samples along the radial.
     * @param radius
     *            The radius of the dome.
     */
    public Dome(Vector3f center, int planes, int radialSamples,
            float radius) {

        setData(center, planes, radialSamples, radius, true, true);
    }

    /**
     * Constructs a dome. All geometry data buffers are updated automatically.
     * Both planes and radialSamples increase the quality of the generated dome.
     *
     * @param name          Name of the dome.
     * @param center        Center of the dome.
     * @param planes        The number of planes along the Z-axis.
     * @param radialSamples The number of samples along the radial.
     * @param radius        The radius of the dome.
     * @param outsideView   If true, the triangles will be connected for a view outside of
     *                      the dome.
     */
    public Dome( Vector3f center, int planes, int radialSamples,
                 float radius, boolean outsideView ) {
        super( );
        setData( center, planes, radialSamples, radius, true, outsideView );
    }

    /**
     * Changes the information of the dome into the given values. The boolean at
     * the end signals if buffer data should be updated as well. If the dome is
     * to be rendered, then that value should be true.
     * 
     * @param center
     *            The new center of the dome.
     * @param planes
     *            The number of planes along the Z-axis.
     * @param radialSamples
     *            The new number of radial samples of the dome.
     * @param radius
     *            The new radius of the dome.
     * @param updateBuffers
     *            If true, buffer information is updated as well.
     * @param outsideView
     *            If true, the triangles will be connected for a view outside of
     *            the dome.
     */
    public void setData(Vector3f center, int planes, int radialSamples,
            float radius, boolean updateBuffers, boolean outsideView) {
        if (center != null)
            this.center = center;
        else
            this.center = new Vector3f(0, 0, 0);
        this.planes = planes;
        this.radialSamples = radialSamples;
        this.radius = radius;

        if (updateBuffers) {
            setGeometryData(outsideView);
            setIndexData(outsideView);
        }
    }

    /**
     * Generates the vertices of the dome
     * 
     * @param outsideView
     *            If the dome should be viewed from the outside (if not zbuffer
     *            is used)
     */
    private void setGeometryData(boolean outsideView) {
        
        int vertCount = ((planes - 1) * (radialSamples + 1)) + 1;

        // allocate vertices, we need one extra in each radial to get the
        // correct texture coordinates

        VertexBuffer verts = new VertexBuffer();
        verts.setFormat(VertexFormat.getDefaultFormat(VertexFormat.setRequested(0,VertexAttribute.USAGE_POSITION)));
        // allocate vertices
        verts.setVertexCount(vertCount);
        verts.createDataBuffer();
        this.addAttribBuffer(verts, 0);
        FloatBuffer vertb = verts.getDataBuffer();

        VertexBuffer norms = new VertexBuffer();
        norms.setFormat(VertexFormat.getDefaultFormat(VertexFormat.setRequested(0,VertexAttribute.USAGE_NORMAL)));
        norms.setVertexCount(vertCount);
        norms.createDataBuffer();
        this.addAttribBuffer(norms, 0);
        FloatBuffer normb = norms.getDataBuffer();
        
        // allocate texture coordinates
        VertexBuffer texs = new VertexBuffer();
        texs.setFormat(VertexFormat.getDefaultFormat(VertexFormat.setRequested(0,VertexAttribute.USAGE_TEXTURE0)));
        texs.setVertexCount(vertCount);
        texs.createDataBuffer();
        this.addAttribBuffer(texs, 0);
        FloatBuffer texb = texs.getDataBuffer();

        // generate geometry
        float fInvRS = 1.0f / radialSamples;
        float fYFactor = 1.0f / (planes - 1);

        // Generate points on the unit circle to be used in computing the mesh
        // points on a dome slice.
        float[] afSin = new float[(radialSamples)];
        float[] afCos = new float[(radialSamples)];
        for (int iR = 0; iR < radialSamples; iR++) {
            float fAngle = FastMath.TWO_PI * fInvRS * iR;
            afCos[iR] = FastMath.cos(fAngle);
            afSin[iR] = FastMath.sin(fAngle);
        }

        // generate the dome itself
        int i = 0;
        for (int iY = 0; iY < (planes - 1); iY++) {
            float fYFraction = fYFactor * iY; // in (0,1)
            float fY = radius * fYFraction;
            // compute center of slice
            Vector3f kSliceCenter = tempVb.set(center);
            kSliceCenter.y += fY;

            // compute radius of slice
            float fSliceRadius = FastMath.sqrt(FastMath.abs(radius * radius - fY * fY));

            // compute slice vertices
            Vector3f kNormal;
            int iSave = i;
            for (int iR = 0; iR < radialSamples; iR++) {
                float fRadialFraction = iR * fInvRS; // in [0,1)
                Vector3f kRadial = tempVc.set(afCos[iR], 0, afSin[iR]);
                kRadial.mult(fSliceRadius, tempVa);
                vertb.put(kSliceCenter.x + tempVa.x).put(kSliceCenter.y + tempVa.y).put(kSliceCenter.z + tempVa.z);

                BufferUtils.populateFromBuffer(tempVa, vertb, i);
                kNormal = tempVa.subtractLocal(center);
                kNormal.normalizeLocal();
                if (outsideView)
                    normb.put(kNormal.x).put(kNormal.y).put(kNormal.z);
                else 
                	normb.put(-kNormal.x).put(-kNormal.y).put(-kNormal.z);

                texb.put(fRadialFraction).put(fYFraction);

                i++;
            }

            BufferUtils.copyInternalVector3(vertb, iSave, i);
            BufferUtils.copyInternalVector3(normb, iSave, i);

            texb.put(1.0f).put(fYFraction);

            i++;
        }

        // pole
        vertb.put(center.x).put(center.y+radius).put(center.z);

        if (outsideView)
        	normb.put(0).put(1).put(0);
        else
        	normb.put(0).put(-1).put(0);

        texb.put(0.5f).put(1.0f);
    }

    /**
     * Generates the connections
     * 
     * @param outsideView
     *            True if the dome should be viewed from the outside (if not
     *            using z buffer)
     */
    private void setIndexData(boolean outsideView) {
        

        // allocate connectivity
        int tricount = (planes - 2) * radialSamples * 2 + radialSamples;
        int vertcount = this.getAttribBuffer(VertexAttribute.USAGE_POSITION).getVertexCount();
        IndexBuffer idx = IndexBuffer.createBuffer(3 * tricount, vertcount, null);
        setIndexBuffer(idx);

        // generate connectivity
        int index = 0;
        // Generate only for middle planes
        for (int plane = 1; plane < (planes - 1); plane++) {
            int bottomPlaneStart = (plane - 1) * (radialSamples + 1);
            int topPlaneStart = plane * (radialSamples + 1);
            for (int sample = 0; sample < radialSamples; sample++, index += 6) {
                if (outsideView) {
                	idx.put(bottomPlaneStart + sample);
                	idx.put(bottomPlaneStart + sample + 1);
                	idx.put(topPlaneStart + sample);
                	idx.put(bottomPlaneStart + sample + 1);
                	idx.put(topPlaneStart + sample + 1);
                	idx.put(topPlaneStart + sample);
                } else // inside view
                {
                	idx.put(bottomPlaneStart + sample);
                	idx.put(topPlaneStart + sample);
                	idx.put(bottomPlaneStart + sample + 1);
                	idx.put(bottomPlaneStart + sample + 1);
                	idx.put(topPlaneStart + sample);
                	idx.put(topPlaneStart + sample + 1);
                }
            }
        }

        // pole triangles
        int bottomPlaneStart = (planes - 2) * (radialSamples + 1);
        for (int samples = 0; samples < radialSamples; samples++, index += 3) {
            if (outsideView) {
            	idx.put(bottomPlaneStart + samples);
            	idx.put(bottomPlaneStart + samples + 1);
            	idx.put(vertcount - 1);
            } else // inside view 
            {
            	idx.put(bottomPlaneStart + samples);
            	idx.put(vertcount - 1);
                idx.put(bottomPlaneStart + samples + 1);
            }
        }
    }

}


