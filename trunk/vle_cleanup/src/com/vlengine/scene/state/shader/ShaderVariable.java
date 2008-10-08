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

import com.vlengine.util.geom.BufferUtils;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ShaderVariable {
    
    public String name;

    /** ID of uniform or attribute. * */
    //public int variableID = -1;
    public int variableIndex = -1;
    
    // 0- uniform
    // 1- vertex attribute
    public int type;
    
    public ShaderVariable() {}
    
    public ShaderVariable(String n) {
        name = n;
    }
    
    public void setName(String n) {
        name = n;
        //variableID = -1;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ShaderVariable) {
            ShaderVariable temp = (ShaderVariable) obj;
            if (name.equals(temp.name)) return true;
        }
        return false;
    }
    
    /*
    public void updateUniformLocation(int programID) {
        ByteBuffer nameBuf = BufferUtils
                .createByteBuffer(name.getBytes().length + 1);
        nameBuf.clear();
        nameBuf.put(name.getBytes());
        nameBuf.rewind();

        variableID = ARBShaderObjects
                .glGetUniformLocationARB(programID, nameBuf);

        if (variableID == -1) {
            // mark invalid uniform
            variableID = -2;
        }
    }
    
    public void updateAttributeLocation(int programID) {
        ByteBuffer nameBuf = BufferUtils
                .createByteBuffer(name.getBytes().length + 1);
        nameBuf.clear();
        nameBuf.put(name.getBytes());
        nameBuf.rewind();

        variableID = ARBVertexShader
                .glGetAttribLocationARB(programID, nameBuf);

        if (variableID == -1) {
            variableID = -2;
        }
    }
     */
    
    // to be impelemted in subclasses
    public void update(ShaderVariableLocation vl) {
        
    }
}
