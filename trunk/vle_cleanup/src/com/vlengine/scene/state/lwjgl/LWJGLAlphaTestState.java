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

package com.vlengine.scene.state.lwjgl;

import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.state.AlphaTestState;
import org.lwjgl.opengl.GL11;

/**
 * <code>LWJGLAlphaTestState</code> subclasses the AlphaTestState using the LWJGL API
 * to set OpenGL's alpha state.
 * 
 * @author Mark Powell
 * @author Joshua Slack - reworked for StateRecords.
 * @author vear (Arpad Vekas) - reworked to VL engine, and separated from alpha blend state
 */
public class LWJGLAlphaTestState extends AlphaTestState {

    /**
     * Constructor instantiates a new <code>LWJGLAlphaState</code> object with
     * default values.
     *  
     */
    public LWJGLAlphaTestState() {
            super();
    }

    /**
     * <code>set</code> is called to set the alpha state. If blending is
     * enabled, the blend function is set up and if alpha testing is enabled the
     * alpha functions are set.
     * 
     * @see com.jme.scene.state.RenderState#apply()
     */
    @Override
    public void apply(RenderContext context) {
        if(indep==null)
            context.currentStates[getType()] = this;
        applyTest();
    }
    
    protected void applyTest() {
        if (enabled) {
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            int glFunc = testFunction.glFunction;
            GL11.glAlphaFunc(glFunc, reference);
        } else {
            GL11.glDisable(GL11.GL_ALPHA_TEST);
        }
    }

    // do nothing for implementation
    @Override
    public void update(RenderContext ctx) {
        
    }
}