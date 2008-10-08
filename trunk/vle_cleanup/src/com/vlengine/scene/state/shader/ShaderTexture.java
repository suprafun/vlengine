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

package com.vlengine.scene.state.shader;

import com.vlengine.image.Texture;
import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.lwjgl.LWJGLTextureState;
import com.vlengine.system.VleException;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ShaderTexture extends ShaderVariableInt {
    // the value1 contains the texture unit number
    private Texture tex;
    
    public ShaderTexture() {}
    
    public ShaderTexture(String name, Texture t) {
        set(name, t);
    }
    
    public void set(String name, Texture t) {
        this.name = name;
        this.tex = t;
        value1 = -1;
    }
    
    public void set(Texture t) {
        this.tex = t;
        value1 = -1;
    }
    
    public void update(RenderContext ctx, ShaderVariableLocation loc) {
        if(tex == null)
            return;
        // get the shared texture state
        LWJGLTextureState rs=ctx.shTex;
        RenderState defst = ctx.currentStates[RenderState.RS_TEXTURE];
        // is it already bound to the proper unit?
        if( value1 > -1 ) {
            // if its not boud to where was previously
            // try to find it bound to some other unit
            
            // XXX beware, it could happen that reusing this way is not safe
            // more textures/object can overwrite textures
            if( rs.getTexture( value1 ) != tex )
                value1 = rs.getUnit( tex );
        }
        if( value1 == -1 ) {
            // not found, add the texture
            value1 = rs.addShaderTexture(tex);
            if( value1 == -1)
                throw new VleException("Could not load shader texture");
            // load the new texture only, if the shader texture state
            // is alread the current
            if( defst == rs)
                rs.apply(ctx, value1);
        }
        
        // if the shader texture state is not the current
        // then set it to be the current
        if( defst != rs ) {
            rs.apply(ctx);
            //ctx.currentStates[RenderState.RS_TEXTURE] = rs;
        }
        
        // set the choosen unit to shader uniform
        super.update(loc);
    }
}
