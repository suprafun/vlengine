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
import com.vlengine.scene.state.WireframeState;
import org.lwjgl.opengl.GL11;


public class LWJGLWireframeState extends WireframeState {

	public int smoothHint = -1;

	/**
	 * <code>set</code> sets the polygon mode to line or fill depending on if
	 * the state is enabled or not.
	 * 
	 * @see com.jme.scene.state.WireframeState#apply()
	 */
    @Override
	public void apply(RenderContext context) {
            needsRefresh = false;
            if(indep==null)
                context.currentStates[getType()] = this;
            if (isEnabled()) {
                GL11.glLineWidth(lineWidth);
                if (antialiased) {
                    GL11.glEnable(GL11.GL_LINE_SMOOTH);
                    GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
                } else {
                    GL11.glDisable(GL11.GL_LINE_SMOOTH);
                }
                GL11.glDisable(GL11.GL_LINE_STIPPLE);
                switch (face) {
                    case WS_FRONT:
                        applyPolyMode(GL11.GL_LINE, GL11.GL_FILL);
                        break;
                    case WS_BACK:
                        applyPolyMode(GL11.GL_FILL, GL11.GL_LINE);
                        break;
                    case WS_FRONT_AND_BACK:
                    default:
                        applyPolyMode(GL11.GL_LINE, GL11.GL_LINE);
                        break;
                }
            } else {
                applyPolyMode(GL11.GL_FILL, GL11.GL_FILL);
            }
	}

    private void applyPolyMode(int frontMode, int backMode) {
        if (frontMode == backMode) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, frontMode);
        } else if (frontMode != backMode) {
            GL11.glPolygonMode(GL11.GL_FRONT, frontMode);
            GL11.glPolygonMode(GL11.GL_BACK, backMode);
        }
    }
    
    // do nothing for implementation
    @Override
    public void update(RenderContext ctx) {}
}