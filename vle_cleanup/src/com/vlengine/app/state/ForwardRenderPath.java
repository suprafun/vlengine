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

package com.vlengine.app.state;

import com.vlengine.app.AppContext;
import com.vlengine.app.frame.Frame;
import com.vlengine.light.LightSorterGameState;
import com.vlengine.renderer.FrameBuffer;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.pass.BackgroundPass;
import com.vlengine.renderer.pass.BloomRenderPass;
import com.vlengine.renderer.pass.DepthPass;
import com.vlengine.renderer.pass.LightExtractPass;
import com.vlengine.renderer.pass.OpaquePass;
import com.vlengine.renderer.pass.OrthoPass;
import com.vlengine.renderer.pass.PassManager;
import com.vlengine.renderer.pass.ColorPostProcessPass;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.renderer.pass.SSAOPass;
import com.vlengine.renderer.pass.AlphaTestPass;
import com.vlengine.renderer.pass.AlphaBlendedPass;

/**
 * Controls normal forward rendering
 * @author vear (Arpad Vekas)
 */
public class ForwardRenderPath extends RenderPath {

    BackgroundPass back;
    DepthPass zpass;
    LightExtractPass light;
    OpaquePass opaque;
    //AlphaTestDepthPass alphaDepth;
    AlphaTestPass alpha;
    RenderPass twosided;
    RenderPass ortho;
    ColorPostProcessPass post;
            
    // optional postprocessing effects
    BloomRenderPass bloom = null;
    SSAOPass ssao = null;
    
    // the main renderbuffer of the screen
    FrameBuffer fb;
    // final framebuffer
    FrameBuffer finalFb;
    protected boolean isBuffersSetup = false;
    
    //protected boolean postprocess = false;
    
    public ForwardRenderPath(AppContext app) {
        super(app);
    }

    @Override
    public void setup() {
        
        // setup ligting
         if(app.conf.lightmode==1) {
            // sorted lights
            LightSorterGameState lightManagement = new LightSorterGameState();
            lightManagement.setActive(true);
            lightManagement.setName("LightManagement");
            app.getGameStates().attachChild(lightManagement);
        }
    }
    
    public RenderPass getAlphaTestPass() {
        return alpha;
    }
    
    public void createDefaultPasses(Frame f) {
        if(fb==null) {
            if(app.conf.graphPostprocess) {
                // create texture renderer for normal passes
                fb = new FrameBuffer(app, app.display.getWidth(), app.display.getHeight());
                // create main fb for final pass
                finalFb = new FrameBuffer(app);
            } else {
                fb = new FrameBuffer(app);
                finalFb = fb;
            }
            isBuffersSetup = false;
        }
        
        // create background pass
        if( back == null ) {
            back = new BackgroundPass("back");
            back.setId(RenderPass.StandardPass.BackGround.passId);
            // uses main renderer
            //back.setRenderer(renderer);
            // uses unlit material
            // TODO:
            back.setUsedMaterialFlags(0);
            // renders queue 0
            back.setQueueNo(RenderQueue.StandardQueue.BackGround.queuId);
            // only render objects intended for opaque queue
            back.setQueueFilter(RenderQueue.QueueFilter.BackGround.value);
            back.setEnabled(true);
            back.setEnableAlphaBlend(false);
        }
        // uses main camera
        back.setCamera(f.getCamera());
        // uses main frambuffer
        back.setTarget(fb);
        // create queue 0 with ordering by id 0
        f.getQueueManager().createQueue(back.getQueueNo(), null);
        // add the pass manager
        f.getPasses().addPass(back);

        // Z pass
        if(zpass==null) {
            zpass = new DepthPass();
            zpass.setId(RenderPass.StandardPass.Depth.passId);
            // uses main camera
            zpass.setCamera(f.getCamera());
            // uses main renderer
            zpass.setTarget(fb);
            //.setRenderer(renderer);
            // TODO:
            // uses depth producing material only
            zpass.setUsedMaterialFlags(0);
            // renders queue 0
            zpass.setQueueNo(RenderQueue.StandardQueue.Opaque.queuId);
            // only render objects intended for opaque queue
            zpass.setQueueFilter(0);
            zpass.setEnabled(true);
        }
        // create queue 0 with ordering by batch material 0
        //queues.createQueue(zpass.getQueueNo(), 0);
        // add the pass manager
        f.getPasses().addPass(zpass);
        f.getPasses().setPassOrder(back.getId(), zpass.getId());

        // create light extract pass
        if( light == null ) {
            light = new LightExtractPass("lightextract");
            light.setId(RenderPass.StandardPass.Ligh.passId);
            // uses main camera
            light.setCamera(f.getCamera());
            // uses main renderer
            light.setTarget(fb);
            //.setRenderer(renderer);
            // this pass uses no material
            light.setUsedMaterialFlags(-1);
            // renders queue 0
            light.setQueueNo(RenderQueue.StandardQueue.Ligh.queuId);
            // only render objects intended for opaque queue
            light.setQueueFilter(RenderQueue.QueueFilter.Light.value);
            light.setEnabled(true);
        }
        // create queue 0 with ordering by batch material 0
        f.getQueueManager().createQueue(light.getQueueNo(), null);
        // add the pass manager
        f.getPasses().addPass(light);
        f.getPasses().setPassOrder(zpass.getId(), light.getId());

        // create opaque pass
        if( opaque == null ) {
            opaque = new OpaquePass("opaque");
            opaque.setId(RenderPass.StandardPass.Opaque.passId);
            //.setRenderer(renderer);
            // uses unlit material
            opaque.setUsedMaterialFlags(0);
            // renders queue 0
            opaque.setQueueNo(RenderQueue.StandardQueue.Opaque.queuId);
            // only render objects intended for opaque queue
            opaque.setQueueFilter(RenderQueue.QueueFilter.Opaque.value);
            opaque.setEnabled(true);
        }
        // uses main camera
        opaque.setCamera(f.getCamera());
        // uses main renderer
        opaque.setTarget(fb);
        // create queue 0 with ordering by batch material 0
        f.getQueueManager().createQueue(opaque.getQueueNo(), RenderQueue.SortType.DistanceSquaredFromCamera); // change this to order front to back
        // add the pass manager
        f.getPasses().addPass(opaque);
        f.getPasses().setPassOrder(light.getId(), opaque.getId());
        f.getPasses().setPassOrder(zpass.getId(), opaque.getId());
        opaque.setZbufferWrite(!zpass.isEnabled());
        opaque.setEnableAlphaBlend(app.conf.graphDepthFog);
        
        /*
        if(alphaDepth==null)
            alphaDepth = new AlphaTestDepthPass();
        alphaDepth.setId(RenderPass.StandardPass.AlphaTestedDepth.passId);
        // uses main camera
        alphaDepth.setCamera(f.getCamera());
        // uses main renderer
        alphaDepth.setTarget(fb);
        //.setRenderer(renderer);
        // TODO:
        // uses depth producing material only
        alphaDepth.setUsedMaterialFlags(0);
        // renders queue 0
        alphaDepth.setQueueNo(RenderQueue.StandardQueue.AlphaTested.queuId);
        // only render objects intended for opaque queue
        alphaDepth.setQueueFilter(0);
        alphaDepth.setEnabled(true);
        // create queue 0 with ordering by batch material 0
        //queues.createQueue(zpass.getQueueNo(), 0);
        // add the pass manager
        f.getPasses().addPass(alphaDepth);
        f.getPasses().setPassOrder(opaque.getId(), alphaDepth.getId());
        */
        
        // create alpha test queue (alpha)
        if( alpha == null ) {
            alpha = new AlphaTestPass("alphatest");
            alpha.setId(RenderPass.StandardPass.AlphaTested.passId);
            //.setRenderer(renderer);
            // use unlit material 
            alpha.setUsedMaterialFlags(0);
            alpha.setQueueNo(RenderQueue.StandardQueue.AlphaTested.queuId);
            alpha.setQueueFilter(RenderQueue.QueueFilter.AlphaTested.value);
            alpha.setEnabled(true);
        }
        alpha.setCamera(f.getCamera());
        alpha.setTarget(fb);

        f.getQueueManager().createQueue(alpha.getQueueNo(), null);//RenderQueue.SortType.InverseDistanceSquaredFromCamera);
        f.getPasses().addPass(alpha);
        // add ordering: alpha after opaque
        f.getPasses().setPassOrder(light.getId(), alpha.getId());
        f.getPasses().setPassOrder(opaque.getId(), alpha.getId());
        //f.getPasses().setPassOrder(opaque.getId(), alpha.getId());
        alpha.setEnableAlphaBlend(app.conf.graphDepthFog);
        
        // create twosided transparent (alpha)
        if( twosided == null ) {
            twosided = new AlphaBlendedPass();
            twosided.setId(RenderPass.StandardPass.AlphaBlended.passId);
            twosided.setCamera(f.getCamera());
            twosided.setTarget(fb);
            //.setRenderer(renderer);
            // use unlit material 
            twosided.setUsedMaterialFlags(0);
            twosided.setQueueNo(RenderQueue.StandardQueue.AlphaBlended.queuId);
            twosided.setQueueFilter(RenderQueue.QueueFilter.AlphaBlended.value);
            twosided.setEnabled(true);
            twosided.setEnableAlphaBlend(false);
        }
        f.getQueueManager().createQueue(twosided.getQueueNo(), RenderQueue.SortType.InverseDistanceSquaredFromCamera);
        f.getPasses().addPass(twosided);
        // add ordering: alpha after opaque
        f.getPasses().setPassOrder(alpha.getId(), twosided.getId());

        // set up postprocess
        if(app.conf.graphPostprocess ) {
            if(post==null) {
                post = new ColorPostProcessPass();
                post.setQueueFilter(0);
                // dont collect a queue
                post.setQueueNo(-1);
                // dont use material
                post.setUsedMaterialFlags(0);
                // assign an id to the pass
                post.setId(app.genPassId());
                post.setEnabled(true);
                post.setSource(fb);
                post.setTarget(finalFb);
                fb.addComponent(FrameBuffer.ComponentType.Color0);
            }

            f.getPasses().addPass(post);
            f.getPasses().setPassOrder(twosided.getId(), post.getId());
        }
        
        // create ortho pass
        if( ortho == null ) {
            ortho = new OrthoPass("ortho");
            ortho.setId(RenderPass.StandardPass.Ortho.passId);
            // no cull check on ortho pass
            ortho.setCamera(f.getCamera());
            ortho.setTarget(finalFb);
            //.setRenderer(renderer);
            ortho.setUsedMaterialFlags(0);
            ortho.setQueueNo(RenderQueue.StandardQueue.Ortho.queuId);
            ortho.setEnabled(true);
            ortho.setQueueFilter(RenderQueue.QueueFilter.Ortho.value);
        }
        // create ortho queue
        f.getQueueManager().createQueue(ortho.getQueueNo(), RenderQueue.SortType.AbsoluteDepth);
        f.getPasses().addPass(ortho);

        // ordering: ortho after alpha
        if(post==null) {
            f.getPasses().setPassOrder(twosided.getId(), ortho.getId());
        } else {
            f.getPasses().setPassOrder(post.getId(), ortho.getId());
        }
        
        if(app.conf.graphBloom) {
            if(bloom==null) {
                // setup bloom
                bloom = new BloomRenderPass();
                //bloom.setCamera(app.cam);
                bloom.setUseCurrentScene(true);
                // it is ignored for now
                //bloom.setRenderer(app.display.getRenderer());
                // dont select scene elements
                bloom.setQueueFilter(0);
                // dont collect a queue
                bloom.setQueueNo(-1);
                // dont use material
                bloom.setUsedMaterialFlags(0);
                // assign an id to the pass
                bloom.setId(app.genPassId());
                bloom.setRenderScale(4);
                bloom.setBlurSize(0.02f);
                bloom.setExposurePow(3.0f);
                bloom.setBlurIntensityMultiplier(0.6f);
                bloom.setNrBlurPasses(2);
                bloom.setEnabled(true);
                bloom.setSource(fb);
                bloom.setTarget(finalFb);
                
                // mark that we need color buffer in the fb
                fb.addComponent(FrameBuffer.ComponentType.Color0);
            }
            if(bloom != null && bloom.isSupported() && bloom.isEnabled()) {
                f.getPasses().addPass(bloom);
                // TODO: rework the dependency system, so that it does not create garbage
                if(post==null) {
                    f.getPasses().setPassOrder(twosided.getId(), bloom.getId(), ortho.getId());
                } else {
                    f.getPasses().setPassOrder(post.getId(), bloom.getId(), ortho.getId());
                }

                // opaque
                // twosided
                // alpha
                // post
                //       <----- bloom
                // ortho
            }
        }
        //bloom.setThrottle(0);
        
        // set up ssao
        
        if(app.conf.graphSSAO) {
            if(ssao==null) {
                ssao = new SSAOPass();
                ssao.setUseCurrentScene(true);
                ssao.setProcessFrame(true);
                //ssao.setRenderer(app.display.getRenderer());
                // dont select scene elements
                ssao.setQueueFilter(0);
                // dont collect a queue
                ssao.setQueueNo(-1);
                // dont use material
                ssao.setUsedMaterialFlags(0);
                // assign an id to the pass
                ssao.setId(app.genPassId());
                ssao.setRenderScale(1);
                ssao.setBlurSize(0.0002f);//0.002f
                ssao.setBlurIntensityMultiplier(0.85f);
                ssao.setNrBlurPasses(4);//4
                ssao.setThrotle(1f/60f);
                //ssao.setThrotle(0);
                ssao.setEnabled(true);
                ssao.setSource(fb);
                ssao.setTarget(finalFb);
                
                // mark that we need depth buffer in the fb
                fb.addComponent(FrameBuffer.ComponentType.Depth);
            }
            if(ssao != null && ssao.isSupported() && ssao.isEnabled()) {
                PassManager p = f.getPasses();
                p.addPass(ssao);
                if(post==null) {
                    p.setPassOrder(twosided.getId(), ssao.getId(), ortho.getId());
                } else {
                    p.setPassOrder(post.getId(), ssao.getId(), ortho.getId());
                }

                // opaque
                // twosided
                // alpha
                // post
                //       <----- bloom
                // ortho

            }
        }

        if(ssao != null && bloom != null && ssao.isSupported() && ssao.isEnabled()
          && bloom.isSupported() && bloom.isEnabled()) {
            PassManager p = f.getPasses();
            // let bloom depend on ssao too
            p.setPassOrder(bloom.getId(), ssao.getId());
        }
        
        if(!isBuffersSetup) {
            isBuffersSetup = true;
            // create the required buffers
            fb.setupBuffers();
            if(finalFb!=null) {
                finalFb.setupBuffers();
            }
        }
    }

    @Override
    public FrameBuffer getRootFrameBuffer() {
        return finalFb;
    }
    
    @Override
    public void cleanup() {
        
    }


}
