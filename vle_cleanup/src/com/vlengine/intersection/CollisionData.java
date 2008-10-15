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

import com.vlengine.math.Vector3f;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.Mesh;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.thread.Context;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;


/**
 * CollisionData contains information about a collision between two TriMesh
 * objects. The mesh that was hit by the relevant TriMesh (the one making the
 * collision check) is referenced as well as an ArrayList for the triangles that
 * collided.
 * 
 * @author Mark Powell
 */
public class CollisionData {

    private Spatial targetMesh;

    private Spatial sourceMesh;

    private IntList sourceTris;

    private IntList targetTris;
    
    // no lod number here, collision is always checked against LOD 0
    private int targetBatchId;
    
    private int sourceBatchId;
    
    private FastList<Vector3f> collisionPoints;
    
    /**
     * instantiates a new CollisionData object.
     *
     * @param sourceMesh
     *            the relevant Geometry
     * @param targetMesh
     *            the mesh the relevant Geometry collided with.
     */
    public CollisionData(Spatial sourceMesh, Spatial targetMesh) {
        this.targetMesh = targetMesh;
        this.sourceMesh = sourceMesh;
        this.targetTris = null;
        this.sourceTris = null;
        this.sourceBatchId = -1;
        this.targetBatchId = -1;
    }

    /**
     * instantiates a new CollisionData object.
     *
     * @param sourceMesh
     *            the relevant Geometry
     * @param targetMesh
     *            the mesh the relevant Geometry collided with.
     */
    public CollisionData(Spatial sourceMesh, Spatial targetMesh, int sourceBatchId, int targetBatchId) {
        this(sourceMesh, targetMesh, sourceBatchId, targetBatchId, null, null);
    }

    public CollisionData(Spatial sourceMesh, Spatial targetMesh, int sourceBatchId, int targetBatchId, FastList<Vector3f> colPoints) {
        this(sourceMesh, targetMesh, sourceBatchId, targetBatchId, null, null);
        collisionPoints = colPoints;
    }
    
    /**
     * instantiates a new CollisionData object.
     *
     * @param sourceMesh
     *            the relevant Geometry
     * @param targetMesh
     *            the mesh the relevant Geometry collided with.
     * @param sourceTris
     *            the triangles of the relevant TriMesh that made contact.
     * @param targetTris
     *            the triangles of the second mesh that made contact.
     */
    public CollisionData(Spatial sourceMesh, Spatial targetMesh,
            int sourceBatchId, int targetBatchId, IntList sourceTris, IntList targetTris) {
        this.targetMesh = targetMesh;
        this.sourceMesh = sourceMesh;
        this.targetTris = targetTris;
        this.sourceTris = sourceTris;
        this.sourceBatchId = sourceBatchId;
        this.targetBatchId = targetBatchId;
    }

    
    /**
     * @return Returns the source mesh.
     */
    public Spatial getSourceMesh() {
        return sourceMesh;
    }

    public Spatial getTargetMesh() {
        return targetMesh;
    }
    
    public int getSourceBatchId() {
            return sourceBatchId;
    }
    
    public int getTargetBatchId() {
            return targetBatchId;
    }

    /**
     * @param mesh
     *            The mesh to set.
     */
    public void setSourceMesh(LodMesh mesh) {
        this.sourceMesh = mesh;
    }

    /**
     * 
     * <code>setTargetMesh</code> sets the mesh that is hit by the source
     * mesh.
     * 
     * @param mesh
     *            the mesh that was hit by the source mesh.
     */
    public void setTargetMesh(LodMesh mesh) {
        this.targetMesh = mesh;
    }

    /**
     * @return Returns the source.
     */
    public IntList getSourceTris() {
        return sourceTris;
    }

    /**
     * @param source
     *            The source to set.
     */
    public void setSourceTris(IntList source) {
        this.sourceTris = source;
    }

    /**
     * @return Returns the target.
     */
    public IntList getTargetTris() {
        return targetTris;
    }

    /**
     * @param target
     *            The target to set.
     */
    public void setTargetTris(IntList target) {
        this.targetTris = target;
    }
    
    /**
     * Returns the lowest Y coordinate where some collision occured
     * @return
     */
    public float getHighestTriangle() {
        float highest = Float.NEGATIVE_INFINITY;
        // get the batches
        TriBatch tb1 = null;
        if(sourceMesh instanceof Mesh) {
            tb1 = ((Mesh)sourceMesh).getBatch();
        } else if(sourceMesh instanceof LodMesh) {
            tb1 = ((LodMesh)sourceMesh).getBatch(0, sourceBatchId);
        }
        
        // get the world transforms
        Context tmp = LocalContext.getContext();
        final Vector3f[] verts = tmp.tempCollisonDataVerts;
                
        for (int i = 0, mi=sourceTris.size(); i < mi; i++) {
            tb1.getModel().getTriangle(sourceTris.get(i), verts);
            highest = verts[0].y;
            if(verts[1].y>highest)
                highest = verts[0].y;
            if(verts[2].y>highest)
                highest = verts[0].y;
            /*
            tri1.set(verts[0]);
            tri2.set(verts[1]);
            tri3.set(verts[2]);
             */
            /*
            roti.mult(tri1.set(verts[0]).multLocal(scalei), tri1).addLocal(transi);
            roti.mult(tri2.set(verts[1]).multLocal(scalei), tri2).addLocal(transi);
            roti.mult(tri3.set(verts[2]).multLocal(scalei), tri3).addLocal(transi);
             */
            // TODO: get the triangle center (this is a hack, it should be where the actual triangle collision was)
            /*
            triAvg.set(0, 0, 0);
            triAvg.addLocal(tri1);
            triAvg.addLocal(tri2);
            triAvg.addLocal(tri3);
            triAvg.multLocal(1f/3f);
            if(triAvg.y > highest) {
                highest = triAvg.y;
            }
             */
        }
        
        return highest;
    }
    
    public void setCollisionPoints(FastList<Vector3f> colpoints) {
        this.collisionPoints = colpoints;
    }
    
    public FastList<Vector3f> getCollisionPoints() {
        return collisionPoints;
    }
}