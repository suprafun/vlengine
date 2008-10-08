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
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.state.AlphaBlendState;
import com.vlengine.scene.state.AlphaTestState;
import com.vlengine.scene.state.AlphaTestState.TestFunction;
import com.vlengine.scene.state.CullState;
import com.vlengine.scene.state.LightState;
import com.vlengine.scene.state.MaterialState;
import com.vlengine.scene.state.MaterialState;
import com.vlengine.scene.state.TextureState;
import com.vlengine.util.FastList;

/**
 * All the material state manipulation takes place here.
 * 
 * @author vear (Arpad Vekas)
 */
public class DefaultMaterialLib extends MaterialLib {

    // renderstates reausable for more materials
    // alpha states with different test values
    private final FastList<AlphaTestState> alphaTest=new FastList<AlphaTestState>();
    //private final FastList<AlphaBlendState> alphaTestBlend=new FastList<AlphaBlendState>();

    // only alpha blended renderstate
    private AlphaBlendState alphaBlend;
    
    // backface culling disabled state
    private CullState noCullState;
    
    // materialstate
    private MaterialState matState;
    
    private AlphaBlendState glassAlpha;
    private MaterialState glassMatState;
    private Material FFP_GLASS;
    
    private TextureState noTextureState;
    
    private TextureState createDisabledTextureState() {
        if( noTextureState == null) {
            noTextureState = new TextureState();
            noTextureState.setEnabled(false);
        }
        return noTextureState;
    }
    
    private AlphaTestState createAlphaTestState(MatParameters p ) {
        
        // extract parameters
        float testValue = p.getFloat(ALPHATEST, 1.0f);
        AlphaTestState.TestFunction testFunc = (TestFunction) p.get(ALPHAFUNC, AlphaTestState.TF_GREATER);
        
        // try to find a state that already exists
        AlphaTestState as=null;
        for(int i=0, mx=alphaTest.size(); i<mx; i++) {
            as=alphaTest.get(i);
            if( ((int)as.getReference()*255) == (int)(testValue*255) 
                    && as.getTestFunction() == testFunc ) {
                return as;
            }
        }
        
        // not found create
        as= new AlphaTestState();
        as.setReference(testValue);
        as.setTestFunction(testFunc);
        
        /*
        as.setSrcFunction(AlphaBlendState.SB_SRC_ALPHA);
        as.setDstFunction(AlphaBlendState.DB_ONE_MINUS_SRC_ALPHA);
            
        as.setBlendEnabled(false);
        as.setTestEnabled(true);
         */
        as.setEnabled(true);
        alphaTest.add(as);
        
        return as;
    }
    
    /*
    private AlphaBlendState createAlphaTestBlendState(MatParameters p ) {
        
        float testValue = p.getFloat(ALPHATEST, 1.0f);
        int testFunc = p.getInt(ALPHAFUNC, AlphaBlendState.TF_GREATER);
        
        // try to find a state that already exists
        AlphaBlendState as=null;
        for(int i=0, mx=alphaTestBlend.size(); i<mx; i++) {
            as=alphaTestBlend.get(i);
            if( ((int)as.getReference()*255) == (int)(testValue*255) 
                    && as.getTestFunction() == testFunc ) {
                return as;
            }
        }
        
        // not found create
        as= new AlphaBlendState();
        as.setReference(testValue);
        as.setTestFunction(testFunc);
        
        as.setSrcFunction(AlphaBlendState.SB_SRC_ALPHA);
        as.setDstFunction(AlphaBlendState.DB_ONE_MINUS_SRC_ALPHA);
            
        as.setBlendEnabled(true);
        as.setTestEnabled(true);
        as.setEnabled(true);
        alphaTestBlend.add(as);
        
        return as;
    }
     */

    private CullState createNoCullState() {
        if( noCullState == null ) {
            noCullState = new CullState();
            noCullState.setCullMode(CullState.CS_NONE);
            noCullState.setEnabled(true);
        }
        return noCullState;
    }
    
    private AlphaBlendState createAlphaBlendState() {
        if( alphaBlend == null ) {
            alphaBlend = new AlphaBlendState();
            alphaBlend.setSourceFunction(AlphaBlendState.SB_SRC_ALPHA);
            alphaBlend.setDestinationFunction(AlphaBlendState.DB_ONE_MINUS_SRC_ALPHA);
            alphaBlend.setEnabled(true);
        }
        return alphaBlend;
    }
    
    private AlphaBlendState createGlassAlphaState() {
        if( glassAlpha == null ) {
            glassAlpha = new AlphaBlendState();
            glassAlpha.setSourceFunction(AlphaBlendState.SB_DST_COLOR);//.SB_ONE_MINUS_DST_COLOR);//.SB_DST_COLOR);
            glassAlpha.setDestinationFunction(AlphaBlendState.DB_ONE_MINUS_SRC_ALPHA);//.DB_ONE_MINUS_SRC_ALPHA);
            glassAlpha.setEnabled(true);
        }
        return glassAlpha;
    }
    
    private MaterialState createGlassMaterialState() {
        if( glassMatState == null ) {
            glassMatState = new MaterialState();
            glassMatState.setEnabled(true);
            glassMatState.getAmbient().set(0.5f, 0.5f, 0.5f, 0.1f);
            //glassMatState.getDiffuse().set(0.2f, 0.2f, 0.2f, 0.5f);
            glassMatState.getSpecular().set(1f, 1f, 1f, 1f);
            glassMatState.getEmissive().set(0.1f, 0.1f, 0.1f, 0.5f);
            glassMatState.setShininess(120);
        }
        return glassMatState;
    }
    
    private MaterialState createMaterialState(MatParameters p) {
        if( matState == null ) {
            matState = new MaterialState();
            matState.setEnabled(true);
            matState.setAmbient(new ColorRGBA(.5f, .5f, .5f, 1f));
            //matState.setAmbient(new ColorRGBA(0f, 0f, 0f, 1f));
            matState.setDiffuse(new ColorRGBA(.8f, .8f, .8f, 1f));
            matState.setSpecular(ColorRGBA.white.clone()); 
            matState.setShininess(0f);
        }
        MaterialState mat = matState;
        if(p.containsKey(DISSOLVE)
            || p.containsKey(AMBIENT)
            || p.containsKey(DIFFUSE)
            || p.containsKey(SPECULAR)
            || p.containsKey(SHININESS)
            || p.containsKey(TRANSMISSIVE)
            || p.containsKey(EMISSIVE)
            ) {
            // we need a custom materialstate
            mat = new MaterialState();
            mat.setEnabled(true);
            // ambient
            if(p.containsKey(AMBIENT) && !p.getBoolean(NOSPECULAR, false)) {
                mat.setAmbient((ColorRGBA)p.get(AMBIENT));
            } else {
                // default ambient
                mat.setAmbient(matState.getAmbient().clone());
            }
            // diffuse
            if(p.containsKey(DIFFUSE) && !p.getBoolean(NOSPECULAR, false)) {
                mat.setDiffuse((ColorRGBA)p.get(DIFFUSE));
            } else {
                // default ambient
                mat.setDiffuse(matState.getDiffuse().clone());
            }
            // specular
            
            if(p.containsKey(SPECULAR) && !p.getBoolean(NOSPECULAR, false)) {
                mat.setSpecular((ColorRGBA)p.get(SPECULAR));
            } else {
                // default ambient
                mat.setSpecular(matState.getSpecular().clone());
            }
            if(p.containsKey(EMISSIVE)) {
                mat.setEmissive((ColorRGBA)p.get(EMISSIVE));
            } else {
                // default emissive
                mat.setEmissive(matState.getEmissive().clone());
            }
            /*
            if(p.containsKey("transmissive")) {
                ColorRGBA transmissive = (ColorRGBA) p.get("transmissive");
                mat.getAmbient().multLocal(transmissive);
                mat.getDiffuse().multLocal(transmissive);
                // TODO: others
            }
             */
            // dissolve
            if(p.containsKey(DISSOLVE)) {
                float diss = p.getFloat(DISSOLVE, 1.0f);
                mat.getDiffuse().a *= diss;
                mat.getSpecular().a *= diss;
                mat.getAmbient().a *= diss;
            }
            float shine = p.getFloat(SHININESS, 18.0f);
            mat.setShininess(shine);
        }
        return mat;
    }
        
    /**
     * FF = Fixed Function (no shader)
     * MT = Multi Texture
     * AB = Alpha Blend
     * 
     * @param p
     * @return
     */
    public Material FF_MT_AB( MatParameters p ) {
        // get an opaque material
        Material mat = FF_MT_OP(p);
        if (mat == null)
            mat = new Material();

        // apply alpha blending
        if( mat!= null ) {
            // alpha test
            boolean alphatest = p.get(ALPHATEST) != null;
            if( alphatest  ) {
                AlphaTestState aTest = createAlphaTestState(p);
                mat.setRenderState(aTest);
            }
            AlphaBlendState aBlend = createAlphaBlendState();
            mat.setRenderState(aBlend);
            //if(mat.getRenderQueueMode()!=RenderQueue.QueueFilter.AlphaBlended.value
            //      && alphatest)
            //    mat.setRenderQueueMode(RenderQueue.QueueFilter.AlphaTested.value);
            mat.setRenderQueueMode(RenderQueue.QueueFilter.AlphaBlended.value);
        }
        return mat;
    }

    /**
     * Glass texture.
     * 
     * FFP = Fixed Function Pipeline
     * 
     * @param p
     * @return
     */
    public Material FFP_GLASS( MatParameters p ) {
        //TODO: glass should be rendered in two passes:
        // from OpenGL org
        //First render all opaque objects in your scene. 
        //Disable lighting, enable blending, and render your glass geometry with a small alpha value. 
        //This should result in a faint rendering of your object in the framebuffer. 
        //(Note: You may need to sort your glass geometry, so it's rendered in back to front Z order.)

        //Now, you need to add the specular highlight. 
        //Set your ambient and diffuse material colors to black, and your specular material and light colors 
        //to white. Enable lighting. Set glDepthFunc(GL_EQUAL), then render your glass object a second time.
       
        Material mat = new Material();

        //AlphaState alpha = createGlassAlphaState();
        //mat.setRenderState(createAlphaBlendState());
        mat.setRenderState(createAlphaBlendState());
        
        mat.setRenderState(createMaterialState(p));
        
        if(p.getBoolean(DefaultMaterialLib.NOCULL, false))
            mat.setRenderState(createNoCullState());
        //else
        mat.setRenderQueueMode(RenderQueue.QueueFilter.AlphaBlended.value);
        ColorRGBA transmissive = (ColorRGBA) p.get(TRANSMISSIVE);
        if(transmissive!=null) {
            mat.getDefaultColor().set(transmissive);
        } else {
            mat.getDefaultColor().set(0.5f, 0.5f, 0.5f, 0.1f);
            
            
        }
            mat.setLightCombineMode(LightState.OFF);
        mat.setRenderState(createDisabledTextureState());
        return mat;
    }

    /**
     * FF = Fixed Function (no shader)
     * MT = Multi Texture
     * OP = Opaque
     * 
     * @param p
     * @return
     */
    public Material FF_MT_OP( MatParameters p ) {
        // check if animating texture needs to be done
        if( p.getInt(ANIM_FRAMES, 0)==0 ) {
            
            // not animating, construct a simple two-texture material
            Material mat = new Material();
            // create a texturestate
            TextureState ts;

            
            // first texture
            Texture t0=(Texture) p.get(DIFFUSE0MAP);
            Texture t1=(Texture) p.get(DIFFUSE1MAP);
            if( t0 == null && t1 == null) {
                // if no textures are used, reuse the disabled texture state
                ts=createDisabledTextureState();
            } else {
                ts=new TextureState();
                ts.setEnabled(true);
                if(t0!=null) {
                    ts.setTexture(t0,0);
                    ts.setEnabled(true);
                }
                if(t1!=null) {
                    ts.setTexture(t1,1);
                    ts.setEnabled(true);
                }
            }
            // set the texturestate to the material
            mat.setRenderState(ts);
            
            // do we need alpha testing
            boolean alpha = p.getFloat(ALPHATEST, -1.0f)!=-1.0f;
            
            if( alpha ) {
                AlphaTestState as = createAlphaTestState(p);
                mat.setRenderState(as);
                mat.setRenderQueueMode(RenderQueue.QueueFilter.AlphaTested.value);
                //mat.setRenderQueueMode(RenderQueue.FILTER_OPAQUE);
                //mat.setRenderQueueMode(RenderQueue.FILTER_TWOSIDED);
                //mat.setRenderState(this.createNoCullState());
            }
                
            // do we disable backface cull?
            if(p.getBoolean(NOCULL, false)) {
                /*
                if(alpha)
                    mat.setRenderQueueMode(RenderQueue.FILTER_TWOSIDED);
                else
                 */
                    mat.setRenderState(this.createNoCullState());
            }
            
            // add a materialstate
            mat.setRenderState(createMaterialState(p));
            
            return mat;
        } else {
            // TODO: implement animating textures
            // if an array is given, a texture animating material is constructed
            // frames +1 Materials are constructed
            // AnimatingMaterial with a proper controller
        }
        
        return null;
    }
    
    @Override
    public Material getMaterial(String name, MatParameters params) {
        Material mat = null;
        if(params.containsKey(TRANSMISSIVE)) {
            mat= FFP_GLASS(params);
        } else if("FF_MT_AB".equals(name)) {
            mat= FF_MT_AB(params);
        }  else {
            mat= FF_MT_OP(params);
        }
        if(mat!=null)
            return mat;
        return super.getMaterial(name, params);
    }
}
