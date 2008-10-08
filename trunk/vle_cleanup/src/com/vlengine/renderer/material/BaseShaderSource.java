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
public class BaseShaderSource extends ShaderSource {
    String base = "void main(void) {\n";
    String vertpos = "gl_Position = gl_ModelViewProjectionMatrix * 'position ;\n";
    String end = "}\n";

    @Override
    public String getVertDeclarations(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getVertBody1(ShaderKey key, HashMap<String, String> variables) {
        return base;
    }
    
    @Override
    public String getVertBody2(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }
    
    @Override
    public String getVertBody3(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getVertBodyEnd(ShaderKey key, HashMap<String, String> variables) {
        String out = "";
        if(variables.get("position")==null)
            variables.put("position", "gl_Vertex");
        if(variables.get("normal")==null)
            variables.put("normal", "gl_Normal");
        out += this.replaceAll(vertpos, variables);
        out += end;
        return out;
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
        return base;
    }

    @Override
    public String getFragBody2(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getFragEnd(ShaderKey key, HashMap<String, String> variables) {
        String out = "";
        if(variables.get("color") == null) {
            out += "gl_FragColor = vec4(1.0,1.0,1.0,1.0);\n";
        } else {
            out += this.replaceAll("gl_FragColor = 'color ;\n", variables);
        }
        out += end;
        return out;
    }
    
}
