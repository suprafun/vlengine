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

package com.vlengine.scene.state.lwjgl;

import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.ShaderParameters;
import com.vlengine.scene.state.shader.ShaderTexture;
import com.vlengine.scene.state.shader.ShaderVariable;
import com.vlengine.scene.state.shader.ShaderVariableLocation;
import com.vlengine.scene.state.shader.ShaderVertexAttribute;



/**
 *
 * @author vear (Arpad Vekas)
 */
public class LWJGLShaderParameters extends ShaderParameters {
              
    int programID = 0;
    
    @Override
    public void apply( RenderContext context ) {
        
        if ( isEnabled() && context.currentProgramid != 0 ) {
            if(indep==null)
                context.currentStates[getType()] = this;
            boolean updateall = false;
            if(needsRefresh)
                updateall = true;
            needsRefresh = false;
            // no need to switch is program changes
            // get the shader program state
            ShaderObjectsState shader = (ShaderObjectsState) context.currentStates[RenderState.RS_GLSL_SHADER_OBJECTS];
            if(shader==null)
                return;
            if( programID != shader.getProgramId() ) {
                // shaderswitch
                updateall = true;
                programID = shader.getProgramId();
            }
            
            for (int i = shaderAttributes.size(); --i >= 0;) {
                ShaderVariable shaderVariable = shaderAttributes.get(i);
                ShaderVariableLocation loc = shader.getVariableLocation(shaderVariable, context);
                if ( loc.variableID == -1  ) {
                    loc.update(programID);
                }
                if( loc.variableID >= 0 )
                    shaderVariable.update(loc);
            }
            for (int i = shaderVertexAttrib.size(); --i >= 0;) {
                ShaderVertexAttribute shaderVariable = shaderVertexAttrib.get(i);
                if(shaderVariable==null)
                    continue;
                
                ShaderVariableLocation loc = shader.getVariableLocation(shaderVariable, context);
                if ( loc.variableID == -1  ) {
                    loc.update(programID);
                }
                // TODO: move code from LWJGLRenderer to update() method
                // and remove calling of update from here
                if( loc.variableID >= 0 )
                    shaderVariable.update(loc);
            }

            for (int i = shaderUniforms.size(); --i >= 0;) {
                ShaderVariable shaderVariable = shaderUniforms.get(i);
                ShaderVariableLocation loc = shader.getVariableLocation(shaderVariable, context);
                if ( loc.variableID == -1  ) {
                    loc.update(programID);
                }
                if( loc.variableID >= 0 )
                    shaderVariable.update(loc);
            }
            for (int i = shaderTextures.size(); --i >= 0;) {
                ShaderTexture shaderVariable = (ShaderTexture)shaderTextures.get(i);
                ShaderVariableLocation loc = shader.getVariableLocation(shaderVariable, context);
                if ( loc.variableID == -1  ) {
                    loc.update(programID);
                }
                if( loc.variableID >= 0 )
                    shaderVariable.update(context, loc);
            }
        }
    }

    // do nothing for implementation
    @Override
    public void update(RenderContext ctx) {}
    
    public int getProgramID() {
        return programID;
    }
}
