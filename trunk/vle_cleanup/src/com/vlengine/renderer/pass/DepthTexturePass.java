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

package com.vlengine.renderer.pass;

import com.vlengine.image.Texture;
import com.vlengine.math.Matrix4f;
import com.vlengine.math.Vector3f;
import com.vlengine.model.Quad;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.TextureRenderer;
import com.vlengine.renderer.lwjgl.LWJGLCamera;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.ColorMaskState;
import com.vlengine.scene.state.CullState;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.ZBufferState;
import com.vlengine.scene.state.lwjgl.LWJGLTextureState;
import com.vlengine.light.ShadowPart;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.Renderer;
import com.vlengine.renderer.material.Material;
import com.vlengine.renderer.material.ShaderKey;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.system.DisplaySystem;
import com.vlengine.util.FastList;

/**
 * This pass is rensposible for rendering the scene (the ortho pass)
 * from a lights view into a depth texture. That texture can be used
 * later for shadowing in the ortho pass.
 * 
 * @author vear (Arpad Vekas)
 */
public class DepthTexturePass extends RenderPass {
    
    // the dimensions of the depth map texture
    private int dimension = 256;
    // the depth bias to apply
    private float depth_scale = 2f;
    private float depth_bias = 4f;
    
    // texture renderer used to render from lights view
    private TextureRenderer tRenderer;
    // texture to hold the depth map
    private Texture shadowTexture;
    
    // the light batch that is casting the shadow
    private boolean supported = true;
    private boolean isinited = false;
    
    // the shadow we are rendering our shadowmap to
    protected ShadowPart shadow = null;
    private Matrix4f bias;

    public DepthTexturePass() {
        this("lightview");
    }
    
    public DepthTexturePass( String name ) {
        super(name);
        // default parameters for this pass
        // this pass uses only depth information from material
        // TODO:
        setUsedMaterialFlags(0);
        // renders queue 0 (opaque geometry)
        setQueueNo(RenderQueue.StandardQueue.Opaque.queuId);
        // it does not collect anything from the scene (let the normal opaque pass collect)
        setQueueFilter(0);
    }

    /**
     * Set the texture size used for the depth texture. Only has effect before
     * this DepthTextureRenderer is initialized.
     * @param dimesion      The depth texture will be dimension*dimension size
     */
    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public int getDimension() {
        return dimension;
    }
    
    public void setTexture(Texture tex) {
        shadowTexture = tex;
    }

    public void setShadowPart(ShadowPart shadowPart) {
        this.shadow = shadowPart;
        this.shadowTexture = shadow.getTexture();
        this.camera =  shadow.getCamera();
    }

    protected void initialize(RenderContext ctx) {
        if(tRenderer==null) {
            tRenderer = 
              DisplaySystem.getDisplaySystem().createTextureRenderer(dimension, dimension, 
              TextureRenderer.RENDER_TEXTURE_2D);
            if (tRenderer==null || !tRenderer.isSupported()) {
                this.supported = false;
                return;
            }
            tRenderer.setMultipleTargets(true);
            tRenderer.setBackgroundColor(new ColorRGBA(0.0f, 0.0f, 0.0f, 1.0f));
            //tRenderer.setCamera(camera);
        }

        if(shadowTexture==null)
            shadowTexture = new Texture();
        if(shadowTexture.getTextureId()==0) {
            shadowTexture.setWrap(Texture.WM_CLAMP_S_CLAMP_T);
            shadowTexture.setFilter(Texture.FM_NEAREST);
            shadowTexture.setMipmapState(Texture.MM_NONE);//.MM_LINEAR);//.MM_NEAREST);
            shadowTexture.setRTTSource(Texture.RTT_SOURCE_DEPTH);
            //shadowTexture.setAnisoLevel(1f);
            tRenderer.setupTexture(shadowTexture);
        }
        
        camera.resize(dimension, dimension);
    }

    /**
     * Release pbuffers in TextureRenderer's. Preferably called from user cleanup method.
     */
    @Override
    public void cleanup() {
        if (tRenderer != null)
            tRenderer.cleanup();
    }

    public boolean isSupported() {
            return supported;
    }

    @Override
    public void renderPass(RenderContext ctx) {
        if(!enabled)
            return;
        
        Renderer renderer = ctx.getRenderer();
        
        // initialize if not yet
        initialize(ctx);

        // if not supported, do nothing
        if(!supported)
            return;

        // get the list of batches that we will render, if we only need to
        // work on those
        FastList<Renderable> list = ctx.getRenderQueue().getQueue(this.queueNo);
        if ( list==null || list.size() == 0 ) {
            return;
        }
        
        // refresh the depth texture
        //Render scene to texture
        
        tRenderer.beginRender(ctx, shadowTexture, true); // maybe we need true here?
        renderer.setCamera(camera);
        
        // set some states, like in the DepthPass
        applyStates(ctx);
        
        // apply bias
        renderer.setPolygonOffset(depth_scale, depth_bias);
        
        // render the renderable list to the texture
        // as with the depth pass, only position data is required
        renderer.setPositionOnlyMode(true);
        // draw all the elements
        for(int i=0, ls=list.size(); i<ls; i++) {
            Renderable e= list.get(i);
            // TODO: maybe we still need shaders because of vertex animation?
            e.draw(ctx);
        }
        renderer.setPositionOnlyMode(false);

        renderer.clearPolygonOffset();
        
        // if we are processing a shadow texture
        // do before reseting the camera back to the scene camera
        if(shadow!=null) {
            LWJGLCamera cam = (LWJGLCamera) tRenderer.getParentRenderer().getCamera();
            // store the used camera matrices in the shadowpart object
            shadow.getProjection().set(cam.getProjectionMatrix()).transposeLocal();
            shadow.getView().set(cam.getModelViewMatrix()).transposeLocal();
            
            // prepare the shadow texture
            shadowTexture.setEnvironmentalMapMode(Texture.EM_EYE_LINEAR);
            // apply bias?
            
            if(bias == null) {
                bias = new Matrix4f();
                bias.set(new float[] {
                    0.5f, 0.0f, 0.0f, 0.0f, 
		    0.0f, 0.5f, 0.0f, 0.0f,
		    0.0f, 0.0f, 0.5f, 0.0f,
		    0.5f, 0.5f, 0.5f, 1.0f}, false);
            }
            
            // apply transformation matrices
            if(shadowTexture.getMatrix()==null) {
                shadowTexture.setMatrix(new Matrix4f());
            }
            shadowTexture.getMatrix().set(bias);
            shadowTexture.getMatrix().multLocal(shadow.getProjection());
            //shadowTexture.getMatrix().set(shadow.getProjection());
            shadowTexture.getMatrix().multLocal(shadow.getView());
            shadow.getLightMatrix().set(shadowTexture.getMatrix());
        }
        
        // restore previous states
        restoreStates(ctx);

        tRenderer.endRender();
        renderer.reset();
    }
    
    // states to restore
    RenderState oldz;
    RenderState oldcm;
    RenderState oldcull;

    // states to apply
    ZBufferState zs;
    ColorMaskState cms;
    CullState cs;

    // the saved states, and the states to be allyed
    protected void applyStates(RenderContext ctx) {
        Renderer renderer = ctx.getRenderer();
        
        // save the default ZState
        oldz = ctx.defaultStateList[RenderState.RS_ZBUFFER];
        
        if( zs == null ) {
            zs = (ZBufferState) renderer.createState(RenderState.RS_ZBUFFER);
            zs.setFunction( ZBufferState.CF_LESS);
            zs.setWritable(true);
            zs.setEnabled(true);
        }
        
        oldcm = ctx.defaultStateList[RenderState.RS_COLORMASK_STATE];
        
        if(cms == null) {
            cms = (ColorMaskState) renderer.createState(RenderState.RS_COLORMASK_STATE);
            cms.setAll(false);
            cms.setEnabled(true);
        }

        oldcull = ctx.defaultStateList[RenderState.RS_CULL];
        
        if( cs == null ) {
            // cull front faces, so they dont cast shadow on themselves
            cs = (CullState) renderer.createState(RenderState.RS_CULL);
            cs.setCullMode(CullState.CS_FRONT);
            cs.setEnabled(true);
        }

        // set the Z function to CF_LEQUAL
        /*
        ctx.defaultStateList[RenderState.RS_ZBUFFER] = zs;
        ctx.defaultStateList[RenderState.RS_COLORMASK_STATE] = cms;
        ctx.defaultStateList[RenderState.RS_CULL] = cs;
         */

        zs.apply(ctx);
        cms.apply(ctx);
        cs.apply(ctx);
    }
    
    protected void restoreStates(RenderContext ctx) {
        /*
        ctx.defaultStateList[RenderState.RS_ZBUFFER] = oldz;
        ctx.defaultStateList[RenderState.RS_COLORMASK_STATE] = oldcm;
        ctx.defaultStateList[RenderState.RS_CULL] = oldcull;
        */
        /*
        oldz.apply(ctx);
        oldcm.apply(ctx);
        oldcull.apply(ctx);
         */
    }
}
