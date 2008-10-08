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

package com.vlengine.scene.state;

import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderContext;


/**
 * <code>FogState</code> maintains the fog qualities for a node and it's
 * children. The fogging function, color, start, end and density are all set and
 * maintained. Please note that fog does not affect alpha.
 * 
 * @author Mark Powell
 * @author Joshua Slack
 * @author vear (Arpad Vekas) reworked to VL engine
 */
public class FogState extends RenderState {

    public enum DensityFunction {
        /**
         * The fog blending function defined as: (end - z) / (end - start).
         */
        Linear,
        /**
         * The fog blending function defined as: e^-(density*z)
         */
        Exponential,
        /**
         * The fog blending function defined as: e^((-density*z)^2)
         */
        ExponentialSquared;
    }

    public enum Quality {
        /**
         * Each vertex color is altered by the fogging function.
         */
        PerVertex,
        /**
         * Each pixel color is altered by the fogging function.
         */
        PerPixel;
    }

    // fogging attributes.
    protected float start = 0;
    protected float end = 1;
    protected float density = 1.0f;
    protected ColorRGBA color = new ColorRGBA();
    protected DensityFunction densityFunction = DensityFunction.Exponential;
    protected Quality quality = Quality.PerVertex;

    /**
     * Constructor instantiates a new <code>FogState</code> with default fog
     * values.
     */
    public FogState() {
    }

    /**
     * <code>setQuality</code> sets the quality used for the fog attributes.
     * 
     * @param quality
     *            the quality used for the fog application.
     * @throws IllegalArgumentException
     *             if quality is null
     */
    public void setQuality(Quality quality) {
        if (quality == null) {
            throw new IllegalArgumentException("quality can not be null.");
        }
        this.quality = quality;
        setNeedsRefresh(true);
    }

    /**
     * <code>setDensityFunction</code> sets the density function used for the
     * fog blending.
     * 
     * @param function
     *            the function used for the fog density.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setDensityFunction(DensityFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        this.densityFunction = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>setColor</code> sets the color of the fog.
     * 
     * @param color
     *            the color of the fog. This value is COPIED into the state.
     *            Further changes to the object after calling this method will
     *            have no affect on this state.
     */
    public void setColor(ColorRGBA color) {
        this.color.set(color);
        setNeedsRefresh(true);
    }

    /**
     * <code>setDensity</code> sets the density of the fog. This value is
     * clamped to [0, 1].
     * 
     * @param density
     *            the density of the fog.
     */
    public void setDensity(float density) {
        if (density < 0) {
            density = 0;
        }

        if (density > 1) {
            density = 1;
        }
        this.density = density;
        setNeedsRefresh(true);
    }

    /**
     * <code>setEnd</code> sets the end distance, or the distance where fog is
     * at it's thickest.
     * 
     * @param end
     *            the distance where the fog is the thickest.
     */
    public void setEnd(float end) {
        this.end = end;
        setNeedsRefresh(true);
    }

    /**
     * <code>setStart</code> sets the start distance, or where fog begins to
     * be applied.
     * 
     * @param start
     *            the start distance of the fog.
     */
    public void setStart(float start) {
        this.start = start;
        setNeedsRefresh(true);
    }

    /**
     * <code>getType</code> returns the render state type of the fog state.
     * (RS_FOG).
     * 
     * @see com.jme.scene.state.RenderState#getType()
     */
    public int getType() {
        return RS_FOG;
    }

    public Quality getQuality() {
        return quality;
    }

    public ColorRGBA getColor() {
        return color;
    }

    public float getDensity() {
        return density;
    }

    public DensityFunction getDensityFunction() {
        return densityFunction;
    }

    public float getEnd() {
        return end;
    }

    public float getStart() {
        return start;
    }
    
    @Override
    public void update(RenderContext ctx) {
        if( indep != null )
            return;
        // create implementation for this state
        super.update(ctx);
        if( needsRefresh ) {
            ((FogState)impl).enabled = this.enabled;
            ((FogState)impl).needsRefresh = true;
            
            ((FogState)impl).start = this.start;
            ((FogState)impl).end = this.end;
            ((FogState)impl).density = this.density;
            ((FogState)impl).color = this.color;
            ((FogState)impl).densityFunction = this.densityFunction;
            ((FogState)impl).quality = this.quality;
            
            
            needsRefresh = false;
        }
    }
}
