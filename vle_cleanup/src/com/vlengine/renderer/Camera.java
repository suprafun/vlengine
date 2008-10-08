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

package com.vlengine.renderer;

import com.vlengine.bounding.BoundingVolume;
import com.vlengine.math.Plane;

/**
 *
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class Camera {
    /**
     * defines a constant assigned to spatials that are outside of this camera's
     * view frustum.
     */
    public static final int OUTSIDE_FRUSTUM = 0;

    /**
     * defines a constant assigned to spatials that are intersecting one of the
     * six planes that define the view frustum.
     */
    public static final int INTERSECTS_FRUSTUM = 1;

    /**
     * defines a constant assigned to spatials that are completely inside the
     * camera's view frustum.
     */
    public static final int INSIDE_FRUSTUM = 2;

   //planes of the frustum
    /**
     * LEFT_PLANE represents the left plane of the camera frustum.
     */
    public static final int LEFT_PLANE = 0;

    /**
     * RIGHT_PLANE represents the right plane of the camera frustum.
     */
    public static final int RIGHT_PLANE = 1;

    /**
     * BOTTOM_PLANE represents the bottom plane of the camera frustum.
     */
    public static final int BOTTOM_PLANE = 2;

    /**
     * TOP_PLANE represents the top plane of the camera frustum.
     */
    public static final int TOP_PLANE = 3;

    /**
     * FAR_PLANE represents the far plane of the camera frustum.
     */
    public static final int FAR_PLANE = 4;

    /**
     * NEAR_PLANE represents the near plane of the camera frustum.
     */
    public static final int NEAR_PLANE = 5;

    /**
     * FRUSTUM_PLANES represents the number of planes of the camera frustum.
     */
    public static final int FRUSTUM_PLANES = 6;

    /**
     * MAX_WORLD_PLANES holds the maximum planes allowed by the system.
     */
    public static final int MAX_WORLD_PLANES = 32;

    /**
     * Array holding the planes that this camera will check for culling.
     */
    protected Plane[] worldPlane;
    
    /**
     * A mask value set during contains() that allows fast culling of a Node's
     * children.
     */
    protected int planeState;
    
    // the Id of the view (renderpass), multiple camera instances can work
    // in different threads on the same view, they will all have the same Id
    protected int viewId = -1;
    
    /**
     * <code>getPlaneState</code> returns the state of the frustum planes. So
     * checks can be made as to which frustum plane has been examined for
     * culling thus far.
     *
     * @return the current plane state int.
     */
    public int getPlaneState() {
        return planeState;
    }

    /**
     * <code>setPlaneState</code> sets the state to keep track of tested
     * planes for culling.
     *
     * @param planeState the updated state.
     */
    public void setPlaneState( int planeState ) {
        this.planeState = planeState;
    }
    
    /**
     * <code>culled</code> tests a bounding volume against the planes of the
     * camera's frustum. The frustums planes are set such that the normals all
     * face in towards the viewable scene. Therefore, if the bounding volume is
     * on the negative side of the plane is can be culled out. If the object
     * should be culled (i.e. not rendered) true is returned, otherwise, false
     * is returned. If bound is null, false is returned and the object will not
     * be culled.
     *
     * @param bound the bound to check for culling
     * @return true if the bound should be culled, false otherwise.
     */
    public int contains( BoundingVolume bound ) {
        if ( bound == null ) {
            return INTERSECTS_FRUSTUM;
        }

        int mask;
        int rVal = INSIDE_FRUSTUM;

        // extracted from loop, to ensure that it will be not overwritten from another
        // thread in meantime
        int checkPlane = bound.getCheckPlane();
        
        for ( int planeCounter = FRUSTUM_PLANES; planeCounter >= 0; planeCounter-- ) {
            if ( planeCounter == checkPlane ) {
                continue; // we have already checked this plane at first iteration
            }
            int planeId = ( planeCounter == FRUSTUM_PLANES ) ? checkPlane : planeCounter;

            mask = 1 << ( planeId );
            if ( ( planeState & mask ) == 0 ) {
                int side = bound.whichSide( worldPlane[planeId] );

                if ( side == Plane.NEGATIVE_SIDE ) {
                    //object is outside of frustum
                    bound.setCheckPlane( planeId );
                    return OUTSIDE_FRUSTUM;
                }
                else if ( side == Plane.POSITIVE_SIDE ) {
                    //object is visible on *this* plane, so mark this plane
                    //so that we don't check it for sub nodes.
                    planeState |= mask;
                }
                else {
                    rVal = INTERSECTS_FRUSTUM;
                }
            }
        }

        return rVal;
    }
    
    public Camera copy( Camera other  ) {
        other.planeState = this.planeState;
        other.viewId = this.viewId;
        if( other.worldPlane == null )
            other.worldPlane = new Plane[FRUSTUM_PLANES];
        for ( int i = 0; i < FRUSTUM_PLANES; i++ ) {
            other.worldPlane[i] = worldPlane[i];
        }
        return other;
    }
    
    public int getViewId() {
        return viewId;
    }
}
