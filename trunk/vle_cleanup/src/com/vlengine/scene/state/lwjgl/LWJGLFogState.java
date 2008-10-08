/*
 * Copyright (c) 2003-2008 jMonkeyEngine, VL Engine
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

import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.state.FogState;
import com.vlengine.thread.LocalContext;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL11;


/**
 * <code>LWJGLFogState</code> subclasses the fog state using the LWJGL API to
 * set the OpenGL fog state.
 * 
 * @author Mark Powell
 * @author Joshua Slack - reworked for StateRecords.
 * @author vear (Arpad Vekas) reworked to VL engine
 */
public class LWJGLFogState extends FogState {
	

	/**
	 * Constructor instantiates a new <code>LWJGLFogState</code> object with
	 * default values.
	 *  
	 */
	public LWJGLFogState() {
		super();
	}

	/**
	 * <code>set</code> sets the OpenGL fog values if the state is enabled.
	 * 
	 * @see com.jme.scene.state.RenderState#apply()
	 */
    @Override
    public void apply(RenderContext context) {
        if(indep==null)
            context.currentStates[getType()] = this;

            if (isEnabled()) {
                enableFog(true);

                GL11.glFogf(GL11.GL_FOG_START, start);
                GL11.glFogf(GL11.GL_FOG_END, end);
                GL11.glFogf(GL11.GL_FOG_DENSITY, density);

                applyFogColor(getColor());
                applyFogMode(densityFunction);
                applyFogHint(quality);
            } else {
                enableFog(false);
            }

    }

    private void enableFog(boolean enable) {
        if (enable) {
            GL11.glEnable(GL11.GL_FOG);
        } else {
            GL11.glDisable(GL11.GL_FOG);
        }            
    }

    private void applyFogColor(ColorRGBA color) {
        FloatBuffer colorBuff = LocalContext.getContext().tLWJGLFogState_colorBuff;
        colorBuff.clear();
        colorBuff.put(color.r).put(color.g).put(color.b).put(color.a);
        colorBuff.flip();
        GL11.glFog(GL11.GL_FOG_COLOR, colorBuff);
    }

    private void applyFogMode(DensityFunction densityFunction) {
        int glMode = 0;
        switch (densityFunction) {
            case Exponential:
                glMode = GL11.GL_EXP;
                break;
            case Linear:
                glMode = GL11.GL_LINEAR;
                break;
            case ExponentialSquared:
                glMode = GL11.GL_EXP2;
                break;
        }
        
        GL11.glFogi(GL11.GL_FOG_MODE, glMode);
    }

    private void applyFogHint(Quality quality) {
        int glHint = 0;
        switch (quality) {
            case PerVertex:
                glHint = GL11.GL_FASTEST;
                break;
            case PerPixel:
                glHint = GL11.GL_NICEST;
                break;
        }
        
        GL11.glHint(GL11.GL_FOG_HINT, glHint);
    }

    // do nothing for implementation
    @Override
    public void update(RenderContext ctx) {
        
    }
    
}