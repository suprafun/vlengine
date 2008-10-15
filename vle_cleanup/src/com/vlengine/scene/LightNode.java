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

package com.vlengine.scene;

import com.vlengine.bounding.BoundingVolume;
import com.vlengine.light.Light;
import com.vlengine.scene.batch.LightBatch;

/**
 * <code>LightNode</code> defines a scene node that contains and maintains a
 * light object. A light node contains a single light, and positions the light
 * based on it's translation vector. If the contained light is a spot light, the
 * rotation of the node determines it's direction. If the contained light is a
 * Directional light rotation determines it's direction. The location is used
 * in conjunction with the bound attached to the light, to cull the lights
 * that currently have no effect on the rendered scene.
 * 
 * @author Mark Powell
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class LightNode extends RenderSpatial {

    // the local bound calculated from batches
    protected BoundingVolume localBound;
    
    public LightNode() {
        this("light");
    }

    /**
     * Constructor creates a new <code>LightState</code> object. The light
     * state the node controls is required at construction time.
     * 
     * @param name
     *            the name of the scene element. This is required for
     *            identification and comparision purposes.
     * @param lightState
     *            the lightstate that this node will control.
     */
    public LightNode(String name) {
        super(name);
    }

    public LightNode(String name, Light light) {
        super(name);
        LightBatch lb = new LightBatch();
        lb.setLight(light);
        setBatch(lb);
    }

    public void updateWorldBound() {
        if ((lockedMode & Spatial.LOCKED_BOUNDS ) != 0 && !changed) return;

        //if( localBound == null ) {
            // calculate the local bound
            if (((LightBatch)batch).getLocalBound() != null) {
                localBound = ((LightBatch)batch).getLocalBound()
                        .clone(localBound);
            }
        //}
        // transform the local bound
        if( localBound!= null )
            worldBound = localBound.clone(worldBound).transform(
                                        getWorldRotation(),
                                        getWorldTranslation(),
                                        getWorldScale(), worldBound);
    }

    @Override
    public String toString() {
        String lightType = null;
        if (batch != null)
            switch (((LightBatch)batch).getLight().getType()) {
                case Light.LT_DIRECTIONAL:
                    lightType = "Directional";
                    break;
                case Light.LT_POINT:
                    lightType = "Point";
                    break;
                case Light.LT_SPOT:
                    lightType = "Spot";
                    break;
                default:
                    lightType = "unknown";
            }
        return getName() +" ("+ lightType+")";
    }

}
