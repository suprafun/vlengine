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

import com.vlengine.light.DirectionalLight;
import com.vlengine.light.Light;
import com.vlengine.light.PointLight;
import com.vlengine.light.SpotLight;
import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.state.LightState;
import com.vlengine.util.geom.BufferUtils;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

public class LWJGLLightState extends LightState {

    // tmp data
        private static final FloatBuffer lightBuffer = BufferUtils.createColorBuffer(1);
        
	/**
	 * Constructor instantiates a new <code>LWJGLLightState</code>.
	 */
	public LWJGLLightState() {
		super();
	}

    /**
     * <code>set</code> iterates over the light queue and processes each
     * individual light.
     * 
     * @see com.jme.scene.state.RenderState#apply()
     */
    @Override
    public void apply(RenderContext context) {
        needsRefresh = false;
        if(indep==null)
            context.currentStates[getType()] = this;
        if (isEnabled() && LIGHTS_ENABLED) {
            setLightEnabled(true);
            setTwoSided(twoSidedOn);
            setLocalViewerPriv(localViewerOn);
            if (GLContext.getCapabilities().OpenGL12) {
                setSpecularControl(separateSpecularOn);
            }

            for (int i = 0, max = getQuantity(); i < max; i++) {
                Light light = get(i);
                if (light == null || !light.isEnabled()) {
                    setSingleLightEnabled(false, i);
                } else {
                    setLight(i, light);
                }
            }

// disable lights at and above the max count in this state
            for (int i = getQuantity(); i < MAX_LIGHTS_ALLOWED; i++) {
                setSingleLightEnabled(false, i);
            }

            if ((lightMask & MASK_GLOBALAMBIENT) == 0) {
                    setModelAmbient(globalAmbient[0], globalAmbient[1], globalAmbient[2], globalAmbient[3]);
            } else {
                    setDefaultModelAmbient();
            }
        } else {
                setLightEnabled(false);
        }        
    }

    private void setLight(int index, Light light) {
        setSingleLightEnabled(true, index);
        if ((lightMask & MASK_AMBIENT) == 0
                        && (light.getLightMask() & MASK_AMBIENT) == 0) {
            setAmbient(index, light.getAmbient().r,
                            light.getAmbient().g, light.getAmbient().b, light
                                            .getAmbient().a);
        } else {
                setDefaultAmbient(index);
        }

        if ((lightMask & MASK_DIFFUSE) == 0
                        && (light.getLightMask() & MASK_DIFFUSE) == 0) {
            setDiffuse(index, light.getDiffuse().r,
                            light.getDiffuse().g, light.getDiffuse().b, light
                                            .getDiffuse().a);
        } else {
                setDefaultDiffuse(index);
        }

        if ((lightMask & MASK_SPECULAR) == 0
                        && (light.getLightMask() & MASK_SPECULAR) == 0) {

                setSpecular(index, light.getSpecular().r, light
                                .getSpecular().g, light.getSpecular().b, light
                                .getSpecular().a);
        } else {
                setDefaultSpecular(index);
        }

        if (light.isAttenuate()) {
            setAttenuate(true, index, light);
        } else {
            setAttenuate(false, index, light);
        }

        switch (light.getType()) {
            case Light.LT_DIRECTIONAL: {
                DirectionalLight pkDL = (DirectionalLight) light;

                setPosition(index, -pkDL.getDirection().x, -pkDL
                        .getDirection().y, -pkDL.getDirection().z, 0);
                break;
            }
            case Light.LT_POINT:
            case Light.LT_SPOT: {
                PointLight pointLight = (PointLight) light;
                setPosition(index, pointLight.getLocation().x,
                        pointLight.getLocation().y, pointLight.getLocation().z,
                        1);
                break;
            }
        }
        if (light.getType() == Light.LT_SPOT) {
                SpotLight spot = (SpotLight) light;
                setSpotCutoff(index, spot.getAngle());
                setSpotDirection(index, spot.getDirection().x, spot
                                .getDirection().y, spot.getDirection().z, 0);
                setSpotExponent(index, spot.getExponent());
        } else {
    // set the cutoff to 180, which causes the other spot params to be ignored.
                setSpotCutoff(index, 180);
        }
    }

    private void setSingleLightEnabled(boolean enable, int index) {
        if (enable) {
            GL11.glEnable(GL11.GL_LIGHT0 + index);
        } else {
            GL11.glDisable(GL11.GL_LIGHT0 + index);
        }
    }

    private void setLightEnabled(boolean enable) {
        if (enable) {
            GL11.glEnable(GL11.GL_LIGHTING);
        } else {
            GL11.glDisable(GL11.GL_LIGHTING);
        }
    }

    private void setTwoSided(boolean twoSided) {
        if (twoSided) {
            GL11.glLightModeli(GL11.GL_LIGHT_MODEL_TWO_SIDE, GL11.GL_TRUE);
        } else {
            GL11.glLightModeli(GL11.GL_LIGHT_MODEL_TWO_SIDE, GL11.GL_FALSE);
        }
    }

    private void setLocalViewerPriv(boolean localViewer) {
        if (localViewer) {
            GL11.glLightModeli(GL11.GL_LIGHT_MODEL_LOCAL_VIEWER,
                            GL11.GL_TRUE);
        } else {
            GL11.glLightModeli(GL11.GL_LIGHT_MODEL_LOCAL_VIEWER,
                            GL11.GL_FALSE);
        }
    }

    private void setSpecularControl(boolean separateSpecularOn) {
        if (separateSpecularOn) {
            GL11.glLightModeli(GL12.GL_LIGHT_MODEL_COLOR_CONTROL,
                            GL12.GL_SEPARATE_SPECULAR_COLOR);
        } else {
            GL11.glLightModeli(GL12.GL_LIGHT_MODEL_COLOR_CONTROL,
                            GL12.GL_SINGLE_COLOR);
        }
    }

    private void setModelAmbient(float red, float green, float blue, float alpha) {
        lightBuffer.clear();
        lightBuffer.put(red);
        lightBuffer.put(green);
        lightBuffer.put(blue);
        lightBuffer.put(alpha);
        lightBuffer.flip();
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, lightBuffer);
    }

    private void setDefaultModelAmbient() {
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, zeroBuffer);
    }

    private void setAmbient(int index, float red, float green, float blue, float alpha) {
            lightBuffer.clear();
            lightBuffer.put(red);
            lightBuffer.put(green);
            lightBuffer.put(blue);
            lightBuffer.put(alpha);
            lightBuffer.flip();
            GL11.glLight(GL11.GL_LIGHT0+index, GL11.GL_AMBIENT, lightBuffer);
    }

    private void setDefaultAmbient(int index) {
        GL11.glLight(GL11.GL_LIGHT0+index, GL11.GL_AMBIENT, zeroBuffer);
    }

    private void setDiffuse(int index, float red, float green, float blue, float alpha) {
            lightBuffer.clear();
            lightBuffer.put(red);
            lightBuffer.put(green);
            lightBuffer.put(blue);
            lightBuffer.put(alpha);
            lightBuffer.flip();
            GL11.glLight(GL11.GL_LIGHT0+index, GL11.GL_DIFFUSE, lightBuffer);
    }

    private void setDefaultDiffuse(int index) {
        GL11.glLight(GL11.GL_LIGHT0+index, GL11.GL_DIFFUSE, zeroBuffer);
    }

    private void setSpecular(int index, float red, float green, float blue, float alpha) {
        lightBuffer.clear();
        lightBuffer.put(red);
        lightBuffer.put(green);
        lightBuffer.put(blue);
        lightBuffer.put(alpha);
        lightBuffer.flip();
        GL11.glLight(GL11.GL_LIGHT0+index, GL11.GL_SPECULAR, lightBuffer);
    }

    private void setDefaultSpecular(int index) {
        GL11.glLight(GL11.GL_LIGHT0+index, GL11.GL_SPECULAR, zeroBuffer);
    }

    private void setPosition(int index, float positionX, float positionY, float positionZ, float value) {
        // From OpenGL Docs:
        // The light position is transformed by the contents of the current top
        // of the ModelView matrix stack when you specify the light position
        // with a call to glLightfv(GL_LIGHT_POSITION,). If you later change
        // the ModelView matrix, such as when the view changes for the next
        // frame, the light position isn't automatically retransformed by the
        // new contents of the ModelView matrix. If you want to update the
        // lights position, you must again specify the light position with a
        // call to glLightfv(GL_LIGHT_POSITION,�).
        lightBuffer.clear();
        lightBuffer.put(positionX);
        lightBuffer.put(positionY);
        lightBuffer.put(positionZ);
        lightBuffer.put(value);
        lightBuffer.flip();
        GL11.glLight(GL11.GL_LIGHT0+index, GL11.GL_POSITION, lightBuffer);
    }

    private void setSpotDirection(int index, float directionX, float directionY, float directionZ, float value) {
            // From OpenGL Docs:
            // The light position is transformed by the contents of the current top
            // of the ModelView matrix stack when you specify the light position
            // with a call to glLightfv(GL_LIGHT_POSITION,�). If you later change
            // the ModelView matrix, such as when the view changes for the next
            // frame, the light position isn't automatically retransformed by the
            // new contents of the ModelView matrix. If you want to update the
            // light�s position, you must again specify the light position with a
            // call to glLightfv(GL_LIGHT_POSITION,�).
        lightBuffer.clear();
        lightBuffer.put(directionX);
        lightBuffer.put(directionY);
        lightBuffer.put(directionZ);
        lightBuffer.put(value);
        lightBuffer.flip();
        GL11.glLight(GL11.GL_LIGHT0+index, GL11.GL_SPOT_DIRECTION, lightBuffer);
    }

    private void setConstant(int index, float constant) {
        GL11.glLightf(GL11.GL_LIGHT0+index, GL11.GL_CONSTANT_ATTENUATION, constant);
    }

    private void setLinear(int index, float linear) {
        GL11.glLightf(GL11.GL_LIGHT0+index, GL11.GL_LINEAR_ATTENUATION, linear);
    }

    private void setQuadratic(int index, float quad) {
        GL11.glLightf(GL11.GL_LIGHT0+index, GL11.GL_QUADRATIC_ATTENUATION, quad);
    }

    private void setAttenuate(boolean attenuate, int index, Light light) {
        if (attenuate) {
            setConstant(index, light.getConstant());
            setLinear(index, light.getLinear());
            setQuadratic(index, light.getQuadratic());
        } else {
            setConstant(index, 1);
            setLinear(index, 0);
            setQuadratic(index, 0);
        }
    }

	private void setSpotExponent(int index, float exponent) {
            GL11.glLightf(GL11.GL_LIGHT0+index, GL11.GL_SPOT_EXPONENT, exponent);
	}

	private void setSpotCutoff(int index, float cutoff) {
            GL11.glLightf(GL11.GL_LIGHT0+index, GL11.GL_SPOT_CUTOFF, cutoff);
	}
        
    // do nothing for implementation
    @Override
    public void update(RenderContext ctx) {
        
    }
    
}