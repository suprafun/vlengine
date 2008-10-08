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
import com.vlengine.light.Light;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.material.Material;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.state.AlphaBlendState;
import com.vlengine.scene.state.LightState;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.TextureState;
import com.vlengine.light.ShadowPart;
import com.vlengine.renderer.Renderer;
import com.vlengine.renderer.material.ShaderKey;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.util.FastList;
import org.lwjgl.opengl.GL11;

/**
 * Renders a queue with one light enabled, blending the result to the framebuffer,
 * if the shadow is set, then a shadowmap is applyed too.
 * @author vear (Arpad Vekas)
 */
public class LightPass extends RenderPass {
    
    
    // the lightstate we will use for the light
    LightState ls;
    // the texture state we will use to hold the enforced texture
    //TextureState ts;
    // the alpha state used to blend the lighting into color buffer
    //AlphaState as;
   
    
    public LightPass(String name) {
        super(name);
    }
    
    public LightState getLigthState() {
        return ls;
    }

    @Override
    public void renderPass(RenderContext ctx) {
        if(!enabled)
            return;
        FastList<Renderable> list = ctx.getRenderQueue().getQueue(this.queueNo);
        if( list==null || list.size() == 0 )
            return;
        
        Renderer renderer = ctx.getRenderer();
        
        // set enforced state to a lightstate, add the light into that
        /*
        if(ls==null) {
            ls=(LightState) renderer.createState(RenderState.RS_LIGHT);
            ls.setEnabled(true);
        }
        if(ts== null) {
            ts=(TextureState) renderer.createState(RenderState.RS_TEXTURE);
            ts.setEnabled(true);
        }
        if(as== null) {
            as=(AlphaState) renderer.createState(RenderState.RS_ALPHA);
            as.setBlendEnabled(true);
            as.setTestEnabled(false);
            // TODO: check this
            as.setSrcFunction(AlphaState.SB_ONE);
            as.setDstFunction(AlphaState.DB_ZERO);
        }
         */
        
        /*
        // set the light as enforced lightstate
        if(ls.get(0)!=light) {
            ls.detachAll();
            ls.attach(light);
            ls.setNeedsRefresh(true);
        }
         */
        ctx.enforcedStateList[RenderState.RS_LIGHT] = ls;
        
        int shadowMapUnit = TextureState.getNumberOfFixedUnits()-1;
        
        /*
        // if shadow is set, apply the shadowmap texture
        if( shadow!=null ) {
            useMaterial = Material.USAGE_LIGHTSHADOW;

            Texture t = shadow.getTexture();
            ts.clearTextures();
            ts.setTexture(t, shadowMapUnit);
            // apply the texture (set shadowmap to the choosen unit)
            ts.apply(ctx);
            // set the texture as enforced in the context
            // so that it will not be overwritten
            ctx.enforcedTextures[shadowMapUnit] = t;
            
            // set the Z depth range to that of the shadows
            // Z range, so that we dont overwrite stuff which belong
            // to another split
            // TODO: we should be able to restore these
            GL11.glDepthRange(camera.getFrustumNear(), camera.getFrustumFar());
            
        }
         */
        
        // enable alpha blending as color += (1-srcalpha)*color
        //ctx.enforcedStateList[as.getType()] = as;

        // TODO: render the queue
        for(int i=0, lm=list.size(); i<lm; i++) {
            Renderable e= list.get(i);
            // apply material
            Material mat = e.getMaterial();
            if(mat == null)
                mat = ctx.defaultMaterial;
            if( mat!= null && ctx.currentMaterial != mat )
                mat.apply(ctx);
            e.draw(ctx);
        }

        // restore stuff
        /*
        ctx.enforcedStateList[RenderState.RS_ALPHA] = null;
        ctx.enforcedTextures[shadowMapUnit] = null;
         */
        ctx.enforcedStateList[RenderState.RS_LIGHT] = null;
         
        
    }
}
