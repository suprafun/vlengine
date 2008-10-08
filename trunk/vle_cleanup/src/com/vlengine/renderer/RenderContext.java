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

package com.vlengine.renderer;

import com.vlengine.app.AppContext;
import com.vlengine.app.frame.Frame;
import com.vlengine.image.Texture;
import com.vlengine.renderer.material.Material;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.lwjgl.LWJGLTextureState;
import com.vlengine.scene.state.shader.ShaderVariable;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexFormat;
import java.util.Arrays;
import java.util.HashMap;
import org.lwjgl.opengl.ARBVertexProgram;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author vear
 */
public class RenderContext {

    // the renderer we curently use for rendering
    protected Renderer renderer;
    
    // the framebuffer we currently use for rendering
    public FrameBuffer fb;
    
    protected RenderQueue que;
    
    // the main application context
    public AppContext app;
    
    // the frame owning this context
    public Frame frame;
    
    // the number of the frame currently drawing
    public int frameId;
    
    // time of last frame
    public float time;
    
    /** List of default states all spatials take if none is set. */
    public final RenderState[] defaultStateList = new RenderState[RenderState.RS_MAX_STATE];
    
        /** List of states that override any set states on a spatial if not null. */
    public final RenderState[] enforcedStateList = new RenderState[RenderState.RS_MAX_STATE];

    /** RenderStates a Spatial contains during rendering. */
    public final RenderState[] currentStates = new RenderState[RenderState.RS_MAX_STATE];
       
    // the current material usage type as requested by the current renderpass
    public int currentPassShaderFlags = 0;
    
    public Material defaultMaterial;
    
    public Material currentMaterial = null;
    public boolean currentMaterialDepthOnly = false;
    
    public ViewCamera currentCamera;
    
    public int currentProgramid = 0;
       
    // shared texture state for shaders shadertextures
    public LWJGLTextureState shTex = new LWJGLTextureState();
    
    // the enabled/disabled state of vertex attribs
    public boolean[] vertexAttribState = new boolean[VertexAttribute.USAGE_MAX];
    // the enabled/disabled shader vertex attribs for each usage
    public IntList shaderAttribState = new IntList();
    
    // the render to rexture textures
    public FastList<Texture> renderTextures = new FastList<Texture>();
    
    // the enforced textures (they are not replaced) by unit they are loaded in
    public Texture[] enforcedTextures;
    
    // position only mode processing
    public boolean positionmode = false;
    
    // the renderer stack
    protected FastList<Renderer> rendererStack = new FastList<Renderer>();
    //protected FastList<FrameBuffer> frameBufferStack = new FastList<FrameBuffer>();
    
    public void clear() {
        for (int i = 0; i < currentStates.length; i++)
            currentStates[i] = null;
        for (int i = 0; i < enforcedStateList.length; i++)
            enforcedStateList[i] = null;
        currentProgramid = 0;
        currentMaterial = null;
        currentCamera = null;
        Arrays.fill(vertexAttribState, false);
        if(enforcedTextures==null) {
            enforcedTextures = new Texture[LWJGLTextureState.getNumberOfFragmentUnits()];
        } else {
            Arrays.fill(enforcedTextures, null);
        }
    }
    
    public Renderer getRenderer() {
        return renderer;
    }
      
    public void setRenderer(Renderer r) {
        this.renderer = r;
    }
   
    public RenderQueue getRenderQueue() {
        return que;
    }
    
    public void setRenderQueue(RenderQueue qu) {
        this.que = qu;
    }

    public void setDefaultStates() {
        Renderer renderer = app.display.getRenderer();
        
        // create the default renderstates, and set it into the default material
        if( renderer!= null && defaultMaterial == null ) {
            defaultMaterial = new Material();
            for (int i = 0; i < RenderState.RS_MAX_STATE; i++) {
                defaultStateList[i] = renderer.createState(i);
                if(defaultStateList[i]!=null) {
                    defaultStateList[i].setEnabled(false);
                    //defaultMaterial.setRenderState(defaultStateList[i]);
                }
            }
            defaultMaterial.states = defaultStateList;
            //defaultMaterial.update(null);
        }
        
    }
    
    public int getFrameId() {
        return frameId;
    }

    public void setEnableVertexAttribute( VertexAttribute.Usage attribNo, boolean state ) {
        if(attribNo.glArrayState < 0)
            return;
        if( vertexAttribState[attribNo.id] != state ) {
            vertexAttribState[attribNo.id] = state;
            if( state )
                GL11.glEnableClientState(attribNo.glArrayState);
            else
                GL11.glDisableClientState(attribNo.glArrayState);
        }
    }
    
    public void setEnableShaderAttribute( int attribId, boolean state) {
        int istate = state ? 1:0;
        if( shaderAttribState.get(attribId) != istate ) {
            //if(attribId==shaderAttribState.size()-1)
            shaderAttribState.set(attribId, istate);
            while(shaderAttribState.size()>0 && shaderAttribState.get(shaderAttribState.size()-1)==0) {
                shaderAttribState.removeElementAt(shaderAttribState.size()-1);
            }
            if( state )
                ARBVertexProgram.glEnableVertexAttribArrayARB(attribId);
            else
                ARBVertexProgram.glDisableVertexAttribArrayARB(attribId);
        }
    }
    
    public void disableUnusedAttribs(IntList usedAttribs) {
        for(int i=0, mi=shaderAttribState.size(); i<mi; i++) {
            if(shaderAttribState.get(i)==1 && usedAttribs.get(i)==0) {
                // this one is unused
                setEnableShaderAttribute(i, false);
            }
        }
    }
    
    /*
    public void pushRenderer() {
        if(this.renderer != null)
            rendererStack.add(this.renderer);
    }
    
    public void popRenderer() {
        if(rendererStack.size() > 0) {
            int idx = rendererStack.size() -1;
            this.renderer = rendererStack.get(idx);
            rendererStack.remove(idx);
        }
    }
     */
/*    
    public void pushFrameBuffer(FrameBuffer pfb) {
        frameBufferStack.add(pfb);
    }
    
    public FrameBuffer popFrameBuffer() {
        if(frameBufferStack.size() > 0) {
            int idx = frameBufferStack.size() -1;
            FrameBuffer pfb = frameBufferStack.get(idx);
            frameBufferStack.remove(idx);
            return pfb;
        }
        return null;
    }
*/    
}
