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

import org.lwjgl.opengl.ARBShaderObjects;


/**
 * 
 * @author vear (Arpad Vekas)
 */
public class ShaderVariableInt extends ShaderVariable {
    public int value1;

    public ShaderVariableInt() {};
    
    public ShaderVariableInt(String string) {
        super(string);
    }    
    
    public ShaderVariableInt(String string, int value) {
        super(string);
        value1 = value;
    }
    
    @Override
    public void update(ShaderVariableLocation loc) {
        ARBShaderObjects.glUniform1iARB(loc.variableID, value1);
    }
    
    /**
     * Set an uniform value for this shader object.
     *
     * @param name uniform variable to change
     * @param value the new value
     */
    public void set(boolean value) {
        value1 = value ? 1 : 0;
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param name uniform variable to change
     * @param value the new value
     */
    public void set(int value) {
        value1 = value;
    }
}
