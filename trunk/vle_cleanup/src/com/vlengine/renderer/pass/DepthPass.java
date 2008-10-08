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

import com.vlengine.image.Texture;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.Renderer;
import com.vlengine.renderer.lwjgl.LWJGLTextureRenderer;
import com.vlengine.renderer.material.ShaderKey;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.state.ColorMaskState;
import com.vlengine.scene.state.CullState;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.ZBufferState;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.BufferUtils;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

/**
 * The depth pass is used to pre-render the main scene with filling only the
 * depth buffer. If enabled, the later passes only check the depth buffer, and do not write
 * to it. It has performance benefits, since graphics cards can fastly discard non-visible fragments.
 * Only use this pass to process the main scene. If you need to render a sub-scene (for example a Lights view),
 * then use the DepthTexturePass
 * @author vear (Arpad Vekas)
 */
public class DepthPass extends RenderPass {

    ZBufferState zs;
    ColorMaskState cms;
    CullState cs;
    FloatBuffer fbparam = BufferUtils.createFloatBuffer(16);
    
    public DepthPass() {
        super("depth");
        this.setId(RenderPass.StandardPass.Depth.passId);
        this.setQueueNo(RenderQueue.StandardQueue.Opaque.queuId);
        this.setQueueFilter(RenderQueue.QueueFilter.None.value);
        // TODO:
        this.setUsedMaterialFlags(0);
    }

    public DepthPass(String name) {
        super(name);
    }
   
    @Override
    public void renderPass(RenderContext ctx) {
        if(!enabled)
            return;
        
        // get the list of renderables, dont need any sorting
        FastList<Renderable> list = ctx.getRenderQueue().getQueue(this.queueNo);
        if( list.size() == 0 )
            return;

        Renderer renderer = ctx.getRenderer();
        
        // save the default ZState
        RenderState oldz = ctx.defaultStateList[RenderState.RS_ZBUFFER];
        
        if( zs == null ) {
            zs = (ZBufferState) renderer.createState(RenderState.RS_ZBUFFER);
            zs.setFunction( ZBufferState.CF_LESS);
            zs.setWritable(true);
            zs.setEnabled(true);
        }
        
        RenderState oldcm = ctx.defaultStateList[RenderState.RS_COLORMASK_STATE];
        
        if(cms == null) {
            cms = (ColorMaskState) renderer.createState(RenderState.RS_COLORMASK_STATE);
            cms.setAll(false);
            cms.setEnabled(true);
        }

        RenderState oldcull = ctx.defaultStateList[RenderState.RS_CULL];
        
        if( cs == null ) {
            cs = (CullState) renderer.createState(RenderState.RS_CULL);
            cs.setCullMode(CullState.CS_NONE);
            cs.setEnabled(true);
        }

        // set the Z function to CF_LEQUAL
        ctx.defaultStateList[RenderState.RS_ZBUFFER] = zs;
        ctx.enforcedStateList[RenderState.RS_ZBUFFER] = zs;
        ctx.defaultStateList[RenderState.RS_COLORMASK_STATE] = cms;
        ctx.enforcedStateList[RenderState.RS_COLORMASK_STATE] = cms;
        ctx.defaultStateList[RenderState.RS_CULL] = cs;
        ctx.enforcedStateList[RenderState.RS_CULL] = cs;
        //renderer = ctx.getRenderer();

        zs.apply(ctx);
        cms.apply(ctx);
        cs.apply(ctx);

        fbparam.rewind();
        GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR, fbparam);
        fbparam.rewind();
        float factor = fbparam.get();
        fbparam.rewind();
        GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS, fbparam);
        fbparam.rewind();
        float offset = fbparam.get();
        
        //renderer.setPolygonOffset(0f, -8f);

        // TODO: pass only position data, ignore any other 
        // how: array in renderer to enable, disable vertex attribute passing to rendering
        renderer.setPositionOnlyMode(true);
        /*
        boolean material = false;
        // draw all the elements
        boolean resetTex = false;
        for(int i=0, ls=list.size(); i<ls; i++) {
            Renderable e= list.get(i);
            // TODO: maybe we still need shaders because of vertex animation?
            
            Material m = e.getMaterial();
            // if the batch has alpha-test state, then we do need textures
            
            AlphaState as = m!=null ? (AlphaState) m.getRenderState( RenderState.RS_ALPHA):null;
            ShaderObjectsState ss = m!=null ? (ShaderObjectsState) m.getRenderState( RenderState.RS_GLSL_SHADER_OBJECTS):null;
            if(as!=null || ss!=null) {
                // we have alpha state, get texture state
                m.apply(ctx);
                resetTex = true;
            } else {
                if(resetTex) {
                    // we need to apply default material
                    ctx.defaultMaterial.apply(ctx);
                    resetTex = false;
                }
            }
            e.draw(ctx);
        }
        if(resetTex) {
            // we need to apply default material
            ctx.defaultMaterial.apply(ctx);
            resetTex = false;
        }
         */
        super.renderPass(ctx);
        renderer.setPositionOnlyMode(false);

        //GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        //renderer.setPolygonOffset(factor, offset);
        //renderer.clearPolygonOffset();

        // restore the old Z state
        ctx.defaultStateList[RenderState.RS_ZBUFFER] = oldz;
        ctx.defaultStateList[RenderState.RS_COLORMASK_STATE] = oldcm;
        ctx.defaultStateList[RenderState.RS_CULL] = oldcull;
        ctx.enforcedStateList[RenderState.RS_ZBUFFER] = null;
        ctx.enforcedStateList[RenderState.RS_COLORMASK_STATE] = null;
        ctx.enforcedStateList[RenderState.RS_CULL] = null;
        ctx.defaultMaterial.apply(ctx);
        
        renderer.reset();
    }
}
