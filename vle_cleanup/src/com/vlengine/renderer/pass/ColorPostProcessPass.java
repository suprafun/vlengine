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
import com.vlengine.renderer.FrameBuffer;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.Renderer;
import com.vlengine.renderer.VBOAttributeInfo;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.ZBufferState;
import com.vlengine.scene.state.lwjgl.LWJGLShaderObjectsState;
import com.vlengine.scene.state.lwjgl.LWJGLShaderParameters;
import com.vlengine.scene.state.lwjgl.LWJGLTextureState;
import com.vlengine.scene.state.lwjgl.LWJGLZBufferState;
import com.vlengine.system.DisplaySystem;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.VertexAttribute;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ColorPostProcessPass extends RenderPass {
    private Quad fullScreenQuad;
    private TriBatch fullScreenQuadBatch;
    
    private LWJGLTextureState ts;
    private boolean isinited = false;
    
    protected FrameBuffer source;
    
    private LWJGLShaderObjectsState finalShader;
    private LWJGLShaderParameters finalParameters;
    
    
    
    public ColorPostProcessPass() {
        super("CopyColor");
    }
    
    public void setSource(FrameBuffer source) {
        this.source = source;
    }
    
    private void initialize(RenderContext ctx) {
        if(isinited)
            return;
        isinited = true;
        DisplaySystem display = ctx.app.display;

        //Create final shader(basic texturing)
        finalShader = (LWJGLShaderObjectsState) display.getRenderer().createState(RenderState.RS_GLSL_SHADER_OBJECTS);
        finalShader.load(BloomRenderPass.class.getClassLoader().getResource(shaderDirectory + "bloom_final.vert"),
                        BloomRenderPass.class.getClassLoader().getResource(shaderDirectory + "bloom_final.frag"));
        finalShader.setEnabled(true);
        try {
            // try to create the shader
            finalShader.apply(ctx);
        } catch(Exception e) {
            finalShader = null;
            return;
        }
        
        //Create fullscreen quad
        fullScreenQuad = new Quad(display.getWidth(), display.getHeight());///4
        VBOAttributeInfo vboInfo = new VBOAttributeInfo();
        fullScreenQuad.getAttribBuffer(VertexAttribute.USAGE_POSITION).setVBOInfo(vboInfo);
        fullScreenQuad.setVBOMode(BaseGeometry.VBO_LONGLIVED);
        fullScreenQuadBatch = new TriBatch();
        fullScreenQuadBatch.setModel(fullScreenQuad);
        
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
        ts = new LWJGLTextureState();
        ts.setEnabled(true);
    }
    
    ZBufferState zs;
    
    @Override
    public void renderPass(RenderContext ctx) {
        // initialize if not yet
        if(!isinited)
            initialize(ctx);
            
        Renderer renderer = ctx.getRenderer();

        if(zs==null) {
            zs = new LWJGLZBufferState();
            zs.setWritable(false);
            zs.setEnabled(false);
        }
        // if we have a source, render it as a fullscreen quad
        if (source != null) {
            // grab backbuffer color to texture
            Texture screenTexture = null;
            if(source!=null) {
                screenTexture = source.getComponent(FrameBuffer.ComponentType.Color0);
            }
            if(screenTexture==null)
                return;
            screenTexture.setApply(Texture.AM_REPLACE);
            
            ts.setTexture(screenTexture, 0);
            //renderer.getCamera().copy(prevCamera);
            //renderer.setCamera(flatCamera);
            if(!renderer.isInOrthoMode())
                renderer.setOrtho();
            ctx.defaultMaterial.apply(ctx);
            zs.apply(ctx);
            ts.apply(ctx, 0);
            finalShader.apply(ctx);
            renderer.draw(fullScreenQuadBatch);
            renderer.unsetOrtho();
            //renderer.setCamera(prevCamera);
            renderer.reset();
        }
        

        // get the list of batches that we will render, if we only need to
        // work on those
        FastList<Renderable> list = ctx.getRenderQueue().getQueue(this.queueNo);
        if( list!=null && list.size() != 0 ) {
            //Render scene to texture
            // render the renderable list to the texture
            super.renderPass(ctx);
        }
    }

}
