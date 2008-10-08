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

import com.vlengine.math.Matrix4f;
import com.vlengine.util.geom.BufferUtils;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL20;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ShaderVariableMatrix4Buffer extends ShaderVariable {
    public FloatBuffer matrixBuffer = null;
    public boolean transpose;
    
    @Override
    public void update(ShaderVariableLocation loc) {
        matrixBuffer.rewind();
        ARBShaderObjects.glUniformMatrix4ARB(loc.variableID,
                transpose, matrixBuffer);
        //GL20.glUniformMatrix4(variableID, transpose, matrixBuffer);
    }
    
    public void allocate(int numMatrices, boolean transpose ) {
        this.transpose = transpose;
        matrixBuffer = BufferUtils.createFloatBuffer(16*numMatrices);
    }
    
    public void set(int index, Matrix4f value) {
        matrixBuffer.position(index*16);
        value.putFloatBufferRowMajor(matrixBuffer);
    }
    
    public void put(Matrix4f value) {
        value.putFloatBufferRowMajor(matrixBuffer);
    }
    
    public Matrix4f get(Matrix4f store) {
        return store.readFloatBuffer(matrixBuffer, false);
    }
    
    public void rewind() {
        matrixBuffer.rewind();
    }
    
    public void position(int index) {
        matrixBuffer.position(index*16);
    }

    public int limit() {
        if(matrixBuffer==null)
            return 0;
        return matrixBuffer.limit()/16;
    }
}
