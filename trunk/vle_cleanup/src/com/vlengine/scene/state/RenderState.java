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

package com.vlengine.scene.state;

import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.CullContext;

public abstract class RenderState {

	/** The value returned by getType() for AlphaState. */
	public final static int RS_ALPHABLEND = 0;
        
        // alpha test state
        public final static int RS_ALPHATEST = 1;

	/** The value returend by getType() for DitherState. */
	public final static int RS_DITHER = 2;

	/** The value returned by getType() for FogState. */
	public final static int RS_FOG = 3;

	/** The value returned by getType() for LightState. */
	public final static int RS_LIGHT = 4;

	/** The value returend by getType() for MaterialState. */
	public final static int RS_MATERIAL = 5;

	/** The value returned by getType() for ShadeState. */
	public final static int RS_SHADE = 6;

	/** The value returned by getType() for TextureState. */
	public final static int RS_TEXTURE = 7;

	/** The value returned by getType() for WireframeState. */
	public final static int RS_WIREFRAME = 8;

	/** The value returned by getType() for ZBufferState. */
	public final static int RS_ZBUFFER = 9;

	/** The value returned by getType() for CullState. */
	public final static int RS_CULL = 10;

	/** The value returned by getType() for VertexProgramState. */
	public final static int RS_VERTEX_PROGRAM = 11;

	/** The value returned by getType() for FragmentProgramState. */
	public final static int RS_FRAGMENT_PROGRAM = 12;

	/** The value returned by getType() for AttributeState. */
	public final static int RS_ATTRIBUTE = 13;

	/** The value returned by getType() for StencilState. */
	public final static int RS_STENCIL = 14;
	
	/** The value returned by getType() for ShaderObjectsState. */
	public final static int RS_GLSL_SHADER_OBJECTS = 15;
        
    /** The value returned by getType() for ColorMaskState. */    
    public static final int RS_COLORMASK_STATE = 16; 

    /** The value returned by getType() for ClipState. */
    public static final int RS_CLIP = 17;

    // attributes, uniforms and textures for shader
        public final static int RS_GLSL_SHADER_PARAM = 18;
        
        // line parameters
        public final static int RS_LINE = 19;
        
    /** The total number of diffrent types of RenderState. */
    public final static int RS_MAX_STATE = 20;
    
    // the implementing renderstate for this state
    // this is needed, so that the scene can contain an implementation
    // independent version of the renderstate, and can update it safely in multiple
    // threads. when the scene is ready to render, the data is transferred to
    // implementing state
    protected RenderState impl;
    
    // the implementation independent pair of this renderstate
    protected RenderState indep;
    
    protected boolean enabled = true;

    protected boolean needsRefresh = false;

	/**
	 * Construts a new RenderState. The state is enabled by default.
	 */
	public RenderState() {
	}

	/**
	 * Defined by the subclass, this returns an int identifying the renderstate.
	 * For example, RS_CULL or RS_TEXTURE.
	 * 
	 * @return An int identifying this render state.
	 */
	public abstract int getType();

	/**
	 * Returns if this render state is enabled during rendering. Disabled states
	 * are ignored.
	 * 
	 * @return True if this state is enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

    /**
     * Sets if this render state is enabled during rendering. Disabled states
     * are ignored.
     * 
     * @param value
     *            False if the state is to be disabled, true otherwise.
     */
    public void setEnabled(boolean value) {
        this.enabled = value;
        setNeedsRefresh(true);
    }

        public void update( RenderContext ctx ) {
            // create implementation for this state
            if( impl == null) {
                impl = ctx.getRenderer().createState( getType() );
                impl.indep = this;
                needsRefresh = true;
            }
        }
        
	/**
	 * This function is defined in the RenderState that is actually used by the
	 * Renderer. It contains the code that, when executed, applies the render
	 * state for the given render system. This should only be called internally
	 * and not by users directly.
	 */
        public void apply(RenderContext ctx) {
            if( impl != null ) {
                ctx.currentStates[getType()] = this;
                impl.apply(ctx);
            }
        }   

    /**
     * @return true if we should apply this state even if we think it is the
     *         current state of its type in the current context. Is reset to
     *         false after apply is finished.
     */
    public boolean isNeedRefresh() {
        return needsRefresh;
    }
    
    /**
     * This should be called by states when it knows internal data has been altered.
     * 
     * @param refresh true if we should apply this state even if we think it is the
     *         current state of its type in the current context.
     */ 
    public void setNeedsRefresh(boolean refresh) {
        needsRefresh  = refresh;
    }
    
}