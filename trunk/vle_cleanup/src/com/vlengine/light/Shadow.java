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

package com.vlengine.light;

import com.vlengine.renderer.ViewCamera;
import com.vlengine.scene.batch.LightBatch;
import com.vlengine.system.VleException;
import com.vlengine.util.FastList;
import com.vlengine.util.IntMap;

/**
 * The shadow object is referenced in LightBatch, it holds the shadowmap
 * information.
 * @author vear (Arpad Vekas)
 */
public class Shadow {
    // light that is casting this shadow
    protected LightBatch parent;
    
    // perspective shadow map generation?
    protected boolean perspective = false;

    // perspective shadow map generation type (parts of the main camera frustrum are shadowed)
    // the number of frustum splits used for shadowmaps
    protected int pSplits;
    // the parts of the shadow for perspective mapping
    protected FastList<ShadowPart> pShadows;
    // the dimension of the shadowmap textrure that is the closest to the main camera
    // each split after has half the dimension
    protected int pDimension = 1024;
    
    // scene shadowmap type generation (parts of the scene are shadowed regardless of the current view)
    // the dimension (in world space) of the area for each of the 
    protected float sExtent = 1024;
    // the dimension of the shadow map texture
    protected int sDimension = 256;
    // the map of shadowparts based on their position
    // the block id is calculated this way: 
    // the position is divided by sExtent
    // the x and z values are added 4096, y is added 128
    // the block id is constructed: 13 bit x, 13 bit z, 8 bit y
    protected IntMap sShadows;
    
    public Shadow(boolean perspective) {
        this.perspective = perspective;
        if(perspective) {
            pShadows = new FastList<ShadowPart>();
        } else {
            sShadows = new IntMap();
        }
    }
    
    public LightBatch getParent() {
        return parent;
    }
    
    public void setParent(LightBatch l) {
        if(parent != null)
            throw new VleException("Shadow already has a parent light, cannot set new parent");
        parent = l;
        
    }
    
    public void setPerspectiveSplits(int split) {
        pSplits = split;
        pShadows.ensureCapacity(pSplits);
        for(int i=0; i<pSplits; i++) {
            ShadowPart sp = pShadows.get(i);
            if(sp==null) {
                sp = new ShadowPart();
                sp.setParent(this);
                pShadows.set(i, sp);
            }
        }
    }
    
    public FastList<ShadowPart> getSplits() {
        return pShadows;
    }
    
    public ShadowPart getPerspectiveSplit(int i) {
        return pShadows.get(i);
    }
    
    public boolean isPerspective() {
        return this.perspective;
    }
    
    public FastList<ShadowPart> getSceneShadows(ViewCamera cam, FastList<ShadowPart> store) {
        if(store == null) {
            store=new FastList<ShadowPart>();
        } else {
            store.clear();
        }
        
        return store;
    }
}
