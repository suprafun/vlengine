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
import com.vlengine.scene.state.CullState;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.ZBufferState;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class OpaquePass extends RenderPass {
    
    ZBufferState zs;
    CullState cs;
    
    boolean writezbuff = true;
    
    public OpaquePass() {
        super("opaque");
    }
    
    public OpaquePass(String name) {
        super(name);
    }
    
    
    public void setZbufferWrite(boolean zbufferWrite) {
        writezbuff = zbufferWrite;
    }
    
    @Override
    public void renderPass(RenderContext ctx) {
        Renderer renderer = ctx.getRenderer();
        
        // save the default ZState
        RenderState oldz = ctx.defaultStateList[RenderState.RS_ZBUFFER];
        
        if( zs == null ) {
            zs = (ZBufferState) renderer.createState(RenderState.RS_ZBUFFER);
            zs.setFunction( ZBufferState.CF_LEQUAL );
            zs.setWritable(writezbuff);
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
        //ctx.enforcedStateList[RenderState.RS_ZBUFFER] = zs;
        // set cull to cull back
        ctx.defaultStateList[RenderState.RS_CULL] = cs;
        //ctx.enforcedStateList[RenderState.RS_CULL] = cs;
        
        //if(!writezbuff)
        //    renderer.setPolygonOffset(1.1f, 4f);
        
        // render the queue
        super.renderPass(ctx);
        
        //renderer.setPolygonOffset(0f, 0f);

        // restore the old Z state
        ctx.defaultStateList[RenderState.RS_ZBUFFER] = oldz;
        //ctx.enforcedStateList[RenderState.RS_ZBUFFER] = null;
        ctx.defaultStateList[RenderState.RS_CULL] = oldcull;
        //ctx.enforcedStateList[RenderState.RS_CULL] = null;
        
        renderer.reset();
    }
}
