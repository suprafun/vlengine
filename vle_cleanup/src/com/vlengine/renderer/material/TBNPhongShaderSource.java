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
 * TB Space phong lighting shader with a single light
 * @author vear (Arpad Vekas)
 */
public class TBNPhongShaderSource extends ShaderSource {

    final String header1 = "attribute vec3 aTangent;\n";
    final String header2 = "attribute vec3 aBinormal;\n";
            
    @Override
    public String getVertDeclarations(ShaderKey key, HashMap<String, String> variables) {
        String out = "";
        out += header1 +
                header2;
        
        if(!key.nospecular && key.hasPointOrSpotLight()) {
            out += "varying vec3 vTBNV["+key.getBumpLightCount()+"];\n";
        }
        out += "varying vec3 vTBNL["+key.getBumpLightCount()+"];\n";
        if(key.hasAttenuate()) {
                out += "varying float vAttenuate["+key.getBumpLightCount()+"];\n";
        }
        if(key.hasSpotLight()) {
            out += "varying float vSpotEffect["+key.getBumpLightCount()+"];\n";
        }
        if(key.hasDirectionaLight() && !key.nospecular) {
            out += "varying vec3 vTBNH["+key.getBumpLightCount()+"];\n";
        }
        //   in single light mode we pass the light we pass the light from vertex shader
        return out;
    }

    @Override
    public String getVertBody1(ShaderKey key, HashMap<String, String> variables) {
        String out = "";
        if(variables.get("texcoord")==null) {
            out += "gl_TexCoord[0] = gl_MultiTexCoord0;\n";
            variables.put("texcoord", "gl_TexCoord[0]");
        }
        return out;
    }

    @Override
    public String getVertBody2(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getVertBody3(ShaderKey key, HashMap<String, String> variables) {
        String out = "";
        if(variables.get("normal") == null)
            variables.put("normal", "gl_Normal");
        // TODO: with animated models, since the normal is changing
        // this may not be good
        out += replaceAll("vec3 normal = gl_NormalMatrix * 'normal;\n", variables);
        out += "vec3 binormal       = gl_NormalMatrix * aBinormal;\n";
        out += "vec3 tangent        = gl_NormalMatrix * aTangent;\n";
        
        // vertex calculation
        if(variables.get("position")==null)
            variables.put("position", "gl_Vertex");
        out += replaceAll("vec3 V = vec3(gl_ModelViewMatrix * 'position );\n", variables);
        
        // this is single vertex calculation
        // done in the vertex shader to speed up stuff
        
        if(key.hasPointOrSpotLight()) {
            out += "vec3 D, L;\n";
            if(key.hasAttenuate()) {
                out += "float dist;\n";
            }
        }
        
        out += "vec3 tmpVec;\n";
        
        // light calculation
        // for each light
        for(int i=0, mi=key.getBumpLightCount(); i<mi; i++) {
            if(key.light[i]==1) {
                out += "tmpVec.x  = dot( tangent, gl_LightSource["+i+"].position.xyz );\n";
                out += "tmpVec.y  = dot( binormal, gl_LightSource["+i+"].position.xyz );\n";
                out += "tmpVec.z  = dot( normal, gl_LightSource["+i+"].position.xyz );\n";
                out += "vTBNL["+i+"] = normalize(tmpVec);\n";

                if(!key.nospecular) {
                    out += "tmpVec.x  = dot( tangent, gl_LightSource["+i+"].halfVector.xyz );\n";
                    out += "tmpVec.y  = dot( binormal, gl_LightSource["+i+"].halfVector.xyz );\n";
                    out += "tmpVec.z  = dot( normal, gl_LightSource["+i+"].halfVector.xyz );\n";
                    out += "vTBNH["+i+"] = normalize(tmpVec);\n";
                }
            } else {
                out += "D = gl_LightSource["+i+"].position.xyz - V;\n" +
                   "L = normalize(D);\n";
            
                if(key.light[i]==3) {
                    // spot effect
                    out += "vSpotEffect["+i+"] = dot(normalize(gl_LightSource["+i+"].spotDirection), -L);\n";
                }

                if(key.attenuate[i]) {
                    out += "dist = length(D);\n" +
                    "vAttenuate["+i+"] = 1.0 / (gl_LightSource["+i+"].constantAttenuation + gl_LightSource["+i+"].linearAttenuation * dist + gl_LightSource["+i+"].quadraticAttenuation * dist * dist);\n";
                }

                out += "tmpVec.x  = dot( tangent, L.xyz );\n";
                out += "tmpVec.y  = dot( binormal, L.xyz );\n";
                out += "tmpVec.z  = dot( normal, L.xyz );\n";
                out += "vTBNL["+i+"] = normalize(tmpVec);\n";
            
                if(!key.nospecular) {
                    out += "tmpVec.x = dot( tangent, V.xyz );\n";
                    out += "tmpVec.y  = dot( binormal, V.xyz );\n";
                    out += "tmpVec.z  = dot( normal, V.xyz );\n";
                    out += "vTBNV["+i+"] = normalize(tmpVec);\n";
                }
            }
        }

        return out;
    }

    @Override
    public String getVertBodyEnd(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getFragDeclarations(ShaderKey key, HashMap<String, String> variables) {
        String out = "";
        if(!key.nospecular && key.hasPointOrSpotLight()) {
            out += "varying vec3 vTBNV["+key.getBumpLightCount()+"];\n";
        }
        out += "varying vec3 vTBNL["+key.getBumpLightCount()+"];\n";
        if(key.hasAttenuate()) {
                out += "varying float vAttenuate["+key.getBumpLightCount()+"];\n";
        }
        if(key.hasSpotLight()) {
            out += "varying float vSpotEffect["+key.getBumpLightCount()+"];\n";
        }
        if(key.hasDirectionaLight() && !key.nospecular) {
            out += "varying vec3 vTBNH["+key.getBumpLightCount()+"];\n";
        }
        out += "uniform sampler2D normalMap;\n";
        //   in single light mode we pass the light we pass the light from vertex shader
        return out;
    }

    @Override
    public String getFragFunctions(ShaderKey key, HashMap<String, String> variables) {
        return "";
    }

    @Override
    public String getFragBody1(ShaderKey key, HashMap<String, String> variables) {
        String out = "";
        if(key.hasLight() && key.normalType == ShaderKey.NORMALTYPE_MAP) {
            out += ( "vec4 ambient  = vec4(0.0);\n" +
                    "vec4 diffuse  = vec4(0.0);\n" +
                    "vec4 specular = vec4(0.0);\n");
            
            // get normal from normal-map
            String scaleString = "";
            if(key.colorMap0Scale) {
                scaleString="*cMap0Scale.xy";
            } 
            String normFetch = "vec3 TBNN = normalize( ( texture2D( normalMap, 'texcoord.st"+scaleString+" ).xyz * 2.0 ) - 1.0 );\n";
            out += replaceAll(normFetch, variables);

            if(key.hasAttenuate()) {
                out += "float attenuate;\n";
            }

            if(key.hasSpotLight()) {
                out += "float spotEffect ;\n";
            }

            // per light processing
            
            //if(!key.nospecular && key.hasPointOrSpotLight()) {
            //    out += "vec3 TBNV["+key.getLightCount()+"];\n";
            //}

            if(!key.nospecular) {
                out += "float shininess = gl_FrontMaterial.shininess;\n";
            }

            out += "vec3 TBNL;\n";
            out += "float nDotL;\n";
            
            if(!key.nospecular) {
                if(key.hasDirectionaLight()) {
                    out += "vec3 TBNH;\n";
                } 
                if(key.hasPointOrSpotLight()) {
                    out += "vec3 TBNV;\n";
                }
            }
            
            //out += "vec3 TBNV = vTBNV;\n";
            for(int i=0, mi=key.getBumpLightCount(); i<mi; i++) {
                if(!key.nospecular) {
                    if(key.light[i]==1) {
                        out += "TBNH = normalize(vTBNH["+i+"]);\n";
                    } else {
                        out += "TBNV = vTBNV["+i+"];\n";
                    }
                }
                
                //out += "float nDotL;\n";
                // we have a single light
                out += "TBNL = normalize(vTBNL["+i+"]);\n" +
                    "nDotL = dot(TBNN, TBNL);\n" +
                    "if (nDotL > 0.0) {\n";
            
                    if(key.attenuate[i]) {
                            out += "attenuate = vAttenuate["+i+"];\n";
                    }
                    if(key.light[i]==3) {
                        // spot check
                        out += "spotEffect = vSpotEffect["+i+"];\n";
                        //"float spotEffect = dot(normalize(gl_LightSource[i].spotDirection), -L);\n" +
                        out += "if (spotEffect > gl_LightSource["+i+"].spotCosCutoff) {\n";
                        if(key.attenuate[i]) {
                            out += "attenuate *=  pow(spotEffect, gl_LightSource["+i+"].spotExponent);\n";
                        }
                    }
                    out += "diffuse  += gl_LightSource["+i+"].diffuse  * nDotL";
                    if(key.attenuate[i])
                        out += "* attenuate";
                    out += ";\n";
                    
                    if(!key.nospecular) {
                        if(key.light[i]==1) {
                            // specular for directional
                            out += 
                                    // TODO: bad, this needs to be calculated in vertex shader too
                            //"vec3 H = normalize(TBNH);\n" +
                            "float pf = pow(max(dot(TBNN,TBNH), 0.0), shininess);\n" +
                            "specular += gl_LightSource["+i+"].specular * pf;\n";
                        } else {
                            // specular for spot and point
                            out +=
                            "vec3 E = normalize(-TBNV);\n" +
                            "vec3 R = reflect(-TBNL, TBNN);\n" +
                            "float pf = pow(max(dot(R,E), 0.0), shininess);\n" +
                            "specular += gl_LightSource["+i+"].specular * pf";
                            if(key.attenuate[i]) {
                                out += " * attenuate";
                            }
                            out += ";\n";
                        }
                    }

                    if(key.light[i]==3) {
                        out += "}\n";
                    }
                 out +=
                    "}\n" +
                    "ambient  += gl_LightSource["+i+"].ambient";
                    if(key.attenuate[i]) {
                        out += "* attenuate";
                    }
                    out += ";\n";                
                
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
