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

package com.vlengine.intersection;

import com.vlengine.math.Ray;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.Mesh;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.thread.Context;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;

/**
 * 
 * PickData contains information about a picking operation (or Ray/Volume
 * intersection). This data contains the mesh the ray hit, the triangles it hit,
 * and the ray itself.
 * 
 * @author Mark Powell
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class PickData {

    protected Spatial targetMesh;

    protected IntList targetTris;
    
    // no lod number here, collision is always checked against LOD 0
    protected int targetBatchId;
    
    protected float distance;
    
    protected FastList<Vector3f> collisionPoints;
    
    /**
     * instantiates a new PickData object.
     */
    public PickData(Spatial targetMesh) {
        this.targetMesh = targetMesh;
        this.targetBatchId = -1;
        this.targetTris = null;
    }

    public PickData(Spatial targetMesh, int targetBatch) {
        this.targetMesh = targetMesh;
        this.targetBatchId = targetBatch;
        this.targetTris = null;
    }

    public PickData(Spatial targetMesh, int targetBatch, IntList targetTris) {
        this.targetMesh = targetMesh;
        this.targetBatchId = targetBatch;
        this.targetTris = targetTris;
    }
    
    /**
     * 
     * <code>getTargetMesh</code> returns the geometry that was hit by the
     * ray.
     * 
     * @return the geometry hit by the ray.
     */
    public Spatial getTargetMesh() {
        return targetMesh;
    }

    public int getTargetBatchId() {
        return targetBatchId;
    }
    
    public IntList getTargetTris() {
        return targetTris;
    }
    
    /**
     * 
     * <code>setTargetMesh</code> sets the geometry hit by the ray.
     * 
     * @param mesh
     *            the geometry hit by the ray.
     */
    public void setTargetMesh(Spatial mesh) {
        this.targetMesh = mesh;
    }

    public FastList<Vector3f> getCollisionPoints() {
        return collisionPoints;
    }
    
    public void setCollisionPoints(FastList<Vector3f> cp) {
        this.collisionPoints = cp;
    }
    
    public float getDistance() {
        return distance;
    }
    
    public void setDistance(float distance) {
        this.distance = distance;
    }

    public void getNearestTriangleIntersectionPoint(Ray ray, Vector3f store) {
        TriBatch tb1 = null;
        if(targetMesh instanceof Mesh) {
            tb1 = ((Mesh)targetMesh).getBatch();
        } else if(targetMesh instanceof LodMesh) {
            tb1 = ((LodMesh)targetMesh).getBatch(0, targetBatchId);
        }
        BaseGeometry batch=tb1.getModel();
        
        Context tmp = LocalContext.getContext();
        final Vector3f[] verts = tmp.ctverts;
        Vector3f vec = tmp._compVect1;
        Vector3f vec1 = tmp._compVect2;
        float minLen = Float.MAX_VALUE;
        for (int i = 0; i < targetTris.size(); i++) {
                batch.getTriangle(targetTris.get(i), verts);
                targetMesh.localToWorld( verts[0], tmp.tempVa );
                targetMesh.localToWorld( verts[1], tmp.tempVb );
                targetMesh.localToWorld( verts[2], tmp.tempVc );
                if (ray.intersectWhere(tmp.tempVa, tmp.tempVb, tmp.tempVc, vec)) {
                        vec1.set(vec).subtractLocal(ray.getOrigin());
                        float len = vec1.lengthSquared();
                        if(len < minLen) {
                            minLen = len;
                            store.set(vec);
                        }
                }
        }
    }
}