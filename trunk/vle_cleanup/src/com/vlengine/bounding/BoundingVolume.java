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

package com.vlengine.bounding;

import com.vlengine.intersection.IntersectionRecord;
import com.vlengine.math.Plane;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Ray;
import com.vlengine.math.Vector3f;
import com.vlengine.model.Geometry;
import com.vlengine.util.geom.VertexBuffer;
import java.nio.FloatBuffer;

/**
 *
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public abstract class BoundingVolume {
    
	public static final int BOUNDING_SPHERE = 0;
	public static final int BOUNDING_BOX = 1;
	public static final int BOUNDING_OBB = 2;
	public static final int BOUNDING_CAPSULE = 3;
        
    protected Vector3f center = new Vector3f();
    // not thread safe variable, used for storing the the most probable plane, which elminates
    // this object, while not thread-safe, this poses no problem, since if one Camera overwrites
    // this value, the worst that can happen to other Camera, is that it does not check for its
    // optimal plane first, but does later anyway
    protected int checkPlane = 0;
    
    public BoundingVolume() {
    }
    
    public BoundingVolume(Vector3f center) {
	    this.center.set(center);
    }
    
    /**
     * Grabs the checkplane we should check first.
     * 
     */
    public int getCheckPlane() {
        return checkPlane;
    }

    /**
     * Sets the index of the plane that should be first checked during rendering.
     * 
     * @param value
     */
    public final void setCheckPlane(int value) {
        checkPlane = value;
    }

    /**
     * getType returns the type of bounding volume this is. 
     */
    public abstract int getType();
        
    public final BoundingVolume transform(Quaternion rotate, Vector3f translate, Vector3f scale) {
        return transform(rotate, translate, scale, null);
    }
    
    public abstract BoundingVolume transform(Quaternion rotate, Vector3f translate, Vector3f scale, BoundingVolume store);
    
    public abstract int whichSide(Plane plane);

    public abstract void computeFromPoints(VertexBuffer attribBuffer, int startVertex, int numVertex);
    
    public abstract BoundingVolume merge(BoundingVolume volume);
    
    public abstract BoundingVolume mergeLocal(BoundingVolume volume);
    
    public abstract BoundingVolume clone(BoundingVolume store);
    
    public final Vector3f getCenter() {
        return center;
    }
    
    public final Vector3f getCenter(Vector3f store) {
        store.set(center);
        return store;
    }

    public final void setCenter(Vector3f newCenter) {
        center = newCenter;
    }
    
    public final float distanceTo(Vector3f point) {
        return center.distance(point);
    }
    
    public final float distanceSquaredTo(Vector3f point) {
        return center.distanceSquared(point);
    }
    
    public abstract float distanceToEdge(Vector3f point);
    
    public abstract boolean intersects(BoundingVolume bv);
    
    public abstract boolean intersects(Ray ray);
    
    public abstract IntersectionRecord intersectsWhere(Ray ray);
    
    /**
     * determines if this bounding volume and a given bounding box are
     * intersecting.
     * 
     * @param bb
     *            the bounding box to test against.
     * @return true if this volume intersects the given bounding box.
     */
    public abstract boolean intersectsBoundingBox(BoundingBox bb);
    
    public abstract boolean intersectsSphere(BoundingSphere bs);
    
    public abstract boolean contains(Vector3f point);
    
    public abstract void computeFromTris(Geometry batch, int start, int end);
        
    public abstract float getVolume();
}
