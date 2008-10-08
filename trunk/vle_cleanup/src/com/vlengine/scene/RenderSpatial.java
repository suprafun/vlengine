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

package com.vlengine.scene;

import com.vlengine.app.frame.Frame;
import com.vlengine.math.Transform;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.scene.control.UpdateContext;
import com.vlengine.util.FastList;

/**
 * RenderSpatial holds the reference to a Renderable, it is
 * responsible for thread-safe updating transform in the Renderable
 * 
 * @author vear (Arpad Vekas)
 */
public abstract class RenderSpatial extends Spatial {

    protected Renderable batch;
    
    // the transforms for child batches
    // for each of the frames
    protected Transform[] childTransforms;
    
    // the list of passes this node removed from active passes
    protected FastList<RenderPass> nopasses = new FastList<RenderPass>();

    public RenderSpatial(String name) {
        super(name);
        setupChildTransforms();
    }
    
    protected void setupChildTransforms() {
        childTransforms = new Transform[Frame.MAX_FRAMES];
        for( int i=0, mx=childTransforms.length; i<mx; i++ )
            childTransforms[i] = new Transform();
    }
    
    public void setBatch(Renderable batch) {
        // set the parent of the batch to be us
        batch.setParent(this);
        // lock the transforms of the batch to us
        batch.setWorldTransform(childTransforms);
        this.batch = batch;
    }
    
    public Renderable getBatch() {
        return batch;
    }
    
    @Override
    public void updateWorldVectors(UpdateContext ctx) {
        super.updateWorldVectors(ctx);
        
        // copy transforms to those used in child Batches
        Transform t = childTransforms[ctx.frameId];
        t.getRotation().set(worldRotation);
        t.getScale().set(worldScale);
        t.getTranslation().set(worldTranslation);
    }
    
    @Override
    public boolean queue(CullContext ctx) {
        if(batch == null) {
            return false;
        }
        // check the cull mode
        //long cm = getRenderQueueMode();
        if ( renderQueueMode != 0 ) {
            // prepare the list of passes, that dont need to be processed on children
            nopasses.clear();
            boolean found = false;
            boolean removed = false;
            // check for every our pass
            for(int i = 0; i < ctx.getPassQuantity(); i++) {
                RenderPass p = ctx.getPass(i);
                if(isUsePass(ctx, p)) {
                    found = true;
                } else {
                    ctx.removePass(p);
                    nopasses.add(p);
                    removed = true;
                    i--;
                }
            }
            if( found ) {
                // determine the LOD level to use
                // save the camera plane state
                int state = ctx.getCullCamera().getPlaneState();
                if( batch.docull(ctx) )
                    batch.queue(ctx);
                // restore the camera plane state
                ctx.getCullCamera().setPlaneState(state);
            }
            // after culling the children, restore previous passes
            if( removed ) {
                ctx.addPass(nopasses);
                nopasses.clear();
            }
            return found;
        }
        return false;
    }
    
    @Override
    public void updateCounts(boolean initiator) {
        if(initiator && parent!=null)
            parent.updateCounts(true);
        
        // calcualte depth now, so children get proper value
        if(parent!=null)
            depth = parent.depth + 1;
        else
            depth = 0;
        
        // we are 1 element
        maxelements = 1;
        
        // clear the queue mode
        renderQueueMode = RenderQueue.QueueFilter.None.value;

        if( batch != null ) {
            batch.updateCounts(false);
        }

        // pass our values up
        if( parent != null) {
            // compile elements into parent
            parent.maxelements += maxelements;
            // compile renderQueueMode into parent
            parent.renderQueueMode |= renderQueueMode;
        }
    }
}
