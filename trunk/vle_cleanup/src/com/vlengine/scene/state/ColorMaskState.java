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



/**
 * <code>ColorMaskState</code>
 * 
 * @author Mike Talbot
 * @author Joshua Slack
 * @version $Id: ColorMaskState.java,v 1.5 2006/11/16 17:02:15 nca Exp $
 */
public class ColorMaskState extends RenderState {

    protected boolean blue = true;
    protected boolean green = true;
    protected boolean red = true;
    protected boolean alpha = true;

    /*
     * (non-Javadoc) <code>getType</code>
     * 
     * @see com.jme.scene.state.RenderState#getType()
     */
    public int getType() {
        return RenderState.RS_COLORMASK_STATE;
    }

    public void setAll(boolean on) {
        blue = on;
        green = on;
        red = on;
        alpha = on;
        setNeedsRefresh(true);
    }

    /**
     * @return Returns the alpha.
     */
    public boolean getAlpha() {
        return alpha;
    }

    /**
     * @param alpha
     *            The alpha to set.
     */
    public void setAlpha(boolean alpha) {
        this.alpha = alpha;
        setNeedsRefresh(true);
    }

    /**
     * @return Returns the blue.
     */
    public boolean getBlue() {
        return blue;
    }

    /**
     * @param blue
     *            The blue to set.
     */
    public void setBlue(boolean blue) {
        this.blue = blue;
        setNeedsRefresh(true);
    }

    /**
     * @return Returns the green.
     */
    public boolean getGreen() {
        return green;
    }

    /**
     * @param green
     *            The green to set.
     */
    public void setGreen(boolean green) {
        this.green = green;
        setNeedsRefresh(true);
    }

    /**
     * @return Returns the red.
     */
    public boolean getRed() {
        return red;
    }

    /**
     * @param red
     *            The red to set.
     */
    public void setRed(boolean red) {
        this.red = red;
        setNeedsRefresh(true);
    }
    
    @Override
    public void update(RenderContext ctx) {
        if( indep != null )
            return;
        super.update(ctx);
        if( needsRefresh ) {
            ((ColorMaskState)impl).enabled = this.enabled;
            ((ColorMaskState)impl).needsRefresh = true;

            ((ColorMaskState)impl).red = this.red;
            ((ColorMaskState)impl).green = this.green;
            ((ColorMaskState)impl).blue = this.blue;
            ((ColorMaskState)impl).alpha = this.alpha;
            needsRefresh = false;
        }
    }
}
