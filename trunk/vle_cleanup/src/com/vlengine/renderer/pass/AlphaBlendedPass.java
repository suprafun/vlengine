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

package com.vlengine.renderer.pass;

import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.Renderer;
import com.vlengine.scene.state.CullState;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.ZBufferState;
import com.vlengine.scene.state.ZBufferState;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class AlphaBlendedPass extends RenderPass {

    CullState cs;
    ZBufferState zs;
    
    public AlphaBlendedPass() {
        this("twosidedtransparent");
    }
    
    public AlphaBlendedPass(String name) {
        super(name);
        queueNo = RenderQueue.StandardQueue.AlphaBlended.queuId;
        queueFilter = RenderQueue.QueueFilter.AlphaBlended.value;
        // use normal opaque material
        materialNo = 0;
        this.id = RenderPass.StandardPass.AlphaBlended.passId;
    }
    
    @Override
    public void renderPass(RenderContext ctx) {
        Renderer renderer = ctx.getRenderer();

        if( cs == null ) {
            cs = (CullState) renderer.createState(RenderState.RS_CULL);
            cs.setEnabled(true);
        }
        
        if( zs!= null) {
            zs = (ZBufferState) renderer.createState(RenderState.RS_ZBUFFER);
            zs.setWritable(false);
            zs.setFunction(ZBufferState.CF_LEQUAL);
            zs.setEnabled(true);
        }
        
        RenderState oldcull = ctx.enforcedStateList[RenderState.RS_CULL];
        // save old Z state
        RenderState oldz = (ZBufferState) ctx.defaultStateList[RenderState.RS_ZBUFFER];
        
        // set cull to cull front
        cs.setCullMode(CullState.CS_FRONT);
        ctx.enforcedStateList[RenderState.RS_CULL] = cs;
        ctx.defaultStateList[RenderState.RS_ZBUFFER] = zs;
        
        // render the queue
        super.renderPass(ctx);

        // set cull to cull back
        cs.setCullMode(CullState.CS_BACK);
        ctx.enforcedStateList[RenderState.RS_CULL] = cs;
        // force reapply of the state
        ctx.currentStates[RenderState.RS_CULL] = null;
        // no need for Z writes on this pass, just the Z check
        ctx.defaultStateList[RenderState.RS_ZBUFFER] = zs;

        // render the queue again
        super.renderPass(ctx);

        // restore old Z state
        ctx.defaultStateList[RenderState.RS_ZBUFFER] = oldz;

        // restore the old cull state
        ctx.enforcedStateList[RenderState.RS_CULL] = oldcull;
        
        renderer.reset();
    }
}
