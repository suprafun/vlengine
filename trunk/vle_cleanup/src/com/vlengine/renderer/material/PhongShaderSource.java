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
 * Class for constructing a phong shader, called by
 * the ShaderMaterialLib
 * @author vear (Arpad Vekas)
 */
public class PhongShaderSource extends ShaderSource {
    final String header1 = "varying vec3 normal;\n";
    final String header2 = "varying vec3 vertex;\n";
    final String vertPostNormal = "normal = normalize(gl_NormalMatrix * 'normal );\n";
    final String vertPostPosition = "vertex = vec3(gl_ModelViewMatrix * 'position );\n";
    
    public String getVertDeclarations(ShaderKey key, HashMap<String,String> variables) {
        if(!key.hasLight()) return "";
        String out = "";
        if(key.normalType == ShaderKey.NORMALTYPE_VERTEX)
            out += header1;
        out += header2;
        return out;
    }

    public String getVertBody1(ShaderKey key, HashMap<String,String> variables) {
        return "";
    }

    @Override
    public String getVertBody2(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    public String getVertBody3(ShaderKey key, HashMap<String,String> variables) {
        if(!key.hasLight()) return "";
        String ret = "";
        if(key.normalType == ShaderKey.NORMALTYPE_VERTEX) {
            if(variables.get("normal") == null)
                variables.put("normal", "gl_Normal");
            ret += replaceAll(vertPostNormal, variables);
        }
        if(variables.get("position")==null)
            variables.put("position", "gl_Vertex");
        ret += replaceAll(vertPostPosition, variables);
        return ret;
    }

    @Override
    public String getVertBodyEnd(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    public String getFragDeclarations(ShaderKey key, HashMap<String,String> variables) {
        if(!key.hasLight()) return "";
        String out = "";
        if(key.normalType == ShaderKey.NORMALTYPE_VERTEX)
            out += header1;
        out += header2;
        return out;
    }

    @Override
    public String getFragFunctions(ShaderKey key, HashMap<String, String> variables) {
        if(!key.hasLight()) return "";

        String out = "";
        boolean directional = false;
        boolean point = false;
        boolean point_att = false;
        boolean spot = false;
        boolean spot_att = false;
        boolean attenuate = false;
        for(int i=0; i<key.light.length; i++) {
            if(key.light[i] == 1) {
                directional = true;
            } else if(key.light[i] == 2) {
                if(key.attenuate[i]) {
                    point_att = true;
                } else {
                    point = true;
                }
            } else if(key.light[i] == 3) {
                if(key.attenuate[i]) {
                    spot_att = true;
                } else {
                    spot = true;
                }
            }
            if(key.light[i]!= 0 && key.attenuate[i]) {
                attenuate = true;
            }
        }
        if(attenuate) {
            out +=
                "float calculateAttenuation(in int i, in float dist) {\n" +
                    "return(1.0 / (gl_LightSource[i].constantAttenuation + gl_LightSource[i].linearAttenuation * dist + gl_LightSource[i].quadraticAttenuation * dist * dist));\n" +
                "}\n";
        }
        
        // output functions
        if(directional) {
            out += 
                "void directionalLight(in int i, in vec3 N, in float shininess, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular) {\n" +
                    "vec3 L = normalize(gl_LightSource[i].position.xyz);\n" +
                    "float nDotL = dot(N, L);\n" +
                    "if (nDotL > 0.0) {\n" +
                        "diffuse  += gl_LightSource[i].diffuse  * nDotL;\n";
                        if(!key.nospecular) {
                            out += 
                            "vec3 H = gl_LightSource[i].halfVector.xyz;\n" +
                            "float pf = pow(max(dot(N,H), 0.0), shininess);\n" +
                            "specular += gl_LightSource[i].specular * pf;\n";
                        }
                 out +=
                    "}\n" +
                    "ambient  += gl_LightSource[i].ambient;\n" +
                "}\n";
        }
        if(point_att) {
            out += 
                "void pointLightAtt(in int i, in vec3 N, in vec3 V, in float shininess, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular) {\n" +
                    "vec3 D = gl_LightSource[i].position.xyz - V;\n" +
                    "vec3 L = normalize(D);\n" +
                    "float dist = length(D);\n" +
                    "float attenuation = calculateAttenuation(i, dist);\n" +
                    "float nDotL = dot(N,L);\n" +
                    "if (nDotL > 0.0) {\n" +
                        "diffuse  += gl_LightSource[i].diffuse  * attenuation * nDotL;\n";
                        if(!key.nospecular) {
                            out += 
                            "vec3 E = normalize(-V);\n" +
                            "vec3 R = reflect(-L, N);\n" +
                            "float pf = clamp(pow(max(dot(R,E), 0.0), shininess), 0.0, 1.0);\n" +
                            "specular += gl_LightSource[i].specular * attenuation * pf;\n";
                        }
                    out += "}\n" +
                    "ambient  += gl_LightSource[i].ambient * attenuation;\n" +
                "}\n";
        }
        if(point) {
            out += 
                "void pointLight(in int i, in vec3 N, in vec3 V, in float shininess, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular) {\n" +
                    "vec3 D = gl_LightSource[i].position.xyz - V;\n" +
                    "vec3 L = normalize(D);\n" +
                    //"float dist = length(D);\n" +
                    //"float attenuation = calculateAttenuation(i, dist);\n" +
                    "float nDotL = dot(N,L);\n" +
                    "if (nDotL > 0.0) {\n" +
                        "diffuse  += gl_LightSource[i].diffuse  * nDotL;\n";
                        if(!key.nospecular) {
                            out += 
                            "vec3 E = normalize(-V);\n" +
                            "vec3 R = reflect(-L, N);\n" +
                            "float pf = pow(max(dot(R,E), 0.0), shininess);\n" +
                            "specular += gl_LightSource[i].specular * pf;\n";
                        }
                    out += "}\n" +
                    "ambient  += gl_LightSource[i].ambient;\n" +
                "}\n";
        }
        if(spot_att) {
            out +=
                "void spotLightAtt(in int i, in vec3 N, in vec3 V, in float shininess, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular) {\n" +
                    "vec3 D = gl_LightSource[i].position.xyz - V;\n" +
                    "vec3 L = normalize(D);\n" +
                    "float dist = length(D);\n" +
                    "float attenuation = calculateAttenuation(i, dist);\n" +
                    "float nDotL = dot(N,L);\n" +
                    "if (nDotL > 0.0) {	\n" +
                            "float spotEffect = dot(normalize(gl_LightSource[i].spotDirection), -L);\n" +
                            "if (spotEffect > gl_LightSource[i].spotCosCutoff) {\n" +
                                    "attenuation *=  pow(spotEffect, gl_LightSource[i].spotExponent);\n" +
                                    "diffuse  += gl_LightSource[i].diffuse  * attenuation * nDotL;\n";
                                    if(!key.nospecular) {
                                        out += 
                                        "vec3 E = normalize(-V);\n" +
                                        "vec3 R = reflect(-L, N);\n" +
                                        "float pf = pow(max(dot(R,E), 0.0), shininess);\n" +
                                        "specular += gl_LightSource[i].specular * attenuation * pf;\n";
                                    }
                            out += "}\n" +
                    "}\n" +
                    "ambient  += gl_LightSource[i].ambient * attenuation;\n" +
                "}\n";
        }
        if(spot) {
            out +=
                "void spotLight(in int i, in vec3 N, in vec3 V, in float shininess, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular) {\n" +
                    "vec3 D = gl_LightSource[i].position.xyz - V;\n" +
                    "vec3 L = normalize(D);\n" +
                    //"float dist = length(D);\n" +
                    //"float attenuation = calculateAttenuation(i, dist);\n" +
                    "float nDotL = dot(N,L);\n" +
                    "if (nDotL > 0.0) {	\n" +
                            "float spotEffect = dot(normalize(gl_LightSource[i].spotDirection), -L);\n" +
                            "if (spotEffect > gl_LightSource[i].spotCosCutoff) {\n" +
                                    //"attenuation *=  pow(spotEffect, gl_LightSource[i].spotExponent);\n" +
                                    "diffuse  += gl_LightSource[i].diffuse  * nDotL;\n";
                                    if(!key.nospecular) {
                                        out += 
                                        "vec3 E = normalize(-V);\n" +
                                        "vec3 R = reflect(-L, N);\n" +
                                        "float pf = pow(max(dot(R,E), 0.0), shininess);\n" +
                                        "specular += gl_LightSource[i].specular * pf;\n";
                                    }
                            out += "}\n" +
                    "}\n" +
                    "ambient  += gl_LightSource[i].ambient;\n" +
                "}\n";
        }
        return out;
    }

    @Override
    public String getFragBody1(ShaderKey key, HashMap<String, String> variables) {
        String out = "";
            if(key.hasLight()) {
                out += "vec3 N = normalize(normal);\n" +
                        "vec4 ambient  = vec4(0.0);\n" +
                        "vec4 diffuse  = vec4(0.0);\n" +
                        "vec4 specular = vec4(0.0);\n";

                for(int i = 0; i<key.light.length; i++) {
                    if(key.light[i]==1) {
                        out += "directionalLight("+i+", N, gl_FrontMaterial.shininess, ambient, diffuse, specular);\n";
                    } else if(key.light[i]==2) {
                        if(key.attenuate[i]) {
                            out += "pointLightAtt("+i+", N, vertex, gl_FrontMaterial.shininess, ambient, diffuse, specular);\n";
                        } else {
                            out += "pointLight("+i+", N, vertex, gl_FrontMaterial.shininess, ambient, diffuse, specular);\n";
                        }
                    } else if(key.light[i]==3) {
                        if(key.attenuate[i]) {
                            out += "spotLightAtt("+i+", N, vertex, gl_FrontMaterial.shininess, ambient, diffuse, specular);\n";
                        } else {
                            out += "spotLight("+i+", N, vertex, gl_FrontMaterial.shininess, ambient, diffuse, specular);\n";
                        }

                    }
                }
                out += 
                    "vec4 color = gl_LightModel.ambient  +\n" +
                         "(ambient  * gl_FrontMaterial.ambient) +\n" +
                         "(diffuse  * gl_FrontMaterial.diffuse) +\n" +
                         "(specular * gl_FrontMaterial.specular);\n" +
                    "color = clamp(color, 0.0, 1.0);\n";
            } else {
                out += "vec4 color = gl_LightModel.ambient;\n";
            }
        // provides color
        variables.put("color", "color");
        return out;
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
