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

import com.vlengine.math.Quaternion;
import com.vlengine.renderer.ColorRGBA;
import org.lwjgl.opengl.ARBShaderObjects;



/**
 * 
 * @author vear (Arpad Vekas)
 */
public class ShaderVariableFloat4 extends ShaderVariable {
    public float value1;
    public float value2;
    public float value3;
    public float value4;

    @Override
    public void update(ShaderVariableLocation loc) {
        ARBShaderObjects.glUniform4fARB(loc.variableID,
                value1, value2,
                value3, value4);
    }
    
    /**
     * Set an uniform value for this shader object.
     *
     * @param name uniform variable to change
     * @param value1 the new value
     * @param value2 the new value
     * @param value3 the new value
     * @param value4 the new value
     */
    public void set(float value1, float value2,
            float value3, float value4) {
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
    }
    
    /**
     * Set an uniform value for this shader object.
     *
     * @param value the new value
     */
    public void set(ColorRGBA value) {
        this.value1 = value.r;
        this.value2 = value.g;
        this.value3 = value.b;
        this.value4 = value.a;
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param name uniform variable to change
     * @param value the new value
     */
    public void set(Quaternion value) {
        this.value1 = value.x;
        this.value2 = value.y;
        this.value3 = value.z;
        this.value4 = value.w;
    }
}
