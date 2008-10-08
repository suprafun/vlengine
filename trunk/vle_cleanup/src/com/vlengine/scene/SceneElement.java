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
import com.vlengine.renderer.Camera;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.LightState;
import com.vlengine.thread.Context;
import com.vlengine.thread.LocalContext;
import java.util.logging.Logger;

/**
 *
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public abstract class SceneElement {
    private static final Logger logger = Logger.getLogger(SceneElement.class
            .getName());
    
    public static enum CullMode {
        ALWAYS,
        DYNAMIC,
        INHERIT,
        NEVER;
    }
    public static final CullMode CULL_NEVER = CullMode.NEVER;
    public static final CullMode CULL_DYNAMIC = CullMode.DYNAMIC;
    
    protected CullMode cullMode = CullMode.DYNAMIC;
    
    // renderqueue filter for fro this element
    protected long renderQueueMode = RenderQueue.QueueFilter.Any.value;
    // renderpass filter for this element and its children
    protected long renderPassMode = RenderPass.PassFilter.Any.value;

    // may be needed to store, for which camera this intersection is valid
    protected int frustrumIntersects = Camera.INTERSECTS_FRUSTUM;
    
    /** Spatial's parent, or null if it has none. */
    protected transient Spatial parent;
    
    /** Spatial's bounding volume relative to the world. */
    protected BoundingVolume worldBound;
     
    // the depth of this element
    protected int depth = 0;
    
    // the maximum number of elements this element contains
    protected int maxelements = 1;
   
    protected int lightCombineMode = LightState.INHERIT;
    
    public BoundingVolume getWorldBound() {
        return worldBound;
    }
    
    public abstract void updateWorldBound();
    
    public void setCullMode( CullMode mode ) {
        cullMode = mode;
    }
    
    public int getLastFrustumIntersection() {
        return frustrumIntersects;
    }
    
    public void setLastFrustumIntersection(int intersects) {
        frustrumIntersects = intersects;
    }
    
        
    public Spatial getParent() {
        return parent;
    }
    
    protected void setParent(Spatial parent) {
        this.parent = parent;
    }
    
    // culls this spatial, return if it is visible
    // this method can also change states of the scenecontext
    public boolean docull( CullContext ctx ) {
        
        CullMode cm = getCullMode();
        if (cm == CullMode.ALWAYS) {
            setLastFrustumIntersection(Camera.OUTSIDE_FRUSTUM);
            return false;
        } else if (cm == CullMode.NEVER) {
            setLastFrustumIntersection(Camera.INSIDE_FRUSTUM);
            return true;
        }
        
        // check to see if we can cull this node
        frustrumIntersects = (parent != null ? parent.frustrumIntersects
                : Camera.INTERSECTS_FRUSTUM);

        if (cm == CullMode.DYNAMIC && frustrumIntersects == Camera.INTERSECTS_FRUSTUM) {
            Camera camera = ctx.getCullCamera();
            frustrumIntersects = camera.contains(worldBound);
        }

        if (frustrumIntersects != Camera.OUTSIDE_FRUSTUM) {
            return true;
        }
        
        return false;
    }
    
    public CullMode getCullMode() {
        if (cullMode != CullMode.INHERIT)
            return cullMode;
        else if (parent != null)
            return parent.getCullMode();
        else return CullMode.DYNAMIC;
    }
    
    // returns the id of the frame that currently processes
    // this batch, this is for safe way to process
    // this batch from multiple threads, one updating, and
    // the other rendering
    
    protected int getFrameId() {
        Context ctx = LocalContext.getContext();
        return ctx.scene.getFrameId();
    }
    
    /**
     * Returns this spatial's renderqueue mode. If the mode is set to inherit,
     * then the spatial gets its renderqueue mode from its parent.
     *
     * @return The spatial's current renderqueue mode.
     */
    public long getRenderQueueMode() {
//        if (renderQueueMode != RenderQueue.FILTER_INHERIT)

            return renderQueueMode;
/*
        else if (parent != null)
            return parent.getRenderQueueMode();
        else
            return RenderQueue.FILTER_OPAQUE;
 */
    }
    
    public void setRenderQueueMode(long qmode) {
        this.renderQueueMode = qmode;
    }
    
    public long getRenderPassMode() {
        return renderPassMode;
    }
    
    public void setRenderPassMode(long pmode) {
        this.renderPassMode = pmode;
    }
    
    public boolean queue( CullContext ctx ) {
        // check the cull mode
        long cm = renderQueueMode;
        if ( cm != 0 ) {
            // check for every our pass
            for(int i = 0; i < ctx.getPassQuantity(); i++) {
                RenderPass p = ctx.getPass(i);
                if(isUsePass(ctx, p))
                    return true;
            }
        }
        return false;
    }

    /**
     * Returns if this element is to be rendered within the given queue.
     * 
     * @param ctx   The CullContext currently in effect
     * @param p     A RenderPass
     * @return      If the element is to be placed into the passes queues
     */
    public boolean isUsePass(CullContext ctx, RenderPass p) {

        if( ( renderQueueMode & p.getQueueFilter() ) != 0 
              && ((1<<p.getId())
                        & ctx.passfilter
                        & this.renderPassMode) != 0) {
                    // the element passed for at least one
                    return true;
        }
        return false;
    }
    
    /*
    // compile data prior to main game loop
    public void compile() {
        // compile depth
        if( parent != null) {
            depth = parent.depth + 1;
            
            // compile elements into parent
            parent.maxelements += maxelements;
        
            // compile renderQueueMode into parent
            parent.renderQueueMode |= renderQueueMode;

        } else
            depth = 0;
    }
     */
    public void updateCounts(boolean initiator) {
        if(initiator && parent!=null)
            parent.updateCounts(true);
        if( parent != null) {
            depth = parent.depth + 1;
            
            // compile elements into parent
            parent.maxelements += maxelements;
        
            // compile renderQueueMode into parent
            parent.renderQueueMode |= renderQueueMode;

        } else
            depth = 0;
    }
    
    public void setLightCombineMode(int lightCombineMode) {
        this.lightCombineMode = lightCombineMode;
    }

    /**
     * @return the lightCombineMode set on this Spatial
     */
    public int getLightCombineMode() {
        return lightCombineMode;
    }
}
