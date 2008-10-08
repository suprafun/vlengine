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

import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderContext;
import org.lwjgl.opengl.EXTBlendColor;
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
public class AlphaBlendState extends RenderState {
    
    
    public enum SourceFunction {
        /**
         * The source value of the blend function is all zeros.
         */
        Zero(false, GL11.GL_ZERO),
        /**
         * The source value of the blend function is all ones.
         */
        One(false, GL11.GL_ONE),
        /**
         * The source value of the blend function is the destination color.
         */
        DestinationColor(false, GL11.GL_DST_COLOR),
        /**
         * The source value of the blend function is 1 - the destination color.
         */
        OneMinusDestinationColor(false, GL11.GL_ONE_MINUS_DST_COLOR),
        /**
         * The source value of the blend function is the source alpha value.
         */
        SourceAlpha(false, GL11.GL_SRC_ALPHA),
        /**
         * The source value of the blend function is 1 - the source alpha value.
         */
        OneMinusSourceAlpha(false, GL11.GL_ONE_MINUS_SRC_ALPHA),
        /**
         * The source value of the blend function is the destination alpha.
         */
        DestinationAlpha(false, GL11.GL_DST_ALPHA),
        /**
         * The source value of the blend function is 1 - the destination alpha.
         */
        OneMinusDestinationAlpha(false, GL11.GL_ONE_MINUS_DST_ALPHA),
        /**
         * The source value of the blend function is the minimum of alpha or 1 -
         * alpha.
         */
        SourceAlphaSaturate(false, GL11.GL_SRC_ALPHA_SATURATE),
        /**
         * The source value of the blend function is the value of the constant
         * color. (Rc, Gc, Bc, Ac) If not set, black with alpha = 0 is used. If
         * not supported, falls back to One.
         */
        ConstantColor(true, EXTBlendColor.GL_CONSTANT_COLOR_EXT),
        /**
         * The source value of the blend function is 1 minus the value of the
         * constant color. (1-Rc, 1-Gc, 1-Bc, 1-Ac) If color is not set, black
         * with alpha = 0 is used. If not supported, falls back to One.
         */
        OneMinusConstantColor(true, EXTBlendColor.GL_ONE_MINUS_CONSTANT_COLOR_EXT),
        /**
         * The source value of the blend function is the value of the constant
         * color's alpha. (Ac, Ac, Ac, Ac) If not set, black with alpha = 0 is
         * used. If not supported, falls back to One.
         */
        ConstantAlpha(true, EXTBlendColor.GL_CONSTANT_ALPHA_EXT),
        /**
         * The source value of the blend function is 1 minus the value of the
         * constant color's alpha. (1-Ac, 1-Ac, 1-Ac, 1-Ac) If color is not set,
         * black with alpha = 0 is used. If not supported, falls back to One.
         */
        OneMinusConstantAlpha(true, EXTBlendColor.GL_ONE_MINUS_CONSTANT_ALPHA_EXT);
        
        public final boolean usesConstantColor;
        public final int glFunction;
        private SourceFunction(boolean usesConstantColor, int glf) {
            this.usesConstantColor = usesConstantColor;
            glFunction = glf;
        }
    }

    //source functions
    /**
     * The source value of the blend function is all zeros.
     */
    public final static SourceFunction SB_ZERO = SourceFunction.Zero;
    /**
     * The source value of the blend function is all ones.
     */
    public final static SourceFunction SB_ONE = SourceFunction.One;
    /**
     * The source value of the blend function is the destination color.
     */
    public final static SourceFunction SB_DST_COLOR = SourceFunction.DestinationColor;
    /**
     * The source value of the blend function is 1 - the destination color.
     */
    public final static SourceFunction SB_ONE_MINUS_DST_COLOR = SourceFunction.OneMinusDestinationColor;
    /**
     * The source value of the blend function is the source alpha value.
     */
    public final static SourceFunction SB_SRC_ALPHA = SourceFunction.SourceAlpha;
    /**
     * The source value of the blend function is 1 - the source alpha value.
     */
    public final static SourceFunction SB_ONE_MINUS_SRC_ALPHA = SourceFunction.OneMinusConstantAlpha;
    /**
     * The source value of the blend function is the destination alpha.
     */
    public final static SourceFunction SB_DST_ALPHA = SourceFunction.DestinationAlpha;
    /**
     * The source value of the blend function is 1 - the destination alpha.
     */
    public final static SourceFunction SB_ONE_MINUS_DST_ALPHA = SourceFunction.OneMinusDestinationAlpha;
    /**
     * The source value of the blend function is the minimum of alpha or
     * 1 - alpha.
     */
    public final static SourceFunction SB_SRC_ALPHA_SATURATE = SourceFunction.SourceAlphaSaturate;

    public enum DestinationFunction {
        /**
         * The destination value of the blend function is all zeros.
         */
        Zero(false, GL11.GL_ZERO),
        /**
         * The destination value of the blend function is all ones.
         */
        One(false, GL11.GL_ONE),
        /**
         * The destination value of the blend function is the source color.
         */
        SourceColor(false, GL11.GL_SRC_COLOR),
        /**
         * The destination value of the blend function is 1 - the source color.
         */
        OneMinusSourceColor(false, GL11.GL_ONE_MINUS_SRC_COLOR),
        /**
         * The destination value of the blend function is the source alpha
         * value.
         */
        SourceAlpha(false, GL11.GL_SRC_ALPHA),
        /**
         * The destination value of the blend function is 1 - the source alpha
         * value.
         */
        OneMinusSourceAlpha(false, GL11.GL_ONE_MINUS_SRC_ALPHA),
        /**
         * The destination value of the blend function is the destination alpha
         * value.
         */
        DestinationAlpha(false, GL11.GL_DST_ALPHA),
        /**
         * The destination value of the blend function is 1 - the destination
         * alpha value.
         */
        OneMinusDestinationAlpha(false, GL11.GL_ONE_MINUS_DST_ALPHA),
        /**
         * The destination value of the blend function is the value of the
         * constant color. (Rc, Gc, Bc, Ac) If not set, black with alpha = 0 is
         * used. If not supported, falls back to One.
         */
        ConstantColor(true, EXTBlendColor.GL_CONSTANT_COLOR_EXT),
        /**
         * The destination value of the blend function is 1 minus the value of
         * the constant color. (1-Rc, 1-Gc, 1-Bc, 1-Ac) If color is not set,
         * black with alpha = 0 is used. If not supported, falls back to One.
         */
        OneMinusConstantColor(true, EXTBlendColor.GL_ONE_MINUS_CONSTANT_COLOR_EXT),
        /**
         * The destination value of the blend function is the value of the
         * constant color's alpha. (Ac, Ac, Ac, Ac) If not set, black with alpha =
         * 0 is used. If not supported, falls back to One.
         */
        ConstantAlpha(true, EXTBlendColor.GL_CONSTANT_ALPHA_EXT),
        /**
         * The destination value of the blend function is 1 minus the value of
         * the constant color's alpha. (1-Ac, 1-Ac, 1-Ac, 1-Ac) If color is not set,
         * black with alpha = 0 is used. If not supported, falls back to One.
         */
        OneMinusConstantAlpha(true, EXTBlendColor.GL_ONE_MINUS_CONSTANT_ALPHA_EXT);

        public final boolean usesConstantColor;
        public final int glFunction;
        
        private DestinationFunction(boolean usesConstantColor, int glf) {
            this.usesConstantColor = usesConstantColor;
            glFunction = glf;
        }
    }

    //destination functions
    /**
     * The destination value of the blend function is all zeros.
     */
    public final static DestinationFunction DB_ZERO = DestinationFunction.Zero;
    /**
     * The destination value of the blend function is all ones.
     */
    public final static DestinationFunction DB_ONE = DestinationFunction.One;
    /**
     * The destination value of the blend function is the source color.
     */
    public final static DestinationFunction DB_SRC_COLOR = DestinationFunction.SourceColor;
    /**
     * The destination value of the blend function is 1 - the source color.
     */
    public final static DestinationFunction DB_ONE_MINUS_SRC_COLOR = DestinationFunction.OneMinusSourceColor;
    /**
     * The destination value of the blend function is the source alpha value.
     */
    public final static DestinationFunction DB_SRC_ALPHA = DestinationFunction.SourceAlpha;
    /**
     * The destination value of the blend function is 1 - the source alpha value.
     */
    public final static DestinationFunction DB_ONE_MINUS_SRC_ALPHA = DestinationFunction.OneMinusSourceAlpha;
    /**
     * The destination value of the blend function is the destination alpha value.
     */
    public final static DestinationFunction DB_DST_ALPHA = DestinationFunction.DestinationAlpha;
    /**
     * The destination value of the blend function is 1 - the destination alpha
     * value.
     */
    public final static DestinationFunction DB_ONE_MINUS_DST_ALPHA = DestinationFunction.OneMinusDestinationAlpha;

    public enum BlendEquation {
        /**
         * Sets the blend equation so that the source and destination data are
         * added. (Default) Clamps to [0,1] Useful for things like antialiasing
         * and transparency.
         */
        Add,
        /**
         * Sets the blend equation so that the source and destination data are
         * subtracted (Src - Dest). Clamps to [0,1] Falls back to Add if
         * supportsSubtract is false.
         */
        Subtract,
        /**
         * Same as Subtract, but the order is reversed (Dst - Src). Clamps to
         * [0,1] Falls back to Add if supportsSubtract is false.
         */
        ReverseSubtract,
        /**
         * sets the blend equation so that each component of the result color is
         * the minimum of the corresponding components of the source and
         * destination colors. This and Max are useful for applications that
         * analyze image data (image thresholding against a constant color, for
         * example). Falls back to Add if supportsMinMax is false.
         */
        Min,
        /**
         * sets the blend equation so that each component of the result color is
         * the maximum of the corresponding components of the source and
         * destination colors. This and Min are useful for applications that
         * analyze image data (image thresholding against a constant color, for
         * example). Falls back to Add if supportsMinMax is false.
         */
        Max;
    }
    
    /** The blend color used in constant blend operations. */
    protected ColorRGBA constantColor = null;
    
    /** The current source blend function. */
    protected SourceFunction sourceFunctionRGB = SourceFunction.SourceAlpha;
    /** The current destiantion blend function. */
    protected DestinationFunction destinationFunctionRGB = DestinationFunction.OneMinusSourceAlpha;
    /** The current blend equation. */
    protected BlendEquation blendEquationRGB = BlendEquation.Add;
    
    /** The current source blend function. */
    protected SourceFunction sourceFunctionAlpha = SourceFunction.SourceAlpha;
    /** The current destiantion blend function. */
    protected DestinationFunction destinationFunctionAlpha = DestinationFunction.OneMinusSourceAlpha;
    /** The current blend equation. */
    protected BlendEquation blendEquationAlpha = BlendEquation.Add;

    /**
     * Constructor instantiates a new <code>AlphaState</code> object with
     * default values.
     *
     */
    public AlphaBlendState() {
    }
    /**
     * <code>getType</code> returns the type of render state this is.
     * (RS_ALPHA).
     * @see com.jme.scene.state.RenderState#getType()
     */
    public int getType() {
        return RS_ALPHABLEND;
    }

    /**
     * <code>setSrcFunction</code> sets the source function for the blending
     * equation for both rgb and alpha values.
     * 
     * @param function
     *            the source function for the blending equation.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setSourceFunction(SourceFunction function) {
        setSourceFunctionRGB(function);
        setSourceFunctionAlpha(function);
    }

    /**
     * <code>setSrcFunction</code> sets the source function for the blending
     * equation. If supportsSeparateFunc is false, this value will be used for
     * RGB and Alpha.
     * 
     * @param function
     *            the source function for the blending equation.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setSourceFunctionRGB(SourceFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        sourceFunctionRGB = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>setSourceFunctionAlpha</code> sets the source function for the blending
     * equation used with alpha values.
     * 
     * @param function
     *            the source function for the blending equation for alpha values.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setSourceFunctionAlpha(SourceFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        sourceFunctionAlpha = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>getSourceFunction</code> returns the source function for the
     * blending function.
     * 
     * @return the source function for the blending function.
     */
    public SourceFunction getSourceFunctionRGB() {
        return sourceFunctionRGB;
    }

    /**
     * <code>getSourceFunction</code> returns the source function for the
     * blending function.
     * 
     * @return the source function for the blending function.
     */
    public SourceFunction getSourceFunctionAlpha() {
        return sourceFunctionAlpha;
    }

    /**
     * <code>setDestinationFunction</code> sets the destination function for
     * the blending equation for both Alpha and RGB values.
     * 
     * @param function
     *            the destination function for the blending equation.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setDestinationFunction(DestinationFunction function) {
        setDestinationFunctionRGB(function);
        setDestinationFunctionAlpha(function);
    }

    /**
     * <code>setDestinationFunctionRGB</code> sets the destination function
     * for the blending equation. If supportsSeparateFunc is false, this value
     * will be used for RGB and Alpha.
     * 
     * @param function
     *            the destination function for the blending equation for RGB
     *            values.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setDestinationFunctionRGB(DestinationFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        destinationFunctionRGB = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>setDestinationFunctionAlpha</code> sets the destination function
     * for the blending equation.
     * 
     * @param function
     *            the destination function for the blending equation for Alpha
     *            values.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setDestinationFunctionAlpha(DestinationFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        destinationFunctionAlpha = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>getDestinationFunction</code> returns the destination function
     * for the blending function.
     * 
     * @return the destination function for the blending function.
     */
    public DestinationFunction getDestinationFunctionRGB() {
        return destinationFunctionRGB;
    }

    /**
     * <code>getDestinationFunction</code> returns the destination function
     * for the blending function.
     * 
     * @return the destination function for the blending function.
     */
    public DestinationFunction getDestinationFunctionAlpha() {
        return destinationFunctionAlpha;
    }

    public void setBlendEquation(BlendEquation blendEquation) {
        setBlendEquationRGB(blendEquation);
        setBlendEquationAlpha(blendEquation);
    }

    public void setBlendEquationRGB(BlendEquation blendEquation) {
        if (blendEquation == null) {
            throw new IllegalArgumentException("blendEquation can not be null.");
        }
        this.blendEquationRGB = blendEquation;
    }

    public void setBlendEquationAlpha(BlendEquation blendEquation) {
        if (blendEquation == null) {
            throw new IllegalArgumentException("blendEquation can not be null.");
        }
        this.blendEquationAlpha = blendEquation;
    }

    public BlendEquation getBlendEquationRGB() {
        return blendEquationRGB;
    }

    public BlendEquation getBlendEquationAlpha() {
        return blendEquationAlpha;
    }
    
    /**
     * @return the color used in constant blending functions. If null and a
     *         *Constant* function is set, (0,0,0,0) is used.
     */
    public ColorRGBA getConstantColor() {
        return constantColor;
    }

    public void setConstantColor(ColorRGBA constantColor) {
        this.constantColor = constantColor;
    }
    
    @Override
    public void update(RenderContext ctx) {
        if( indep != null )
            return;
        // create implementation for this state
        super.update(ctx);
        if( needsRefresh ) {
            ((AlphaBlendState)impl).enabled = this.enabled;
            ((AlphaBlendState)impl).needsRefresh = true;

            ((AlphaBlendState)impl).sourceFunctionAlpha = this.sourceFunctionAlpha;
            ((AlphaBlendState)impl).sourceFunctionRGB = this.sourceFunctionRGB;
            
            ((AlphaBlendState)impl).destinationFunctionAlpha = this.destinationFunctionAlpha;
            ((AlphaBlendState)impl).destinationFunctionRGB = this.destinationFunctionRGB;
            
            ((AlphaBlendState)impl).blendEquationAlpha = this.blendEquationAlpha;
            ((AlphaBlendState)impl).blendEquationRGB = this.blendEquationRGB;
            
            
            needsRefresh = false;
        }
    }

}
