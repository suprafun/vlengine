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
import com.vlengine.util.FastList;

/**
 * <code>PickResults</code> contains information resulting from a pick test.
 * The results will contain a list of every node that was "struck" during a
 * pick test. Distance can be used to order the results. If <code>checkDistance</code> 
 * is set, objects will be ordered with the first element in the list being the
 * closest picked object.
 *
 * @author Mark Powell
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class PickResults {

    protected FastList<PickData> nodeList;

    protected Ray ray;
    protected float rayLength = Float.MAX_VALUE;
    
    protected boolean checkTriangles = false;
    protected boolean checkVolume = false;
    protected boolean gatherVolumePickPoints = false;
    
    /**
     * Constructor instantiates a new <code>PickResults</code> object.
     */
    public PickResults(Ray r) {
        nodeList = new FastList<PickData>();
        ray = r;
    }

    public void setCheckTriangles(boolean checkTriangles) {
        this.checkTriangles = checkTriangles;
    }
    
    public boolean isCheckTriangles() {
        return checkTriangles;
    }
    
    public void setCheckVolume(boolean checkVolume) {
        this.checkVolume = checkVolume;
    }
    
    public boolean isCheckVolume() {
        return checkVolume;
    }
    
    public void setVolumeGetPickPoints(boolean get) {
        gatherVolumePickPoints = get;
    }

    public boolean isVolumeGetPickPoints() {
        return gatherVolumePickPoints;
    }
   
    public void setRayLenght(float rl) {
        this.rayLength = rl;
    }
    
    public float getRayLength() {
        return rayLength;
    }
    
    public Ray getRay() {
        return ray;
    }
    
    /**
     * <code>getNumber</code> retrieves the number of geometries that have been
     * placed in the results.
     *
     * @return the number of Geometry objects in the list.
     */
    public int getNumber() {
        return nodeList.size();
    }

    /**
     * <code>getGeometry</code> retrieves a Geometry from a specific index.
     *
     * @param i the index requested.
     * @return the Geometry at the specified index.
     */
    public FastList<PickData> getPickData() {
        return nodeList;
    }

    /**
     * <code>clear</code> clears the list of all Geometry objects.
     */
    public void clear() {
        nodeList.clear();
    }
    
    /**
     * <code>addPick</code> generates an entry to be added to the list
     * of picked objects. If checkDistance is true, the implementing class
     * should order the object.
     * @param ray the ray that was cast for the pick calculation.
     * @param s the object to add to the pick data.
     */
    public void addPick(PickData s) {
        nodeList.add(s);
    }
}
