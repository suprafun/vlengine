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

import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.FrameBuffer;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.Renderer;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.renderer.material.Material;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.state.AlphaBlendState;
import com.vlengine.scene.state.FogState;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.lwjgl.LWJGLAlphaBlendState;
import com.vlengine.scene.state.lwjgl.LWJGLFogState;
import com.vlengine.util.FastList;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class RenderPass {
    public static String shaderDirectory = "com/vlengine/renderer/pass/data/";
    
    public static enum StandardPass {
        Opaque(0),
        AlphaTested(1),
        Ortho(2),
        AlphaBlended(3),
        BackGround(4),
        Ligh(5),
        Depth(6),
        AlphaTestedDepth(7),
        User(8)
        ;
        
        public final int passId;
        
        StandardPass(int pId) {
            passId = pId;
        }
    }

    public static enum PassFilter {
        None(0),
        Opaque(1<<StandardPass.Opaque.passId),
        AlphaTested(1<<StandardPass.AlphaTested.passId),
        Ortho(1<<StandardPass.Ortho.passId),
        AlphaBlended(1<<StandardPass.AlphaBlended.passId),
        BackGround(1<<StandardPass.BackGround.passId),
        Light(1<<StandardPass.Ligh.passId),
        Any(Long.MAX_VALUE)
        ;
        
        public final long value;
        PassFilter(long v) {
            value = v;
        }
    }
    
    /*
    public static final int PASS_OPAQUE = 0;
    public static final int PASS_TRANSPARENT = 1;
    public static final int PASS_ORTHO = 2;
    public static final int PASS_TWOSIDED = 3;
    public static final int PASS_BACKGROUND = 4;
    public static final int PASS_LIGHT = 5;
    public static final int PASS_DEPTH = 6;
    public static final int PASS_USER0 = 7;
    
    public final static int QUE_OPAQUE = 0;
    public final static int QUE_TRANSPARENT = 1;
    public final static int QUE_ORTHO = 2;
    // use two-sided transparent rendering
    public final static int QUE_TWOSIDED = 3;
    public final static int QUE_BACKGROUND = 4;
    public final static int QUE_LIGHT = 5;
    public final static int QUE_USER0 = 6;
     */
    
    // the unique id of this renderpass
    protected int id;
    
    // the name of the pass
    protected String name;
    
    // is this pass enbaled
    protected boolean enabled = false;
    
    // the camera used in the pass
    protected ViewCamera camera;
    
    // the queue this pass uses
    // if -1, then no queue is used
    protected int queueNo = -1;
    
    // the filter for selecting objects
    // objects are put into queue queueNo if RenderPass.getQueueFilter() & Spatial.getRenderQueueMode() != 0
    protected long queueFilter = 0;
    
    // the material this pass uses 
    protected int materialNo = -1;
    
    // the renderer this pass uses for rendering
    //protected Renderer renderer;
    
    // do we apply depth fog in this pass
    protected boolean alphablend = false;
    
    // the target framebuffer
    protected FrameBuffer target;
    
    public RenderPass(String name) {
        this.name = name;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    /*
    public void setRenderer(Renderer r) {
        this.renderer = r;
    }
    
    public Renderer getRenderer() {
        return renderer;
    }
     */
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public ViewCamera getCamera() {
        return camera;
    }
    
    public void setCamera(ViewCamera cam) {
        camera = cam;
    }
    
    public void setQueueNo( int no ) {
        this.queueNo = no;
    }
    
    public int getQueueNo() {
        return this.queueNo;
    }
    
    public void setUsedMaterialFlags(int matno) {
        this.materialNo = matno;
    }
    
    public int getUsedMaterialFlags() {
        return materialNo;
    }
    
    public long getQueueFilter() {
        return this.queueFilter;
    }
    
    public void setQueueFilter(long filter) {
        queueFilter = filter;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnableAlphaBlend(boolean blend) {
        alphablend=blend;
    }

    public void setTarget(FrameBuffer target) {
        this.target = target;
    }

    public FrameBuffer getTarget() {
        return target;
    }
    
    /**
     * This method is called by the engine when the application finishes.
     */
    public void cleanup() {
        
    }
    
    AlphaBlendState as;
    FogState fs;
    
    public void renderPass(RenderContext ctx) {
        if(!enabled)
            return;
        
        // get the list of renderables, dont need any sorting
        FastList<Renderable> list = ctx.getRenderQueue().getQueue(this.queueNo);
        if( list ==null || list.size() == 0 )
            return;
        
        //renderer = ctx.getRenderer();

        // save previous material flags
        int prevMaterialFlags = ctx.currentPassShaderFlags;
        if( materialNo != -1 ) {
            ctx.currentPassShaderFlags = materialNo;
        } else {
            // no special shader is needed
            ctx.currentPassShaderFlags = 0;
        }

        // get the renderer
        Renderer renderer = ctx.getRenderer();
        
        if(alphablend) {
            // enforce alpha state
            if(as == null) {
                as = (LWJGLAlphaBlendState) renderer.createState(RenderState.RS_ALPHABLEND);
                as.setSourceFunction(AlphaBlendState.SB_SRC_ALPHA);
                as.setDestinationFunction(AlphaBlendState.DB_ONE_MINUS_SRC_ALPHA);

                //as.setSrcFunction(AlphaState.SB_DST_COLOR);
                //as.setDstFunction(AlphaState.DB_ZERO);
                as.setEnabled(true);
            }
            if(fs == null) {
                fs = (LWJGLFogState) renderer.createState(RenderState.RS_FOG);
                fs.setStart(ctx.app.conf.view_frustrum_far*ctx.app.conf.graphDepthFogStart);
                //fs.setStart(ctx.app.conf.view_frustrum_far*(2f/3f));
                fs.setEnd(ctx.app.conf.view_frustrum_far);
                fs.setDensity(ctx.app.conf.graphDepthFogDensity);
                fs.setDensityFunction(FogState.DensityFunction.ExponentialSquared);
                fs.setQuality(FogState.Quality.PerVertex);
                fs.setColor(new ColorRGBA(0.6f,0.6f,0.8f,0f));
                fs.setEnabled(true);
            }
            ctx.enforcedStateList[RenderState.RS_ALPHABLEND] = as;
            ctx.enforcedStateList[RenderState.RS_FOG] = fs;
        }
        
        // draw all the elements
        for(int i=0, ls=list.size(); i<ls; i++) {
            Renderable e= list.get(i);
            if( materialNo != -1 ) {
                // apply material
                Material mat = e.getMaterial();
                if(mat == null)
                    mat = ctx.defaultMaterial;
                if( mat!= null 
                  && ( ctx.currentMaterial != mat 
                  || ctx.currentMaterialDepthOnly != ctx.positionmode)
                  )
                    mat.apply(ctx);
            }
            e.draw(ctx);
        }
        if(alphablend) {
            ctx.enforcedStateList[RenderState.RS_ALPHABLEND] = null;
            ctx.enforcedStateList[RenderState.RS_FOG] = null;
        }
        ctx.currentPassShaderFlags = prevMaterialFlags;
        
    }
}
