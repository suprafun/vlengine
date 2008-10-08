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

package com.vlengine.renderer.material;

import java.util.HashMap;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class SkinShaderSource extends ShaderSource {

    @Override
    public String getVertDeclarations(ShaderKey key, HashMap<String, String> variables) {
        return "attribute vec4 boneWeight;\n" +
                "attribute vec4 boneIndex;\n" +
                "attribute float weightNum;\n" +
                "uniform mat4 boneMatrix[" +key.numBones + "];\n";
    }

    @Override
    public String getVertBody1(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getVertBody2(ShaderKey key, HashMap<String, String> variables) {
        String ret ="";
        ret += "vec4 tPosition = vec4(0.0, 0.0, 0.0, 0.0);\n";

        if(key.normalType==ShaderKey.NORMALTYPE_VERTEX) {
           //vsm.append("vec4 tempNormal = vec4(gl_Normal.xyz,0.0);\n");
           //vsm.append("vec4 tNormal  = vec4( 0.0, 0.0, 0.0, 0.0 );\n");
           ret += "vec3 tempNormal = gl_Normal.xyz;\n";
           ret += "vec3 tNormal  = vec3( 0.0, 0.0, 0.0 );\n";
        }

        ret += "vec4 curIndex = boneIndex;\n";
        ret += "vec4 curWeight = boneWeight;\n";
        ret += "float numWeights = weightNum;\n";

        ret += "for (int i = 0; i < int(numWeights); i++) {\n";
        ret += "mat4 m44 = boneMatrix[int(curIndex.x)];\n";
        ret += "tPosition = tPosition + (curWeight.x *( m44* gl_Vertex));\n";

        if(key.normalType==ShaderKey.NORMALTYPE_VERTEX) {
            ret += "mat3 m33 = mat3(m44[0].xyz, m44[1].xyz, m44[2].xyz);\n";
            ret += "tNormal = tNormal + (m33 * tempNormal * curWeight.x);\n";
        }

        ret += "curIndex = curIndex.yzwx;\n";
        ret += "curWeight = curWeight.yzwx;\n";
        ret += "}\n";

        //ret += "gl_Position = gl_ModelViewProjectionMatrix * tPosition;\n";

        variables.put("position", "tPosition");
        
        if(key.normalType==ShaderKey.NORMALTYPE_VERTEX) {
            variables.put("normal", "tNormal.xyz");
        }
        return ret;
    }

    @Override
    public String getVertBody3(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getVertBodyEnd(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getFragDeclarations(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getFragFunctions(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getFragBody1(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getFragBody2(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getFragEnd(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

}
