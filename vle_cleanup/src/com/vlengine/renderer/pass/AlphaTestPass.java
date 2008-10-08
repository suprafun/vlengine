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
import com.vlengine.renderer.Renderer;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.state.CullState;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.ZBufferState;
import com.vlengine.util.FastList;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class AlphaTestPass extends RenderPass {

    ZBufferState zs;
    CullState cs;
    
    public AlphaTestPass() {
        this("transparent");
    }
    
    public AlphaTestPass(String name) {
        super(name);
    }
    
    @Override
    public void renderPass(RenderContext ctx) {
        if(!enabled)
            return;
        
        // get the list of renderables, dont need any sorting
        FastList<Renderable> list = ctx.getRenderQueue().getQueue(this.queueNo);
        if( list ==null || list.size() == 0 )
            return;
        
        Renderer renderer = ctx.getRenderer();
        
        // save the default ZState
        RenderState oldz = ctx.defaultStateList[RenderState.RS_ZBUFFER];
        
        if( zs == null ) {
            zs = (ZBufferState) renderer.createState(RenderState.RS_ZBUFFER);
            // is occluded by opaque
            zs.setFunction( ZBufferState.CF_LEQUAL );
            // but not from transparent
            zs.setWritable(false);
            zs.setEnabled(true);
        }
        
        RenderState oldcull = ctx.defaultStateList[RenderState.RS_CULL];
        
        if( cs == null ) {
            cs = (CullState) renderer.createState(RenderState.RS_CULL);
            cs.setCullMode(CullState.CS_BACK);
            cs.setEnabled(true);
        }
        // set the Z function to CF_LEQUAL
        ctx.defaultStateList[RenderState.RS_ZBUFFER] = zs;
        // set cull to cull back
        ctx.defaultStateList[RenderState.RS_CULL] = cs;
        
        // render the queue
        super.renderPass(ctx);
        
        // restore the old Z state
        ctx.defaultStateList[RenderState.RS_ZBUFFER] = oldz;
        ctx.defaultStateList[RenderState.RS_CULL] = oldcull;
        
        renderer.reset();
    }
}
