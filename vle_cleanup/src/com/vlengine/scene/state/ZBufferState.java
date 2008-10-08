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
import org.lwjgl.opengl.GL11;

public class ZBufferState extends RenderState {
    
    public static enum TestFunction {
        /**
         * Depth comparison never passes.
         */
        Never(GL11.GL_NEVER),
        /**
         * Depth comparison always passes.
         */
        Always(GL11.GL_ALWAYS),
        /**
         * Passes if the incoming value is the same as the stored value.
         */
        EqualTo(GL11.GL_EQUAL),
        /**
         * Passes if the incoming value is not equal to the stored value.
         */
        NotEqualTo(GL11.GL_NOTEQUAL),
        /**
         * Passes if the incoming value is less than the stored value.
         */
        LessThan(GL11.GL_LESS),
        /**
         * Passes if the incoming value is less than or equal to the stored
         * value.
         */
        LessThanOrEqualTo(GL11.GL_LEQUAL),
        /**
         * Passes if the incoming value is greater than the stored value.
         */
        GreaterThan(GL11.GL_GREATER),
        /**
         * Passes if the incoming value is greater than or equal to the stored
         * value.
         */
        GreaterThanOrEqualTo(GL11.GL_GEQUAL);

        public final int glDepthFunc;
        
        TestFunction(int df) {
            glDepthFunc = df;
        }
    }
    
    /**
     * Depth comparison never passes.
     */
    public static final TestFunction CF_NEVER = TestFunction.Never;
    /**
     * Passes if the incoming value is less than the stored value.
     */
    public static final TestFunction CF_LESS = TestFunction.LessThan;
    /**
     * Passes if the incoming value is the same as the stored value.
     */
    public static final TestFunction CF_EQUAL = TestFunction.EqualTo;
    /**
     * Passes if the incoming value is less than or equal to the stored value.
     */
    public static final TestFunction CF_LEQUAL = TestFunction.LessThanOrEqualTo;
    /**
     * Passes if the incoming value is greater than the stored value.
     */
    public static final TestFunction CF_GREATER = TestFunction.GreaterThan;
    /**
     * Passes if the incoming value is not equal to the stored value.
     */
    public static final TestFunction CF_NOTEQUAL = TestFunction.NotEqualTo;
    /**
     * Passes if the incoming value is greater than or equal to the stored value.
     */
    public static final TestFunction CF_GEQUAL = TestFunction.GreaterThanOrEqualTo;
    /**
     * Depth comparison always passes.
     */
    public static final TestFunction CF_ALWAYS = TestFunction.Always;

    /** Depth function. */
    protected TestFunction function = TestFunction.LessThan;

    /** Depth mask is writable or not. */
    protected boolean writable;

    /**
     * Constructor instantiates a new <code>ZBufferState</code> object. The
     * initial values are CF_LESS and depth writing on.
     *
     */
    public ZBufferState() {
        function = CF_LESS;
        writable = true;
    }

    /**
     * <code>getFunction</code> returns the current depth function.
     * @return the depth function currently used.
     */
    public TestFunction getFunction() {
        return function;
    }

    /**
     * <code>setFunction</code> sets the depth function. If an invalid value is
     * passed, CF_LESS is used.
     * @param function the depth function.
     */
    public void setFunction(TestFunction function) {
        this.function = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>isWritable</code> returns if the depth mask is writable or not.
     * @return true if the depth mask is writable, false otherwise.
     */
    public boolean isWritable() {
        return writable;
    }

    /**
     * <code>setWritable</code> sets the depth mask writable or not.
     * @param writable true to turn on depth writing, false otherwise.
     */
    public void setWritable(boolean writable) {
        this.writable = writable;
        setNeedsRefresh(true);
    }

    /**
     * <code>getType</code> returns the type of renderstate this is.
     * (RS_ZBUFFER).
     * @see com.jme.scene.state.RenderState#getType()
     */
    public int getType() {
        return RS_ZBUFFER;
    }
    
        // called after culling the scene, transfer data to
        // the proper implementation
    @Override
    public void update(RenderContext ctx) {
        if( indep != null )
            return;
        super.update(ctx);
        if( needsRefresh ) {
            ((ZBufferState)impl).enabled = this.enabled;
            ((ZBufferState)impl).needsRefresh = true;

            ((ZBufferState)impl).function = this.function;
            ((ZBufferState)impl).writable = this.writable;
            needsRefresh = false;
        }
    }
        

}
