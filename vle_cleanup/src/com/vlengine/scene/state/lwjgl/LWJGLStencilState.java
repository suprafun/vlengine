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
import com.vlengine.scene.state.StencilState;
import org.lwjgl.opengl.GL11;


/**
 * <code>LWJGLStencilState</code>
 * 
 * @author Mark Powell
 * @author Joshua Slack - reworked for StateRecords.
 * @version $Id: LWJGLStencilState.java,v 1.9 2007/04/11 18:27:36 nca Exp $
 */
public class LWJGLStencilState extends StencilState {
	private static final long serialVersionUID = 2L;

    private static int[] stencilFunc = { GL11.GL_NEVER, GL11.GL_LESS,
            GL11.GL_LEQUAL, GL11.GL_GREATER, GL11.GL_GEQUAL, GL11.GL_EQUAL,
            GL11.GL_NOTEQUAL, GL11.GL_ALWAYS };

    private static int[] stencilOp = { GL11.GL_KEEP, GL11.GL_ZERO,
            GL11.GL_REPLACE, GL11.GL_INCR, GL11.GL_DECR, GL11.GL_INVERT };

    @Override
    public void apply(RenderContext context) {
        if(indep==null)
                context.currentStates[getType()] = this;

        setEnable(isEnabled());
        if (isEnabled()) {
            applyMask(getStencilWriteMask());
            applyFunc(stencilFunc[getStencilFunc()], getStencilRef(),
                    getStencilFuncMask());
            applyOp(stencilOp[getStencilOpFail()],
                    stencilOp[getStencilOpZFail()],
                    stencilOp[getStencilOpZPass()]);
        }
        
    }

    private void setEnable(boolean enable) {
        if (enable)
            GL11.glEnable(GL11.GL_STENCIL_TEST);
        else
            GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private void applyMask(int writeMask) {
        GL11.glStencilMask(writeMask);
    }

    private void applyFunc(int glfunc, int stencilRef, int funcMask) {
        GL11.glStencilFunc(glfunc, stencilRef, funcMask);
    }

    private void applyOp(int fail, int zfail, int zpass) {
        GL11.glStencilOp(fail, zfail, zpass);
    }
    

    // do nothing for implementation
    @Override
    public void update(RenderContext ctx) {
        
    }
}