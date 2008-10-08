/*
 * Copyright (c) 2003-2007 jMonkeyEngine, 2008 VL Engine
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
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
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
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Quad;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.FrameBuffer;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.Renderer;
import com.vlengine.renderer.TextureRenderer;
import com.vlengine.renderer.VBOAttributeInfo;
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
import com.vlengine.scene.state.shader.ShaderVariableFloat;
import com.vlengine.scene.state.shader.ShaderVariableInt;
import com.vlengine.system.DisplaySystem;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.VertexAttribute;

/**
 * GLSL bloom effect pass. - Render supplied source to a texture - Extract
 * intensity - Blur intensity - Blend with first pass
 * 
 * @author Rikard Herlitz (MrCoder) - initial implementation
 * @author Joshua Slack - Enhancements and reworking to use a single
 *         texrenderer, ability to reuse existing back buffer, faster blur,
 *         throttling speed-up, etc.
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class BloomRenderPass extends RenderPass {
    
    private float throttle = 1/50f; 
    private float sinceLast = 1; 
    
    private TextureRenderer tRenderer;
    private Texture mainTexture;
    private Texture secondTexture;
    //private Texture screenTexture;

    private Quad fullScreenQuad;
    private TriBatch fullScreenQuadBatch;
    //private Material material;

    private LWJGLTextureState ts;
    private LWJGLAlphaBlendState asb;
    private LWJGLAlphaTestState ast;
    private LWJGLShaderObjectsState extractionShader;
    private LWJGLShaderParameters extractionParameters;
    
    private LWJGLShaderObjectsState blurShader;
    private LWJGLShaderParameters blurParameters;
    
    private LWJGLShaderObjectsState finalShader;
    private LWJGLShaderParameters finalParameters;

    private int nrBlurPasses;
    private float blurSize;
    private float blurIntensityMultiplier;
    private float exposurePow;
    private float exposureCutoff;
    private boolean supported = true;
    private boolean useCurrentScene = false;
    private int renderScale = 4;
    
    private ShaderVariableFloat exposurePowVar;
    private ShaderVariableFloat exposureCutoffVar;
    private ShaderVariableFloat sampleDist0Var;
    private ShaderVariableFloat blurIntensityMultiplierVar;
    
    private boolean isinited = false;
    
    protected FrameBuffer source;
    
    public BloomRenderPass() {
        this("bloom");
    }
    
    public BloomRenderPass(String name) {
        super(name);
        resetParameters();
    }
    
    public void setSource(FrameBuffer source) {
        this.source = source;
    }
    
    /**
     * Creates a new bloom renderpass
     *
     * @param cam		 Camera used for rendering the bloomsource
     * @param renderScale Scale of bloom texture
     */
    
    private void initialize(RenderContext ctx) {
        if(isinited)
            return;
        isinited = true;
        DisplaySystem display = ctx.app.display;

        
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

        mainTexture = new Texture();
        mainTexture.setWrap(Texture.WM_CLAMP_S_CLAMP_T);
        mainTexture.setFilter(Texture.FM_LINEAR);
        tRenderer.setupTexture(mainTexture);

        secondTexture = new Texture();
        secondTexture.setWrap(Texture.WM_CLAMP_S_CLAMP_T);
        secondTexture.setFilter(Texture.FM_LINEAR);
        tRenderer.setupTexture(secondTexture);

        /*
        screenTexture = new Texture();
        screenTexture.setWrap(Texture.WM_CLAMP_S_CLAMP_T);
        screenTexture.setFilter(Texture.FM_LINEAR);
        tRenderer.setupTexture(screenTexture);
         */

        LWJGLShaderObjectsState.init();
        if(!LWJGLShaderObjectsState.isSupported()) {
            supported = false;
            return;
        }

        //material = new Material();
        //material.setLightCombineMode(LightState.OFF);
        
        
        //Create extract intensity shader
        extractionShader = (LWJGLShaderObjectsState) display.getRenderer().createState(RenderState.RS_GLSL_SHADER_OBJECTS);
        extractionShader.load(BloomRenderPass.class.getClassLoader().getResource(shaderDirectory + "bloom_extract.vert"),
                        BloomRenderPass.class.getClassLoader().getResource(shaderDirectory + "bloom_extract.frag"));
        extractionShader.setEnabled(true);
        try {
            // try to create the shader
            extractionShader.apply(ctx);
        } catch(Exception e) {
            supported = false;
            return;
        }
        extractionParameters = (LWJGLShaderParameters) display.getRenderer().createState(RenderState.RS_GLSL_SHADER_PARAM);
        ShaderVariableInt extractionRT = new ShaderVariableInt("RT");
        extractionRT.set(0);
        extractionParameters.setUniform(extractionRT);
        exposurePowVar = new ShaderVariableFloat("exposurePow");
        extractionParameters.setUniform(exposurePowVar);
        exposureCutoffVar = new ShaderVariableFloat("exposureCutoff");
        extractionParameters.setUniform(exposureCutoffVar);

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
        VBOAttributeInfo vboInfo = new VBOAttributeInfo();
        //vboInfo.useVBO = true;
        fullScreenQuad.getAttribBuffer(VertexAttribute.USAGE_POSITION).setVBOInfo(vboInfo);
        fullScreenQuad.setVBOMode(BaseGeometry.VBO_LONGLIVED);
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

        asb = (LWJGLAlphaBlendState) display.getRenderer().createState(RenderState.RS_ALPHABLEND);
        asb.setSourceFunction(AlphaBlendState.SB_ONE);
        asb.setDestinationFunction(AlphaBlendState.DB_ONE);
        asb.setEnabled(true);
        //material.setRenderState(as);
        //material.updateStates(null);
        ast =   (LWJGLAlphaTestState) ctx.getRenderer().createState(RenderState.RS_ALPHATEST);
        ast.setEnabled(true);
        ast.setReference(0);
        ast.setTestFunction(AlphaTestState.TF_GREATER);
        
    }

    /**
     * Reset bloom parameters to default
     */
    public void resetParameters() {
        nrBlurPasses = 2;
        blurSize = 0.02f;
        blurIntensityMultiplier = 1.3f;
        exposurePow = 3.0f;
        exposureCutoff = 0.0f;
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
        Renderer renderer = ctx.getRenderer();

        // calculate passed time
        sinceLast += ctx.time;

        if (sinceLast > throttle) {
            sinceLast = 0;
            
            // see if we should use the current scene to bloom, or only things added to the pass.
            if (useCurrentScene) {
                // grab backbuffer color to texture
                Texture screenTexture = null;
                if(source!=null) {
                    screenTexture = source.getComponent(FrameBuffer.ComponentType.Color0);
                } else if(target!=null) {
                    screenTexture = target.getComponent(FrameBuffer.ComponentType.Color0);
                }
                if(screenTexture==null)
                    return;
                ts.setTexture(screenTexture, 0);
            } else {
                //Render scene to texture
                tRenderer.beginRender(ctx, mainTexture, true); // maybe we need true here?
                // render the renderable list to the texture
                super.renderPass(ctx);
                tRenderer.endRender();
                ts.setTexture(mainTexture, 0);
            }
            if(!ctx.getRenderer().isInOrthoMode())
                ctx.getRenderer().setOrtho();
            ctx.defaultMaterial.apply(ctx);


            //Extract intensity
            exposurePowVar.set(getExposurePow());
            exposureCutoffVar.set(getExposureCutoff());
            
            // begin rendering to texture
            tRenderer.beginRender(ctx, secondTexture, true);
            // apply the material
            //material.apply(ctx);
            extractionShader.apply(ctx);
            extractionParameters.apply(ctx);
            ts.apply(ctx,0);
            
            // draw the quad
            renderer.draw(fullScreenQuadBatch);
            tRenderer.endRender();
    
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
            renderer.draw(fullScreenQuadBatch);
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
                    renderer.draw(fullScreenQuadBatch);
                    tRenderer.endRender();
                    
                } else {
                    ts.setTexture(secondTexture, 0);
                    // begin rendering to texture
                    tRenderer.beginRender(ctx, mainTexture, true);
                    // apply the material
                    //material.apply(ctx);
                    ts.apply(ctx,0);
                    // draw the quad
                    renderer.draw(fullScreenQuadBatch);
                    tRenderer.endRender();
                }
            }
            if (getNrBlurPasses()%2 == 1) {
                ts.setTexture(mainTexture, 0);
            } else {
                ts.setTexture(secondTexture, 0);
            }
    }
            //Final blend
            // reset the alpha state
            // draw to main framebuffer
            //ctx.defaultMaterial.apply(ctx);

            //((LWJGLRenderer)ctx.getRenderer()).reset();

            if(!ctx.getRenderer().isInOrthoMode())
                ctx.getRenderer().setOrtho();

            asb.apply(ctx);
            ast.apply(ctx);
            finalShader.apply(ctx);
            ts.apply(ctx, 0);

            // activate the target renderbuffer
            //target.activate(ctx);

            ctx.getRenderer().draw(fullScreenQuadBatch);

        ctx.getRenderer().unsetOrtho();
        ctx.getRenderer().reset();
    }

    /**
     * @return The throttle amount - or in other words, how much time in
     *         seconds must pass before the bloom effect is updated.
     */
    
    public float getThrottle() {
        return throttle;
    }

    /**
     * @param throttle
     *            The throttle amount - or in other words, how much time in
     *            seconds must pass before the bloom effect is updated.
     */
    public void setThrottle(float throttle) {
        this.throttle = throttle;
    }

    public float getBlurSize() {
        return blurSize;
    }

    public void setBlurSize(float blurSize) {
            this.blurSize = blurSize;
    }

    public float getExposurePow() {
            return exposurePow;
    }

    public void setExposurePow(float exposurePow) {
            this.exposurePow = exposurePow;
    }

    public float getExposureCutoff() {
            return exposureCutoff;
    }

    public void setExposureCutoff(float exposureCutoff) {
            this.exposureCutoff = exposureCutoff;
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

    public boolean useCurrentScene() {
        return useCurrentScene;
    }

    public void setUseCurrentScene(boolean useCurrentScene) {
        this.useCurrentScene = useCurrentScene;
    }

    public void setRenderScale(int renderScale) {
        this.renderScale = renderScale;
    }
}
