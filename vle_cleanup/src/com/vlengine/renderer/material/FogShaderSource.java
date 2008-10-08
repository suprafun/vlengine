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
public class FogShaderSource extends ShaderSource {

    @Override
    public String getVertDeclarations(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getVertBody1(ShaderKey key, HashMap<String, String> variables) {
        return "";
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
        String out = "";
        if(key.screendepthfog) {
            if(variables.get("color")!=null) {
                String fogdensity = "gl_Fog.density";
                String fogcolor = "gl_Fog.color";
              out += "const float LOG2 = 1.442695;\n" +
                     "if(gl_Fog.start > 0.0) {\n"+
                     "float z = gl_FragCoord.z / gl_FragCoord.w;\n" +
                     "z = max(z-gl_Fog.start, 0.0);\n"+
                     "float fogFactor = exp2( -"+fogdensity+" * "+fogdensity+" * z * z * LOG2 );\n" +
                     //"float fogFactor = z/(gl_Fog.start - gl_Fog.end);\n" +
                     "fogFactor = clamp(fogFactor, 0.0, 1.0);\n";
              
                  //variables.put("color", "color");
                  //out += "vec4 color = "+fogcolor+";\n";
              //} else {
                  out += this.replaceAll("'color = mix("+fogcolor+", 'color, fogFactor );\n", variables);
                  out += "}\n";
              }
        }
      return out;
    }

    @Override
    public String getFragEnd(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

}
