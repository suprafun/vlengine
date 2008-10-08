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
import com.vlengine.system.VleException;


/**
 * <code>SpotLight</code> defines a light that has a location in space and
 * emits light within a cone. This cone is defined by an angle and exponent.
 * Typically this light's values are attenuated based on the
 * distance of the point light and the object it illuminates.
 * @author Mark Powell
 * @version $Id: SpotLight.java,v 1.11 2007/09/21 15:45:30 nca Exp $
 */
public class SpotLight extends PointLight {
    
	//attributes
    private float angle;
    private float exponent=50;
    private Vector3f direction;

    /**
     * Constructor instantiates a new <code>SpotLight</code> object. The
     * initial position of the light is (0,0,0) with angle 0, and colors white.
     *
     */
    public SpotLight() {
        super();
        direction = new Vector3f();
    }
    /**
     * <code>getDirection</code> returns the direction the spot light pointing.
     * @return the direction the spot light is pointing.
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * <code>setDirection</code> sets the direction the spot light is pointing.
     * @param direction the direction the spot light is pointing.
     */
    public void setDirection(Vector3f direction) {
        this.direction = direction;
    }

    /**
     * <code>getAngle</code> returns the angle of the spot light.
     * 
     * @see #setAngle(float) for more info
     * @return the angle (in degrees)
     */
    public float getAngle() {
        return angle;
    }

    /**
     * <code>setAngle</code> sets the angle of focus of the spot light
     * measured from the direction vector. Think of this as the angle of a cone.
     * Therefore, if you specify 10 degrees, you will get a 20 degree cone (10
     * degrees off either side of the direction vector.) 180 degrees means
     * radiate in all directions.
     * 
     * @param angle
     *            the angle (in degrees) which must be between 0 and 90
     *            (inclusive) or the special case 180.
     */
    public void setAngle(float angle) {
        if (angle < 0 || (angle > 90 && angle != 180))
            throw new VleException("invalid angle.  Angle must be between 0 and 90, or 180");
        this.angle = angle;
    }

    /**
     * <code>getExponent</code> gets the spot exponent of this light.
     * 
     * @see #setExponent(float) for more info
     * @return the spot exponent of this light.
     */
    public float getExponent() {
        return exponent;
    }

    /**
     * <code>setExponent</code> sets the spot exponent of this light. This
     * value represents how focused the light beam is.
     * 
     * @param exponent
     *            the spot exponent of this light. Should be between 0-128
     */
    public void setExponent(float exponent) {
        this.exponent = exponent;
    }

    /**
     * <code>getType</code> returns the type of this light (LT_SPOT).
     * @see com.jme.light.Light#getType()
     */
    @Override
    public int getType() {
        return LT_SPOT;
    }


}
