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

import com.vlengine.image.Texture;
import com.vlengine.math.FastMath;
import com.vlengine.math.Vector3f;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.animation.x.XAnimatedItem;
import com.vlengine.scene.animation.x.XGPUAnimatedItem;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.ShaderParameters;
import com.vlengine.scene.state.TextureState;
import com.vlengine.scene.state.shader.ShaderTexture;
import com.vlengine.scene.state.shader.ShaderVariableFloat2;
import com.vlengine.scene.state.shader.ShaderVariableInt;
import com.vlengine.scene.state.shader.ShaderVertexAttribute;
import com.vlengine.scene.state.shader.ShaderVertexAttributeFloat;
import com.vlengine.util.geom.VertexAttribute;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ShaderMaterialLib extends MaterialLib {

    protected HashMap<ShaderKey, ShaderObjectsState> shaderCache = new HashMap<ShaderKey, ShaderObjectsState>();
    
    //int[] lightType = new int[Renderable.LOWPROFILE_LIGHTS];
    //boolean[] shadow = new boolean[Renderable.LOWPROFILE_LIGHTS];
    //boolean[] attenuation = new boolean[Renderable.LOWPROFILE_LIGHTS];
    
    protected ShaderKey createShaderKey(MatParameters params, ShaderKey store) {
        ShaderKey key = store;
        if(key==null)
            key = new ShaderKey();
        // do we have a diffuse 0 texture?
        if(params.get(DIFFUSE0MAP)!=null) {
            // mark it
            key.colorMap0 = true;
            Texture t = (Texture) params.get(DIFFUSE0MAP);
            Vector3f tscale = t.getScale();
            if( tscale != null && ( tscale.x != 1 || tscale.y != 1)) {
                // we have a texture scale
                // TODO: enable
                key.colorMap0Scale = true;
            }
        }
        key.normalType = ShaderKey.NORMALTYPE_NO;
        
        // optimize the key, if no light is requested, then no normal is needed
        if(key.lightIndex || key.hasLight()) {
            key.normalType = ShaderKey.NORMALTYPE_VERTEX;
        }
        
        // do we have normal map
        if(params.get(NORMALMAP)!=null) {
            key.normalType = ShaderKey.NORMALTYPE_MAP;
            /*
            Texture t = (Texture) params.get(NORMALMAP);
            Vector3f tscale = t.getScale();
            if( tscale != null && ( tscale.x != 1 || tscale.y != 1)) {
                // we have a texture scale
                // TODO: enable
                key.normalMapScale = true;
            }
             */
        }
        // do we have bump map?
        if(params.get(BUMPMAP)!=null) {
            key.bumpMap = true;
            /*
            Texture t = (Texture) params.get(NORMALMAP);
            Vector3f tscale = t.getScale();
            if( tscale != null && ( tscale.x != 1 || tscale.y != 1)) {
                // we have a texture scale
                // TODO: enable
                key.bumpMapScale = true;
            }
             */
        }
        // do we have a bone animated model
        XGPUAnimatedItem xa = (XGPUAnimatedItem) params.get(BONES);
        if(xa!=null) {
            key.numBones = xa.getMatrixBuffer().limit();
        }

        key.nospecular = params.getBoolean(NOSPECULAR, false);
        key.screendepthfog = params.getBoolean(SCREENDEPTHFOG, false);
        return key;
    }

    // the sources
    BaseShaderSource baseSource = new BaseShaderSource();
    SkinShaderSource skinSource = new SkinShaderSource();
    FogShaderSource fogSource = new FogShaderSource();
    TextureShaderSource textureSource = new TextureShaderSource();
    PhongShaderSource phongSource = new PhongShaderSource();
    TBNPhongShaderSource bumpSource = new TBNPhongShaderSource();
    HashMap<String,String> variables = new HashMap<String,String>();
    CompositeShaderSource css = new CompositeShaderSource();
    
    protected ShaderObjectsState createShader(ShaderKey key) {
        if(!key.colorMap0) {
            key.colorMap0Scale = false;
        }
        ShaderObjectsState shader = shaderCache.get(key);
        if(shader==null) {
            // the shader is unknown yet, create it
            // start a stringbuffer
            // vertex shader
            // header
            StringBuffer vsh = new StringBuffer();
            // main
            StringBuffer vsm = new StringBuffer();
            
            // fragment shader
            // header
            StringBuffer fsh = new StringBuffer();
            // main
            StringBuffer fsm = new StringBuffer();
            
            // do we operate TNB lighting shaders or normal
            boolean tbn = isTNB(key);
                    
            // by different options, go over the ources and append their ouput
            // create vaiables
            variables.clear();
            css.clear();
            css.addSource(baseSource);
            // vert declarations
            if(key.numBones>0)
                css.addSource(skinSource);
            
            if(key.colorMap0)
                        css.addSource(textureSource);
            
            if(key.forwardLigting) {
                if(tbn) {
                    // tbn lighting
                    css.addSource(bumpSource);
                } else {
                    css.addSource(phongSource);
                }
            }
            if(key.screendepthfog)
                css.addSource(fogSource);
            
            // collect all the sources
            vsh.append(css.getVertDeclarations(key, variables));
            vsm.append(css.getVertBody1(key, variables));
            vsm.append(css.getVertBody2(key, variables));
            vsm.append(css.getVertBody3(key, variables));
            vsm.append(css.getVertBodyEnd(key, variables));
            fsh.append(css.getFragDeclarations(key, variables));
            fsh.append(css.getFragFunctions(key, variables));
            fsm.append(css.getFragBody1(key, variables));
            fsm.append(css.getFragBody2(key, variables));
            fsm.append(css.getFragEnd(key, variables));
            
            // create a shader
            shader = new ShaderObjectsState();
            shader.setEnabled(true);
            String vertShader = vsh.append(vsm).toString();
            String fragShader = fsh.append(fsm).toString();
            
            shader.load(vertShader, fragShader);
            
            shader.setShaderKey(key.clone());
            // put the shader into the cache
            shaderCache.put(shader.getShaderKey(), shader);
        }
        return shader;
    }
    
    protected boolean isTNB(ShaderKey key) {
        return key.normalType == ShaderKey.NORMALTYPE_MAP || key.bumpMap;
    }
    
    @Override
    public Material getMaterial(String name, MatParameters params) {
        Material mat = null;
        
        //this should be false if we dont have vertex normal
        // get data needed from parameters
        boolean hasVertexNormal = true;
        Texture t0=(Texture) params.get(DIFFUSE0MAP);
        // bones
        XGPUAnimatedItem xa = (XGPUAnimatedItem) params.get(MaterialLib.BONES);
        // disable for testing
        //xa=null;
        // do we need shader?
        if(name.startsWith("VS") || xa != null || params.getBoolean(PERPIXEL, false)) {
            // get material from parent, which will set alpha and material states
            mat = parent.getMaterial(name, params);
            // TODO: do we need this?
            if(
                    //( 
                    mat.states[RenderState.RS_ALPHABLEND]!=null 
                    //|| mat.states[RenderState.RS_ALPHATEST]!=null ) 
                    && xa ==null)
                return mat;
            mat.states[RenderState.RS_TEXTURE] = null;
            //mat = new Material();
            ShaderObjectsState so;
            
            // construct a shaderkey
            ShaderKey key = createShaderKey(params, null);
            
            // create shader for every possible usage
            key.light[0] = 0;
            key.normalType = ShaderKey.NORMALTYPE_NO;
            key.colorMap0 = false;
            // depth only shader
            key.depthOnly = true;
            key.forwardLigting = false;
            key.screendepthfog = false;
            // create the different versions of the shader, depending in which renderpass
            // the they will be used
            // TODO:
            so = createShader(key);
            mat.setShaderState(so);

            // create color-only version
            key = createShaderKey(params, null);
            key.light[0] = 0;
            key.normalType = ShaderKey.NORMALTYPE_NO;
            key.depthOnly = false;
            key.forwardLigting = false;
            so = createShader(key);
            mat.setRenderState(so);
            
            // create unlit version
            key = createShaderKey(params, null);
            key.light[0] = 0;
            key.normalType = ShaderKey.NORMALTYPE_NO;
            key.forwardLigting = true;
            so = createShader(key);
            mat.setShaderState(so);
            
            key = new ShaderKey();
            
            // create versions with different light types
            // bits used by single light
            int bitsused = 3 + (ShaderKey.LIGHTSORTER_SHADOWS? 1 : 0);
            
            int allbits = (2<<(bitsused*Renderable.LOWPROFILE_LIGHTS))-1;
            for(int i=1; i<allbits; i++) {
                
                //Arrays.fill(lightType, 0);
                //Arrays.fill(shadow, false);
                //Arrays.fill(attenuation, false);
                
                //
                
                key.clear();
                // set the values based on id
                key.setById(i);
                
                // check if the ligts are consistent
                
                if(key.hasLight() && key.isLightConsistent() ) {
                    // fill in other data
                    key.depthOnly = false;
                    key.forwardLigting = true;
                    key = createShaderKey(params, key); 
                    // TODO: remove wehn bumpmapping implemented
                    //key.normalType = ShaderKey.NORMALTYPE_VERTEX;
                    so = createShader(key);
                    mat.setShaderState(so);
                }
            }

            // create shader parameters
            ShaderParameters sp=new ShaderParameters();
            /*
            if(xa.shaderParams==null) {
                
                //TODO: if we set in other data (textures) then we cannot use the same
                // animated item
                xa.shaderParams = sp;
            } else {
                sp = xa.shaderParams;
            }
             */
            sp.setEnabled(true);

            // if we have bones, put it in
            if(xa!=null) {
                xa.getMatrixBuffer().setName("boneMatrix");
                xa.getMatrixBuffer().transpose = false;
                sp.setUniform(xa.getMatrixBuffer());
                sp.setVertexAttribute(new ShaderVertexAttribute("boneWeight", VertexAttribute.USAGE_WEIGHTS));
                //bwgt.normalized = true;
                sp.setVertexAttribute(new ShaderVertexAttributeFloat("boneIndex", VertexAttribute.USAGE_WEIGHTINDICES));
                sp.setVertexAttribute(new ShaderVertexAttribute("weightNum", VertexAttribute.USAGE_NUMWEIGHTS));
            }
            mat.setRenderState(sp);

            // if we have a color texture, put in
            if(t0!=null) {
                /*
                TextureState ts = new TextureState();
                ts.setTexture(t0, 0);
                ts.setEnabled(true);
                mat.setRenderState(ts);
                 
                sp.setUniform(new ShaderVariableInt("colorMap", 0));
                 */
                
                sp.setTexture(new ShaderTexture("colorMap", t0));
                //Texture t = (Texture) params.get(DIFFUSE0MAP);
                Vector3f tscale = t0.getScale();
                if( key.colorMap0Scale) {
                    // we have a texture scale
                    sp.setUniform(new ShaderVariableFloat2("cMap0Scale", tscale.x, tscale.y ));
                }
            }
            
            boolean tnb = isTNB(key);
            
            // check for normal map
            Texture t1 = (Texture) params.get(NORMALMAP);
            if(t1 != null && isTNB(key)) {
                sp.setTexture(new ShaderTexture("normalMap", t1));
                //Texture t = (Texture) params.get(DIFFUSE0MAP);
                //Vector3f tscale = t0.getScale();
            }
            
            // TODO: add bump map
            
            // add TNB attributes
            if(tnb) {
                sp.setVertexAttribute(new ShaderVertexAttributeFloat("aTangent", VertexAttribute.USAGE_TANGENT));
                sp.setVertexAttribute(new ShaderVertexAttributeFloat("aBinormal", VertexAttribute.USAGE_BINORMAL));
            }
            
            mat.setRenderState(sp);

            // create materialstate
            
        }
        if(mat!=null)
            return mat;
        return super.getMaterial(name, params);
    }
}
