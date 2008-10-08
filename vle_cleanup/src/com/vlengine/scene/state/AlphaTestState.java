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

/**
 * <code>AlphaState</code> maintains the state of the alpha values of a
 * particular node and it's children. The alpha state provides a method for
 * blending a source pixel with a destination pixel. The alpha value provides
 * a transparent or translucent surfaces. For example, this would allow for
 * the rendering of green glass. Where you could see all objects behind this
 * green glass but they would be tinted green.
 * @author Mark Powell
 * @version $Id: AlphaState.java,v 1.9 2007/08/02 22:05:39 nca Exp $
 */
public class AlphaTestState extends RenderState {
    
    public enum TestFunction {
        /**
         * Never passes the depth test.
         */
        Never(GL11.GL_NEVER),
        /**
         * Always passes the depth test.
         */
        Always(GL11.GL_ALWAYS),
        /**
         * Pass the test if this alpha is equal to the reference alpha.
         */
        EqualTo(GL11.GL_EQUAL),
        /**
         * Pass the test if this alpha is not equal to the reference alpha.
         */
        NotEqualTo(GL11.GL_NOTEQUAL),
        /**
         * Pass the test if this alpha is less than the reference alpha.
         */
        LessThan(GL11.GL_LESS),
        /**
         * Pass the test if this alpha is less than or equal to the reference
         * alpha.
         */
        LessThanOrEqualTo(GL11.GL_LEQUAL),
        /**
         * Pass the test if this alpha is less than the reference alpha.
         */
        GreaterThan(GL11.GL_GREATER),
        /**
         * Pass the test if this alpha is less than or equal to the reference
         * alpha.
         */
        GreaterThanOrEqualTo(GL11.GL_GEQUAL);

        public final int glFunction;
        
        TestFunction(int glf) {
            glFunction=glf;
        }
    }

    //test functions
    /**
     * Never passes the depth test.
     */
    public final static TestFunction TF_NEVER = TestFunction.Never;
    /**
     * Pass the test if this alpha is less than the reference alpha.
     */
    public final static TestFunction TF_LESS = TestFunction.LessThan;
    /**
     * Pass the test if this alpha is equal to the reference alpha.
     */
    public final static TestFunction TF_EQUAL = TestFunction.EqualTo;
    /**
     * Pass the test if this alpha is less than or equal to the reference alpha.
     */
    public final static TestFunction TF_LEQUAL = TestFunction.LessThanOrEqualTo;
    /**
     * Pass the test if this alpha is greater than the reference alpha.
     */
    public final static TestFunction TF_GREATER = TestFunction.GreaterThan;
    /**
     * Pass the test if this alpha is not equal to the reference alpha.
     */
    public final static TestFunction TF_NOTEQUAL = TestFunction.NotEqualTo;
    /**
     * Pass the test if this alpha is greater than or equal to the reference
     * alpha.
     */
    public final static TestFunction TF_GEQUAL = TestFunction.GreaterThanOrEqualTo;
    /**
     * Always passes the depth test.
     */
    public final static TestFunction TF_ALWAYS = TestFunction.Always;

    /** Alpha test value. */
    protected TestFunction testFunction = TestFunction.Always;
    /** The reference value to which incoming alpha values are compared. */
    protected float reference;

    /**
     * Constructor instantiates a new <code>AlphaState</code> object with
     * default values.
     *
     */
    public AlphaTestState() {
    }
    
    /**
     * <code>getType</code> returns the type of render state this is.
     * (RS_ALPHA).
     * @see com.jme.scene.state.RenderState#getType()
     */
    public int getType() {
        return RS_ALPHATEST;
    }

/**
     * <code>setTestFunction</code> sets the testing function used for the
     * alpha testing. If an invalid value is passed, the default TF_ALWAYS is
     * used.
     * 
     * @param function
     *            the testing function used for the alpha testing.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setTestFunction(TestFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        testFunction = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>getTestFunction</code> returns the testing function used for the
     * alpha testing.
     * 
     * @return the testing function used for the alpha testing.
     */
    public TestFunction getTestFunction() {
        return testFunction;
    }

    /**
     * <code>setReference</code> sets the reference value that incoming alpha
     * values are compared to when doing alpha testing. This is clamped to [0, 1].
     * 
     * @param reference
     *            the reference value that alpha values are compared to.
     */
    public void setReference(float reference) {
        if (reference < 0) {
            reference = 0;
        }

        if (reference > 1) {
            reference = 1;
        }
        this.reference = reference;
        setNeedsRefresh(true);
    }

    /**
     *
     * <code>getReference</code> returns the reference value that incoming
     * alpha values are compared to.
     * @return the reference value that alpha values are compared to.
     */
    public float getReference() {
        return reference;
    }

    @Override
    public void update(RenderContext ctx) {
        if( indep != null )
            return;
        // create implementation for this state
        super.update(ctx);
        if( needsRefresh ) {
            ((AlphaTestState)impl).enabled = this.enabled;
            ((AlphaTestState)impl).needsRefresh = true;

            ((AlphaTestState)impl).testFunction = this.testFunction;
            ((AlphaTestState)impl).reference = this.reference;
            
            needsRefresh = false;
        }
    }

}
