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
import com.vlengine.scene.state.AlphaBlendState;
import org.lwjgl.opengl.ARBImaging;
import org.lwjgl.opengl.EXTBlendColor;
import org.lwjgl.opengl.EXTBlendEquationSeparate;
import org.lwjgl.opengl.EXTBlendFuncSeparate;
import org.lwjgl.opengl.EXTBlendMinmax;
import org.lwjgl.opengl.EXTBlendSubtract;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

/**
 * <code>LWJGLAlphaState</code> subclasses the AlphaState using the LWJGL API
 * to set OpenGL's alpha state.
 * 
 * @author Mark Powell
 * @author Joshua Slack - reworked for StateRecords.
 * @version $Id: LWJGLAlphaState.java,v 1.10 2007/04/11 18:27:36 nca Exp $
 */
public class LWJGLAlphaBlendState extends AlphaBlendState {

    /**
     * Constructor instantiates a new <code>LWJGLAlphaState</code> object with
     * default values.
     *  
     */
    private static boolean inited = false;
    // support vars
    protected static boolean supportsConstantColor = false;
    protected static boolean supportsEq = false;
    protected static boolean supportsSeparateEq = false;
    protected static boolean supportsSeparateFunc = false;
    protected static boolean supportsMinMax = false;
    protected static boolean supportsSubtract = false;
    
    /**
     * Constructor instantiates a new <code>LWJGLBlendState</code> object with
     * default values.
     */
    public LWJGLAlphaBlendState() {
        super();
        if (!inited) {
            supportsConstantColor = supportsEq = GLContext.getCapabilities().GL_ARB_imaging;
            supportsSeparateFunc = GLContext.getCapabilities().GL_EXT_blend_func_separate;
            supportsSeparateEq = GLContext.getCapabilities().GL_EXT_blend_equation_separate;
            supportsMinMax = GLContext.getCapabilities().GL_EXT_blend_minmax;
            supportsSubtract = GLContext.getCapabilities().GL_EXT_blend_subtract;

            // We're done initing! Wee! :)
            inited = true;
        }
    }

/**
     * @return true if we support setting a constant color for use with
     *         *Constant* type BlendFunctions.
     */
    public static boolean supportsConstantColor() {
        return supportsConstantColor;
    }

    /**
     * @return true if we support setting rgb and alpha functions separately for
     *         source and destination.
     */
    public static boolean supportsSeparateFunctions() {
        return supportsSeparateFunc;
    }

    /**
     * @return true if we support setting the blend equation
     */
    public static boolean supportsEquation() {
        return supportsEq;
    }

    /**
     * @return true if we support setting the blend equation for alpha and rgb
     *         separately
     */
    public static boolean supportsSeparateEquations() {
        return supportsSeparateEq;
    }

    /**
     * @return true if we support using min and max blend equations
     */
    public static boolean supportsMinMaxEquations() {
        return supportsMinMax;
    }

    /**
     * @return true if we support using subtract blend equations
     */
    public static boolean supportsSubtractEquations() {
        return supportsSubtract;
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
        // ask for the current state record
        if (enabled) {
            applyBlendEquations();
            applyBlendColor();
            applyBlendFunctions();            
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }       
    }

    private void applyBlendEquations() {
        GL11.glEnable(GL11.GL_BLEND);
        int blendEqRGB = getGLEquationValue(this.blendEquationRGB);
        int blendEqAlpha = getGLEquationValue(this.blendEquationAlpha);
        if (blendEqRGB!=blendEqAlpha && supportsSeparateEquations()) {
            EXTBlendEquationSeparate.glBlendEquationSeparateEXT(blendEqRGB, blendEqAlpha);
        } else if (supportsEq) {
            ARBImaging.glBlendEquation(blendEqRGB);
        }
    }

    private int getGLEquationValue(BlendEquation eq) {
        switch (eq) {
            case Min:
                if (supportsMinMax)
                    return EXTBlendMinmax.GL_MIN_EXT;
                // FALLS THROUGH
            case Max:
                if (supportsMinMax)
                    return EXTBlendMinmax.GL_MAX_EXT;
                else
                    return ARBImaging.GL_FUNC_ADD;
            case Subtract:
                if (supportsSubtract)
                    return EXTBlendSubtract.GL_FUNC_SUBTRACT_EXT;
                // FALLS THROUGH
            case ReverseSubtract:
                if (supportsSubtract)
                    return EXTBlendSubtract.GL_FUNC_REVERSE_SUBTRACT_EXT;
                // FALLS THROUGH
            case Add:
                return ARBImaging.GL_FUNC_ADD;
        }
        throw new IllegalArgumentException("Invalid blend equation: " + eq);
    }

    private void applyBlendColor() {
        boolean applyConstant = getDestinationFunctionRGB().usesConstantColor
                || getSourceFunctionRGB().usesConstantColor
                || (supportsConstantColor() && (getDestinationFunctionAlpha().usesConstantColor
                || getSourceFunctionAlpha().usesConstantColor));
        if (applyConstant && supportsConstantColor()) {
            float r = 0, g = 0, b = 0, a = 0;
            if (getConstantColor() != null) {
                r = getConstantColor().r;
                g = getConstantColor().g;
                b = getConstantColor().b;
                a = getConstantColor().a;
            }
            EXTBlendColor.glBlendColorEXT(r, g, b, a);
        }
    }

    private void applyBlendFunctions() {
        int glSrcRGB = this.sourceFunctionRGB.glFunction;
        int glDstRGB = this.destinationFunctionRGB.glFunction;
        int glSrcAlpha = this.sourceFunctionAlpha.glFunction;
        int glDstAlpha = this.destinationFunctionAlpha.glFunction;
        if((glSrcRGB!=glSrcAlpha || glDstRGB != glDstAlpha) && supportsSeparateFunctions()) {
            EXTBlendFuncSeparate.glBlendFuncSeparateEXT(glSrcRGB,glDstRGB, glSrcAlpha, glDstAlpha);
        } else {
            GL11.glBlendFunc(glSrcRGB, glDstRGB);
        }
    }

    // do nothing for implementation
    @Override
    public void update(RenderContext ctx) {
        
    }
}