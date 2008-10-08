/*
 * Copyright (c) 2008 VL Engine
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

/**
 *
 * @author vear (Arpad Vekas)
 */
public class LineState extends RenderState {

    protected float lineWidth = 1.0f;
    protected short stipplePattern = (short)0xFFFF;
    protected int stippleFactor = 1;
    protected boolean antialiased = false;

    public LineState() {
        
    }
    
    @Override
    public int getType() {
        return RS_LINE;
    }

/**
     * @return true if points are to be drawn antialiased
     */
    public boolean isAntialiased() {
        return antialiased;
    }
    
    /**
     * Sets whether the point should be antialiased. May decrease performance. If
     * you want to enabled antialiasing, you should also use an alphastate with
     * a source of SB_SRC_ALPHA and a destination of DB_ONE_MINUS_SRC_ALPHA or
     * DB_ONE.
     * 
     * @param antiAliased
     *            true if the line should be antialiased.
     */
    public void setAntialiased(boolean antialiased) {
        this.antialiased = antialiased;
    }

    /**
     * @return the width of this line.
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Sets the width of the line when drawn. Non anti-aliased line widths are
     * rounded to the nearest whole number by opengl.
     * 
     * @param lineWidth
     *            The lineWidth to set.
     */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * @return the set stipplePattern. 0xFFFF means no stipple.
     */
    public short getStipplePattern() {
        return stipplePattern;
    }

    /**
     * The stipple or pattern to use when drawing this line. 0xFFFF is a solid
     * line.
     * 
     * @param stipplePattern
     *            a 16bit short whose bits describe the pattern to use when
     *            drawing this line
     */
    public void setStipplePattern(short stipplePattern) {
        this.stipplePattern = stipplePattern;
    }
    
    /**
     * @return the set stippleFactor.
     */
    public int getStippleFactor() {
        return stippleFactor;
    }

    /**
     * @param stippleFactor
     *            magnification factor to apply to the stipple pattern.
     */
    public void setStippleFactor(int stippleFactor) {
        this.stippleFactor = stippleFactor;
    }

    @Override
    public void update(RenderContext ctx) {
        if( indep != null )
            return;
        // create implementation for this state
        super.update(ctx);
        if( needsRefresh ) {
            ((LineState)impl).enabled = this.enabled;
            ((LineState)impl).needsRefresh = true;

            ((LineState)impl).lineWidth = this.lineWidth;
            ((LineState)impl).stipplePattern = this.stipplePattern;
            ((LineState)impl).stippleFactor = this.stippleFactor;
            ((LineState)impl).antialiased = this.antialiased;
            
            needsRefresh = false;
        }
    }
}
