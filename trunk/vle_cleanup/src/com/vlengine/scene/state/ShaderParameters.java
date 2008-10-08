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

package com.vlengine.scene.state;

import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.state.shader.ShaderTexture;
import com.vlengine.scene.state.shader.ShaderVariable;
import com.vlengine.scene.state.shader.ShaderVertexAttribute;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.VertexAttribute;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ShaderParameters extends RenderState {

    /** Storage for shader uniform values */
    protected FastList<ShaderVariable> shaderUniforms =
            new FastList<ShaderVariable>();
    
    /** Storage for shader attribute values */
    protected FastList<ShaderVariable> shaderAttributes =
            new FastList<ShaderVariable>();
    
    // textures, (name, uniformid, unit, texture)
    protected FastList<ShaderVariable> shaderTextures =
            new FastList<ShaderVariable>();
    
    // interleaved vertex array shader attributes, by usage
    protected FastList<ShaderVertexAttribute> shaderVertexAttrib =
            new FastList<ShaderVertexAttribute>();
    
    public ShaderVariable getUniform(String name) {
        return getShaderVariable(name, shaderUniforms);
    }
    
    public ShaderVariable getAttribute(String name) {
        return getShaderVariable(name, shaderAttributes);
    }
    
    public ShaderTexture getTexture(String name) {
        return (ShaderTexture)getShaderVariable(name, shaderTextures);
    }

    public ShaderVertexAttribute getVertexAttribute(VertexAttribute.Usage usage) {
        if(usage.id < shaderVertexAttrib.size())
            return shaderVertexAttrib.get(usage.id);
        return null;
    }

    public ShaderVertexAttribute getVertexAttribute(String name) {
        for (int i = shaderVertexAttrib.size(); --i >= 0;) {
            ShaderVertexAttribute temp = shaderVertexAttrib.get(i);
            if (name.equals(temp.name)) {
                return temp;
            }
        }
        return null;
    }

    public void setUniform(ShaderVariable v) {
        v.type = 0;
        setVariable(v,shaderUniforms);
    }

    public void setAttribute(ShaderVariable v) {
        v.type = 1;
        setVariable(v,shaderAttributes);
    }

    public void setTexture(ShaderTexture v) {
        v.type = 0;
        setVariable(v,shaderTextures);
    }

    public void setVertexAttribute(ShaderVertexAttribute attr) {
        attr.type = 1;
        shaderVertexAttrib.ensureCapacity(attr.usage.id+1);
        shaderVertexAttrib.set(attr.usage.id, attr);
    }

    public void removeUniform(String name) {
        removeVariable(name, shaderUniforms);
    }
    
    public void removeAttribute(String name) {
        removeVariable(name, shaderAttributes);
    }
    
    public void removeTexture(String name) {
        removeVariable(name, shaderTextures);
    }
    
    private void setVariable(ShaderVariable v, FastList<ShaderVariable> list) {
         for (int i = list.size(); --i >= 0;) {
            ShaderVariable temp = list.get(i);
            if (v.name.equals(temp.name)) {
                list.set(i, v);
                return;
            }
        }
        list.add(v);
        needsRefresh = true;
    }
    
    private void removeVariable(String name, FastList<ShaderVariable> list) {
         for (int i = list.size(); --i >= 0;) {
            ShaderVariable temp = list.get(i);
            if (name.equals(temp.name)) {
                list.remove(i);
                return;
            }
        }
        needsRefresh = true;
    }
    
    public void clear() {
        shaderUniforms.clear();
        shaderAttributes.clear();
        shaderTextures.clear();
    }
    
    private ShaderVariable getShaderVariable(String name,FastList<ShaderVariable> shaderVariableList) {
        for (int i = shaderVariableList.size(); --i >= 0;) {
            ShaderVariable temp = shaderVariableList.get(i);
            if (name.equals(temp.name)) {
                return temp;
            }
        }
        return null;
    }
    
    
    @Override
    public int getType() {
        return RenderState.RS_GLSL_SHADER_PARAM;
    }

    @Override
    public void update(RenderContext ctx) {
        if( indep != null )
            return;
        super.update(ctx);
        if( needsRefresh ) {
            ((ShaderParameters)impl).enabled = this.enabled;
            ((ShaderParameters)impl).needsRefresh = true;

            ((ShaderParameters)impl).shaderUniforms.clear();
            ((ShaderParameters)impl).shaderUniforms.addAll(shaderUniforms);

            ((ShaderParameters)impl).shaderAttributes.clear();
            ((ShaderParameters)impl).shaderAttributes.addAll(shaderAttributes);

            ((ShaderParameters)impl).shaderTextures.clear();
            ((ShaderParameters)impl).shaderTextures.addAll(shaderTextures);
            
            ((ShaderParameters)impl).shaderVertexAttrib.clear();
            ((ShaderParameters)impl).shaderVertexAttrib.addAll(shaderVertexAttrib);
            
            needsRefresh = false;
        }
    }
    
    public int getProgramID() {
        if(impl!=null)
            return ((ShaderParameters)impl).getProgramID();
        return 0;
    }
}
