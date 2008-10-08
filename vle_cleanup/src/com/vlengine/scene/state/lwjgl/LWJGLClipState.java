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
import com.vlengine.scene.state.ClipState;
import com.vlengine.util.geom.BufferUtils;
import java.nio.DoubleBuffer;
import org.lwjgl.opengl.GL11;

/**
 * <code>LWJGLClipState</code>
 * @author Joshua Slack - reworked for StateRecords.
 */
public class LWJGLClipState extends ClipState {

    public DoubleBuffer buf = BufferUtils.createDoubleBuffer(4);

    public LWJGLClipState() {
    }

    /**
     * <code>apply</code>
     * 
     * @see com.jme.scene.state.ClipState#apply()
     */
    public void apply( RenderContext context ) {
        if(indep==null)
                context.currentStates[getType()] = this;

        if (isEnabled()) {
            for (int i = 0; i < MAX_CLIP_PLANES; i++) {
                enableClipPlane(i, enabledClipPlanes[i]);
            }
        } else {
            for (int i = 0; i < MAX_CLIP_PLANES; i++) {
                enableClipPlane(i, false);
            }
        }

    }

    private void enableClipPlane(int planeIndex, boolean enable) {
        if (enable) {
            GL11.glEnable(GL11.GL_CLIP_PLANE0 + planeIndex);

            buf.rewind();
            buf.put(planeEquations[planeIndex]);
            buf.flip();
            GL11.glClipPlane(GL11.GL_CLIP_PLANE0 + planeIndex, buf);
        } else {
            GL11.glDisable(GL11.GL_CLIP_PLANE0 + planeIndex);
        }
    }


    // do nothing for implementation
    @Override
    public void update(RenderContext ctx) {
        
    }
}