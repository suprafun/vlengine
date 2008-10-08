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

/**
 * IntersectionRecord stores needed information for a interesection query between
 * two objects. This includes all points that were intersected, and the
 * distances between these points. Therefore, a 1 to 1 ratio between the distance
 * array and the point array is enforced.
 *
 */

/*
 * Rewritten to re-use the same arrays, and not create garbage, this means that
 * multiple IntersectionRecord-s cannot be stored, it should be evaluated on-by-one
 * as they are returned from intersection checks.
 */
public class IntersectionRecord {

    private float[] distances;
    private Vector3f[] points;
    private int length=0;
    
    private static final int INITIAL_LENGTH = 2;
    
    /**
     * Instantiates a new IntersectionRecord with no distances or points assigned.
     *
     */
    public IntersectionRecord() {
        resize(INITIAL_LENGTH);
        length=0;
    }

    /**
     * Instantiates a new IntersectionRecord defining the distances and points. 
     * If the size of the distance and point arrays do not match, an exception
     * is thrown.
     * @param distances the distances of this intersection.
     * @param points the points of this intersection.
     */
    public IntersectionRecord(int capacity) {
        resize(capacity);
    }
    
    public float[] getDistances(int capacity) {
        if(capacity > length)
            resize(capacity);
        return distances;
    }
    
    public Vector3f[] getPoints(int capacity) {
        if(capacity > length)
            resize(capacity);
        return points;
    }

    protected void resize(int capacity) {
        if(points != null && capacity <= points.length) {
            length = capacity;
            return;
        }
            
        float[] newdistances = new float[capacity];
        Vector3f[] newpoints = new Vector3f[capacity];
        if(distances!=null)
            System.arraycopy(distances, 0, newdistances, 0, distances.length);
        int len = length;
        if(points!=null) {
            len = points.length;
            System.arraycopy(points, 0, newpoints, 0, points.length);
        }
        // create new point vectors
        for(int i=len; i < capacity; i++ )
            newpoints[i] = new Vector3f();
        points = newpoints;
        distances = newdistances;        
        length = capacity;
    }
    
    /**
     * Returns the number of intersections that occured.
     * @return the number of intersections that occured.
     */
    public int getQuantity() {
        return length;
    }

    /**
     * Returns an intersection point at a provided index.
     * @param index the index of the point to obtain.
     * @return the point at the index of the array.
     */
    public Vector3f getIntersectionPoint(int index) {
        return points[index];
    }

    /**
     * Returns an intersection distance at a provided index.
     * @param index the index of the distance to obtain.
     * @return the distance at the index of the array.
     */
    public float getIntersectionDistance(int index) {
        return distances[index];
    }

    /**
     * Returns the smallest distance in the distance array.
     * @return the smallest distance in the distance array.
     */
    public float getClosestDistance() {
        float min = Float.MAX_VALUE;
        if (distances != null) {
            for (int i = length; --i >= 0;) {
                float val = distances[i];
                if (val < min) {
                    min = val;
                }
            }
        }
        return min;
    }

    /**
     * Returns the point that has the smallest associated distance value.
     * @return the point that has the smallest associated distance value.
     */
    public int getClosestPoint() {
        float min = Float.MAX_VALUE;
        int point = 0;
        if (distances != null) {
            for (int i = length; --i >= 0;) {
                float val = distances[i];
                if (val < min) {
                    min = val;
                    point = i;
                }
            }
        }
        return point;
    }

    /**
     * Returns the point that has the largest associated distance value.
     * @return the point that has the largest associated distance value.
     */
    public int getFarthestPoint() {
        float max = Float.MIN_VALUE;
        int point = 0;
        if (distances != null) {
            for (int i = length; --i >= 0;) {
                float val = distances[i];
                if (val > max) {
                    max = val;
                    point = i;
                }
            }
        }
        return point;
    }

}
