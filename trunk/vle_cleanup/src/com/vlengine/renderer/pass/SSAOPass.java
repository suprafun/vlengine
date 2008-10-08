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

import com.vlengine.app.frame.Frame;
import com.vlengine.image.Texture;
import com.vlengine.math.Transform;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Quad;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.FrameBuffer;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.Renderer;
import com.vlengine.renderer.TextureRenderer;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.AlphaBlendState;
import com.vlengine.scene.state.AlphaTestState;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.lwjgl.LWJGLAlphaBlendState;
import com.vlengine.scene.state.lwjgl.LWJGLAlphaTestState;
import com.vlengine.scene.state.lwjgl.LWJGLShaderObjectsState;
import com.vlengine.scene.state.lwjgl.LWJGLShaderParameters;
import com.vlengine.scene.state.lwjgl.LWJGLTextureState;
import com.vlengine.scene.state.shader.ShaderTexture;
import com.vlengine.scene.state.shader.ShaderVariableFloat;
import com.vlengine.scene.state.shader.ShaderVariableFloat2;
import com.vlengine.scene.state.shader.ShaderVariableInt;
import com.vlengine.system.DisplaySystem;
import com.vlengine.util.FastList;

/**
 * SSAO pass is a post processing pass (like bloom)
 * it takes the depth buffer and color buffer, and overlays an estimation of
 * ambient occlusion shading onto the final image.
 * @author vear (Arpad Vekas)
 */
public class SSAOPass extends RenderPass {

    private float throttle = 1/60f; 
    private float sinceLast = 1; 
    
    // septh texture we got by rendering ourselves
    protected Texture depthTexture;
    
    private TextureRenderer tRenderer;
    private Texture mainTexture;
    private Texture secondTexture;
    
    protected Texture ssaoTexture;
    
    private Quad fullScreenQuad;
    private TriBatch fullScreenQuadBatch;
    //private Material material;

    private LWJGLTextureState ts;
    private LWJGLAlphaBlendState as;
    private LWJGLAlphaTestState ast;
    private LWJGLShaderObjectsState extractionShader;
    private LWJGLShaderParameters extractionParameters;
    private ShaderVariableFloat2 cr;
    
    private LWJGLShaderObjectsState blurShader;
    private LWJGLShaderParameters blurParameters;
    
    private LWJGLShaderObjectsState finalShader;
    private LWJGLShaderParameters finalParameters;
    
    private int nrBlurPasses;
    private float blurSize;
    private float blurIntensityMultiplier;
    // use the current scene already rendered
    protected boolean useCurrentScene;
    // render to screen, or just to to texture
    protected boolean processScreen;
    
    protected boolean isinited = false;
    private boolean supported = true;
    private int renderScale = 1;
    
    private ShaderVariableFloat sampleDist0Var;
    private ShaderVariableFloat blurIntensityMultiplierVar;
    
    protected Vector3f prevCamPosition = new Vector3f();
    protected Vector3f prevCamDirection = new Vector3f();
    
    protected FrameBuffer source;
    
    
    public SSAOPass() {
        this("SSAO");
    }
    
    public SSAOPass(String name) {
        super(name);
    }

    public boolean isSupported() {
        return supported;
    }

    public void setSource(FrameBuffer source) {
        this.source = source;
    }

    /**
     * Set the texture into which the ambien occlusion shading is to be stored
     * @param tex
     */
    public void setSSAOTexture(Texture tex) {
        ssaoTexture = tex;
    }

    public Texture getSSAOTexture() {
        return ssaoTexture;
    }

    public boolean useCurrentScene() {
        return useCurrentScene;
    }

    public void setUseCurrentScene(boolean useCurrentScene) {
        this.useCurrentScene = useCurrentScene;
    }

    public void setRenderScale(int renderScale) {
        this.renderScale = renderScale;
    }

    public void setProcessFrame(boolean processFrame) {
        this.processScreen = processFrame;
    }

    public boolean getProcessFrame() {
        return processScreen;
    }

    public float getBlurSize() {
        return blurSize;
    }

    public void setBlurSize(float blurSize) {
            this.blurSize = blurSize;
    }
    
    public float getBlurIntensityMultiplier() {
            return blurIntensityMultiplier;
    }

    public void setBlurIntensityMultiplier(float blurIntensityMultiplier) {
            this.blurIntensityMultiplier = blurIntensityMultiplier;
    }

    public int getNrBlurPasses() {
            return nrBlurPasses;
    }

    public void setNrBlurPasses(int nrBlurPasses) {
            this.nrBlurPasses = nrBlurPasses;
    }
    
    public void setThrotle(float throtle) {
        this.throttle = throtle;
    }
            
    protected void initialize(RenderContext ctx) {
        // set up render to texture
        if(isinited)
            return;
        isinited = true;
        
        DisplaySystem display = ctx.app.display;
        
        Renderer renderer = ctx.getRenderer();

        //Create texture renderers and rendertextures(alternating between two not to overwrite pbuffers)
        tRenderer = display.createTextureRenderer(
            display.getWidth() / renderScale, 
            display.getHeight() / renderScale,
            TextureRenderer.RENDER_TEXTURE_2D);

        if (!tRenderer.isSupported()) {
            supported = false;
            return;
        }
        tRenderer.setMultipleTargets(true);
        tRenderer.setBackgroundColor(new ColorRGBA(0.0f, 0.0f, 0.0f, 1.0f));
        //tRenderer.setCamera(camera);

        depthTexture = new Texture();
        depthTexture.setRTTSource(Texture.RTT_SOURCE_DEPTH);
        depthTexture.setWrap(Texture.WM_CLAMP_S_CLAMP_T);
        depthTexture.setFilter(Texture.FM_LINEAR);
        tRenderer.setupTexture(depthTexture);
        
        mainTexture = new Texture();
        mainTexture.setWrap(Texture.WM_CLAMP_S_CLAMP_T);
        mainTexture.setFilter(Texture.FM_LINEAR);
        tRenderer.setupTexture(mainTexture);

        secondTexture = new Texture();
        secondTexture.setWrap(Texture.WM_CLAMP_S_CLAMP_T);
        secondTexture.setFilter(Texture.FM_LINEAR);
        tRenderer.setupTexture(secondTexture);

        LWJGLShaderObjectsState.init();
        if(!LWJGLShaderObjectsState.isSupported()) {
            supported = false;
            return;
        }

        //material = new Material();
        //material.setLightCombineMode(LightState.OFF);
        
        
        //Create extract intensity shader
        extractionShader = (LWJGLShaderObjectsState) renderer.createState(RenderState.RS_GLSL_SHADER_OBJECTS);
        extractionShader.load(SSAOPass.class.getClassLoader().getResource(shaderDirectory + "ssao.vert"),
                        SSAOPass.class.getClassLoader().getResource(shaderDirectory + "ssao.frag"));
        extractionShader.setEnabled(true);
        try {
            // try to create the shader
            extractionShader.apply(ctx);
        } catch(Exception e) {
            supported = false;
            return;
        }
        extractionParameters = (LWJGLShaderParameters) display.getRenderer().createState(RenderState.RS_GLSL_SHADER_PARAM);
        // set screen dimension
        ShaderVariableFloat2 ss = new ShaderVariableFloat2("screensize");
        ss.set(display.getWidth(), display.getHeight());
        
        extractionParameters.setUniform(ss);
        
        // create screen range variable
        cr = new ShaderVariableFloat2("camerarange");
        extractionParameters.setUniform(cr);
        
        ShaderVariableInt depthtex = new ShaderVariableInt("texture0");
        depthtex.set(0);
        extractionParameters.setUniform(depthtex);

        //Create blur shader
        
        blurShader = (LWJGLShaderObjectsState) display.getRenderer().createState(RenderState.RS_GLSL_SHADER_OBJECTS);
        blurShader.load(BloomRenderPass.class.getClassLoader().getResource(shaderDirectory + "bloom_blur.vert"),
                        BloomRenderPass.class.getClassLoader().getResource(shaderDirectory + "bloom_blur.frag"));
        blurShader.setEnabled(true);
        try {
            // try to create the shader
            blurShader.apply(ctx);
        } catch(Exception e) {
            supported = false;
            return;
        }
        blurParameters = (LWJGLShaderParameters) display.getRenderer().createState(RenderState.RS_GLSL_SHADER_PARAM);
        ShaderVariableInt blurRT = new ShaderVariableInt("RT");
        blurRT.set(0);
        blurParameters.setUniform(blurRT);
        sampleDist0Var = new ShaderVariableFloat("sampleDist0");
        blurParameters.setUniform(sampleDist0Var);
        blurIntensityMultiplierVar = new ShaderVariableFloat("blurIntensityMultiplier");
        blurParameters.setUniform(blurIntensityMultiplierVar);

        //Create final shader(basic texturing)
        finalShader = (LWJGLShaderObjectsState) display.getRenderer().createState(RenderState.RS_GLSL_SHADER_OBJECTS);
        finalShader.load(BloomRenderPass.class.getClassLoader().getResource(shaderDirectory + "bloom_final.vert"),
                        BloomRenderPass.class.getClassLoader().getResource(shaderDirectory + "bloom_final.frag"));
        finalShader.setEnabled(true);
        try {
            // try to create the shader
            finalShader.apply(ctx);
        } catch(Exception e) {
            supported = false;
            return;
        }
        //Create fullscreen quad
        fullScreenQuad = new Quad(display.getWidth()/4, display.getHeight()/4);///4
        fullScreenQuad.setVBOMode(BaseGeometry.VBO_LONGLIVED);
        fullScreenQuad.createVBOInfos();
        fullScreenQuadBatch = new TriBatch();
        fullScreenQuadBatch.setModel(fullScreenQuad);
        //fullScreenQuadBatch.setMaterial(0, material);
        
        // create fixed postition for the quad
        Transform quadTransForm = new Transform();
        quadTransForm.getRotation().set(0, 0, 0, 1);
        quadTransForm.getTranslation().set(display.getWidth() / 2, display.getHeight() / 2, 0);
        quadTransForm.getScale().set(1, 1, 1);
        // create transform array for all the frames
        Transform[] batchTransforms = new Transform[Frame.MAX_FRAMES];
        // but all will point to the same transform
        for(int i=0; i<batchTransforms.length; i++)
            batchTransforms[i] = quadTransForm;
        fullScreenQuadBatch.setWorldTransform(batchTransforms);
        // ortho rendering, but it doesnt matter, we will set ortho ourselves?
        //fullScreenQuadBatch.setRenderQueueMode(RenderQueue.FILTER_ORTHO);

        //fullScreenQuadBatch.setCullMode(SceneElement.CULL_NEVER);
        //fullScreenQuadBatch.setTextureCombineMode(TextureState.REPLACE);
        //fullScreenQuadBatch.setLightCombineMode(LightState.OFF);

        ts = new LWJGLTextureState();
        ts.setEnabled(true);
        //material.setRenderState(ts);

        as = (LWJGLAlphaBlendState) ctx.getRenderer().createState(RenderState.RS_ALPHABLEND);
        //if(ctx.app.conf.graphDepthFog) {
        //    as.setSrcFunction(AlphaState.SB_DST_ALPHA);
        //    as.setDstFunction(AlphaState.DB_SRC_COLOR);
        //} else {
            
            as.setSourceFunction(AlphaBlendState.SB_DST_COLOR);
            as.setDestinationFunction(AlphaBlendState.DB_SRC_COLOR);
            //as.setBlendEquation(AlphaBlendState.BlendEquation.Min);
        //}
         
        //as.setSrcFunction(AlphaState.SB_DST_COLOR);
        //as.setDstFunction(AlphaState.DB_ZERO);
        as.setEnabled(true); 
        
        ast =   (LWJGLAlphaTestState) ctx.getRenderer().createState(RenderState.RS_ALPHATEST);
        ast.setEnabled(true);
        ast.setReference(0);
        ast.setTestFunction(AlphaTestState.TF_GREATER);
    }
    
    @Override
    public void renderPass(RenderContext ctx) {
        // initialize if not yet
        if(!isinited)
            initialize(ctx);
        // if not supported, do nothing
        if(!supported)
            return;

        // get the list of batches that we will render, if we only need to
        // work on those
        FastList<Renderable> list = ctx.getRenderQueue().getQueue(this.queueNo);
        if (!useCurrentScene && ( list==null || list.size() == 0 )) {
            return;
        }
        
        //LWJGLAlphaState as = (LWJGLAlphaState) material.getRenderState(RenderState.RS_ALPHA);
        //LWJGLTextureState ts = (LWJGLTextureState) material.getRenderState(RenderState.RS_TEXTURE);

        Renderer rederer = ctx.getRenderer();
        
        // calculate passed time
        sinceLast += ctx.time;

        boolean camMoved = false;
        if(!prevCamPosition.equals(ctx.currentCamera.getLocation())) {
            camMoved = true;
            prevCamPosition.set(ctx.currentCamera.getLocation());
        }
        if(!prevCamDirection.equals(ctx.currentCamera.getDirection())) {
            prevCamDirection.set(ctx.currentCamera.getDirection());
            camMoved = true;
        }
        if (sinceLast > throttle || camMoved) {
            sinceLast = 0;

            // see if we should use the current scene to bloom, or only things added to the pass.
            if (useCurrentScene) {
                Texture t = null;
                if(source!=null) {
                    t=source.getComponent(FrameBuffer.ComponentType.Depth);
                } else if(target!=null) {
                    t=target.getComponent(FrameBuffer.ComponentType.Depth);
                }
                // grab backbuffer depth buffer to texture
                //tRenderer.copyToTexture(depthTexture, 
                //        DisplaySystem.getDisplaySystem().getWidth(), 
                //        DisplaySystem.getDisplaySystem().getHeight());
                if(t==null)
                    return;
                ts.setTexture(t, 0);
            } else {
                //Render scene to texture
                tRenderer.beginRender(ctx, depthTexture, true); // maybe we need true here?
                // render the renderable list to the texture
                super.renderPass(ctx);
                tRenderer.endRender();
                ts.setTexture(depthTexture, 0);
            }
            if(!ctx.getRenderer().isInOrthoMode())
                ctx.getRenderer().setOrtho();
            ctx.defaultMaterial.apply(ctx);


            // set camera range
            cr.set(ctx.currentCamera.getFrustumNear(), ctx.currentCamera.getFrustumFar());
            
            // begin rendering to texture
            tRenderer.beginRender(ctx, secondTexture, true);
            // apply the material
            //material.apply(ctx);
            extractionShader.apply(ctx);
            extractionParameters.apply(ctx);
            ts.apply(ctx,0);
            
            // draw the quad
            rederer.draw(fullScreenQuadBatch);
            tRenderer.endRender();
    
            if(getNrBlurPasses()>0) {
                //Blur
                sampleDist0Var.set(getBlurSize());
                blurIntensityMultiplierVar.set(getBlurIntensityMultiplier());

                ts.setTexture(secondTexture, 0);

                // begin rendering to texture
                tRenderer.beginRender(ctx, mainTexture, true);
                // apply the material
                blurShader.apply(ctx);
                blurParameters.apply(ctx);
                ts.apply(ctx,0);
                //material.apply(ctx);
                // draw the quad
                rederer.draw(fullScreenQuadBatch);
                tRenderer.endRender();

                //Extra blur passes
                for(int i = 1; i < getNrBlurPasses(); i++) {
                    if (i%2 == 1) {
                        ts.setTexture(mainTexture, 0);
                        // begin rendering to texture
                        tRenderer.beginRender(ctx, secondTexture, true);
                        // apply the material
                        ts.apply(ctx,0);
                        // draw the quad
                        rederer.draw(fullScreenQuadBatch);
                        tRenderer.endRender();

                    } else {
                        ts.setTexture(secondTexture, 0);
                        // begin rendering to texture
                        tRenderer.beginRender(ctx, mainTexture, true);
                        // apply the material
                        //material.apply(ctx);
                        ts.apply(ctx,0);
                        // draw the quad
                        rederer.draw(fullScreenQuadBatch);
                        tRenderer.endRender();
                    }
                }
            }
            if (getNrBlurPasses()%2 == 1) {
                ts.setTexture(mainTexture, 0);
            } else {
                ts.setTexture(secondTexture, 0);
            }
            
            // if ssao texture is set, copy to that texture
            if(ssaoTexture==null) {
                ssaoTexture = new Texture();
            }
            ssaoTexture.setTextureId(ts.getTexture(0).getTextureId());
                //ssaoTexture.setTextureId(depthTexture.getTextureId());
    }
            //Final blend
            // reset the alpha state
            // draw to main framebuffer
            //ctx.defaultMaterial.apply(ctx);

            //((LWJGLRenderer)ctx.getRenderer()).reset();

        if(processScreen==true) {
            if(!ctx.getRenderer().isInOrthoMode())
                ctx.getRenderer().setOrtho();

            as.apply(ctx);
            ast.apply(ctx);
            finalShader.apply(ctx);
            ts.setTexture(ssaoTexture, 0);
            ts.apply(ctx, 0);

            ctx.getRenderer().draw(fullScreenQuadBatch);
        
            
        }
        if(ctx.getRenderer().isInOrthoMode())
            ctx.getRenderer().unsetOrtho();
        ctx.getRenderer().reset();
    }
    
}
