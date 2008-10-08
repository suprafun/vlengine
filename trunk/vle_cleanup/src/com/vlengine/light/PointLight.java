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

package com.vlengine.light;

import com.vlengine.math.Vector3f;

/**
 * <code>PointLight</code> defines a light that has a location in space and
 * emits light in all directions evenly. This would be something similar to a
 * light bulb. Typically this light's values are attenuated based on the
 * distance of the point light and the object it illuminates.
 * @author Mark Powell
 * @version $Id: PointLight.java,v 1.7 2007/09/21 15:45:30 nca Exp $
 */
public class PointLight extends Light {

	//Position of the light.
    private Vector3f location;

    /**
     * Constructor instantiates a new <code>PointLight</code> object. The
     * initial position of the light is (0,0,0) and it's colors are white.
     *
     */
    public PointLight() {
        super();
        location = new Vector3f();
    }

    /**
     * <code>getLocation</code> returns the position of this light.
     * @return the position of the light.
     */
    public Vector3f getLocation() {
        return location;
    }

    /**
     * <code>setLocation</code> sets the position of the light.
     * @param location the position of the light.
     */
    public void setLocation(Vector3f location) {
        this.location = location;
    }

    /**
     * <code>getType</code> returns the type of this light (LT_POINT).
     * @see com.jme.light.Light#getType()
     */
    public int getType() {
        return LT_POINT;
    }
}
