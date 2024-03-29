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
 * <code>ClipState</code> specifies a plane to test for clipping of the nodes. This can be used to
 * take "slices" out of geometric objects. ClipPlane can add an additional (to the normal frustum planes) 
 * six planes to clip against.
 */
public class ClipState extends RenderState {
    public static final int CLIP_PLANE0 = 0;

    public static final int CLIP_PLANE1 = 1;

    public static final int CLIP_PLANE2 = 2;

    public static final int CLIP_PLANE3 = 3;

    public static final int CLIP_PLANE4 = 4;

    public static final int CLIP_PLANE5 = 5;

    public static final int MAX_CLIP_PLANES = 6;

    protected boolean[] enabledClipPlanes = new boolean[MAX_CLIP_PLANES];

    protected double[][] planeEquations = new double[MAX_CLIP_PLANES][4];

    /**
     * <code>getType</code> returns RenderState.RS_CLIP
     * 
     * @return RenderState.RS_CLIP
     * @see RenderState#getType()
     */
    public int getType() {
        return RS_CLIP;
    }

    /**
     * Enables/disables a specific clip plane
     * 
     * @param planeIndex
     *            Plane to enable/disable (CLIP_PLANE0-CLIP_PLANE5)
     * @param enabled
     *            true/false
     */
    public void setEnableClipPlane(int planeIndex, boolean enabled) {
        if (planeIndex < 0 || planeIndex >= MAX_CLIP_PLANES) {
            return;
        }

        enabledClipPlanes[planeIndex] = enabled;
        setNeedsRefresh(true);
    }

    /**
     * Sets plane equation for a specific clip plane
     * 
     * @param planeIndex
     *            Plane to set equation for (CLIP_PLANE0-CLIP_PLANE5)
     * @param clipX
     *            plane x variable
     * @param clipY
     *            plane y variable
     * @param clipZ
     *            plane z variable
     * @param clipW
     *            plane w variable
     */
    public void setClipPlaneEquation(int planeIndex, double clipX,
            double clipY, double clipZ, double clipW) {
        if (planeIndex < 0 || planeIndex >= MAX_CLIP_PLANES) {
            return;
        }

        planeEquations[planeIndex][0] = clipX;
        planeEquations[planeIndex][1] = clipY;
        planeEquations[planeIndex][2] = clipZ;
        planeEquations[planeIndex][3] = clipW;
        setNeedsRefresh(true);
    }

    /**
     * @param index plane to check
     * @return true if given clip plane is enabled
     */
    public boolean getPlaneEnabled(int index) {
        return enabledClipPlanes[index];
    }

    public double getPlaneEq(int plane, int eqIndex) {
        return planeEquations[plane][eqIndex];
    }
    public void setPlaneEq(int plane, int eqIndex, double value) {
        planeEquations[plane][eqIndex] = value;
        setNeedsRefresh(true);
    }
    
    @Override
    public void update( RenderContext ctx ) {
        if( indep != null )
            return;
        // create implementation for this state
        super.update(ctx);
        if( needsRefresh ) {
            ((ClipState)impl).enabled = this.enabled;
            ((ClipState)impl).needsRefresh = true;

            System.arraycopy(enabledClipPlanes, 0, ((ClipState)impl).enabledClipPlanes, 0, MAX_CLIP_PLANES);
            for( int i=0; i<MAX_CLIP_PLANES; i++ ) {
                System.arraycopy(planeEquations[i], 0, ((ClipState)impl).planeEquations[i], 0, MAX_CLIP_PLANES);
            }

            needsRefresh = false;
        }
    }

}