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

import com.vlengine.bounding.BoundingBox;
import com.vlengine.bounding.BoundingSphere;
import com.vlengine.bounding.BoundingVolume;
import com.vlengine.math.FastMath;
import com.vlengine.math.Plane;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.batch.LightBatch;
import com.vlengine.thread.LocalContext;

/**
 * 
 * @author Badmi
 * @author Mark Powell (cleaning, savable)
 * @author vear (Arpad Vekas) - reworked to VL engine (was LightManagement)
 */
public class LightSorter {

    private final Plane tmpPlane = new Plane();
    private final Vector3f tmpVec = new Vector3f();
    
    public float getValueFor(LightBatch l, Renderable r) {
        // if light has bound, and it does not intersect with given bound, return 0
        // this is for local lights
        BoundingVolume bound = r.getWorldBound();
        BoundingVolume lb = l.getWorldBound();
        // if the light has no bound, then its top priority
        if(lb==null) {
            return 1.0f;
        }
        if(lb!=null && !lb.intersects(bound))
            return 0;
        Light li = l.getLight();
        if (!li.isEnabled()) {
            return 0;
        }
        int frameId = LocalContext.getContext().scene.getFrameId();
        Quaternion dir = l.getWorldTransForm(frameId).getRotation();
        tmpVec.set(Vector3f.UNIT_MINNUS_Z);
        dir.multLocal(tmpVec);
        
        if (li.getType() == Light.LT_DIRECTIONAL
                || li.getType() == Light.LT_SPOT) {
            tmpPlane.setNormal(tmpVec);
            tmpPlane.setConstant(tmpVec.dot(l.getWorldTransForm(frameId).getTranslation()));
            if(bound!=null) {
                if (bound.whichSide(tmpPlane) == Plane.NEGATIVE_SIDE)
                    return 0;
            } else {
                return 0;
            }
        }
        // TODO: maybe relative ratio of how much inside or outside of lights bounds is the closest
        // intersection of bounds
        tmpVec.set(l.getWorldTransForm(frameId).getTranslation()).subtractLocal(bound.getCenter());
        return 1f/tmpVec.length();
    }
    

    protected static float max(ColorRGBA a) {
        return Math.max(Math.max(a.r, a.g), a.b);
    }

    protected static float getColorValue(Light l) {
        return Math.max(Math.max(max(l.getAmbient()), max(l.getDiffuse())),
                max(l.getSpecular()));
    }
    
    protected static float getColorPower(Light l) {
        float mr = l.getAmbient().r + l.getDiffuse().r + l.getSpecular().r;
        float mg = l.getAmbient().g + l.getDiffuse().g + l.getSpecular().g;
        float mb = l.getAmbient().b + l.getDiffuse().b + l.getSpecular().b;
        return 0.299f * mr + 0.587f * mg + 0.114f * mb;
    }
    
    float getValueFor(PointLight l, BoundingVolume val) {
        if(val == null) {
            return 0;
        }
        if (l.isAttenuate() && val != null) {
            float dist = val.distanceTo(l.getLocation());

            float color = getColorValue(l);
            float amlat = l.getConstant() + l.getLinear() * dist
                        + l.getQuadratic() * dist * dist;

            return color / amlat;
        }

        return getColorValue(l);        
    }

    float getValueFor(SpotLight l, BoundingVolume val) {
        if(val == null) {
            return 0;
        }
        tmpPlane.setNormal(l.getDirection());
        tmpPlane.setConstant(l.getDirection().dot(l.getLocation()));
        if (val.whichSide(tmpPlane) != Plane.NEGATIVE_SIDE)
                return getValueFor((PointLight) l, val);

        return 0;
    }
    
    /**
     * Calculates the distance, where the attenuated light 
     * has no more effect, it is used in calculating the lights
     * bounds.
     * @param l
     * @return
     */
    public static float getLightDistance(Light l) {
        // the constant under which light is no more visible
        float epsilon = 0.01f;
        float colorValue = getColorPower(l);
        float constant = l.getConstant();
        float linear = l.getLinear();
        float quadratic = l.getQuadratic();
        float dist = 0;
        if(quadratic!=0) {
            float sq = FastMath.sqrt(linear*linear -4*quadratic*constant )/2*quadratic;
            float att = -linear + sq;
            if(att > 0) {
                dist = colorValue / att;
            } else {
                att = -linear - sq;
                if(att > 0) {
                    dist = colorValue / att;
                }
            }
            return dist;
        } else if(linear != 0) {
            return ((colorValue/epsilon)-constant)/linear;
        }
        return Float.NaN;
    }

    /**
     * Generates a bound of the light. Objects inside the bound are possibly affected by the
     * light, objects outside are not.
     * 
     * @param l
     * @return
     */
    public static BoundingVolume createLightBound(Light l) {
        float lightDist = getLightDistance(l);
        // if no attenuation, then no bound, this is global light
        if(Float.isNaN(lightDist))
            return null;
        // we dont use position, the position is dictated by the
        // LightNode, so the bound is transfromed to world
        /*
        if(l instanceof PointLight 
                || l instanceof SpotLight) {
            // bound is a boundingsphere for a point light
            // this would be a cone, but we use a spere anyway
            BoundingSphere bs = new BoundingSphere();
            bs.setCenter(Vector3f.ZERO.clone());
            bs.setRadius(lightDist);
            return bs;
        }
        if(l instanceof DirectionalLight) {
         */
            // we use a box
            BoundingBox bb = new BoundingBox();
            bb.setCenter(Vector3f.ZERO.clone());
            bb.xExtent = bb.yExtent = bb.zExtent = lightDist;
        //}
        return bb;
    }
}
