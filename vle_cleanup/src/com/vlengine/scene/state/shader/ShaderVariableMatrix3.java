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

import com.vlengine.math.Matrix3f;
import com.vlengine.util.geom.BufferUtils;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.ARBShaderObjects;

/**
 * 
 * @author vear (Arpad Vekas)
 */
public class ShaderVariableMatrix3 extends ShaderVariable {
    public FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(9);
    public boolean transpose;

    @Override
    public void update(ShaderVariableLocation loc) {
        matrixBuffer.rewind();
        ARBShaderObjects.glUniformMatrix3ARB(loc.variableID,
                transpose, matrixBuffer);
    }
    
    /**
     * Set an uniform value for this shader object.
     *
     * @param value the new value
     * @param transpose transpose the matrix ?
     */
    public void set(Matrix3f value, boolean transpose) {
        value.fillFloatBuffer(matrixBuffer);
        this.transpose = transpose;
    }
}
