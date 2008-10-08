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

import java.nio.FloatBuffer;
import org.lwjgl.opengl.ARBVertexProgram;


/**
 * 
 * @author vear (Arpad Vekas)
 */
public class ShaderVariablePointerFloat extends ShaderVariablePointer {

    /** The data for the attribute value */
    public FloatBuffer data;

    @Override
    public void update(ShaderVariableLocation loc) {
        if( data != null ) {
            data.rewind();
            ARBVertexProgram.glEnableVertexAttribArrayARB(loc.variableID);
            ARBVertexProgram.glVertexAttribPointerARB(loc.variableID, size, normalized, stride, data);
        }
    }
    
    /**
     * Set an attribute pointer value for this shader object.
     *
     * @param size Specifies the number of values for each element of the
     * generic vertex attribute array. Must be 1, 2, 3, or 4.
     * @param normalized Specifies whether fixed-point data values should be
     * normalized or converted directly as fixed-point values when they are
     * accessed.
     * @param stride Specifies the byte offset between consecutive attribute
     * values. If stride is 0 (the initial value), the attribute values are
     * understood to be tightly packed in the array.
     * @param data The actual data to use as attribute pointer
     */
    public void set(int size, boolean normalized,
            int stride, FloatBuffer data) {
        this.size = size;
        this.normalized = normalized;
        this.stride = stride;
        this.data = data;
    }
}
