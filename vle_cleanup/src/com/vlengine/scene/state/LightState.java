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

import com.vlengine.light.Light;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.SceneElement;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.BufferUtils;
import java.nio.FloatBuffer;
import java.util.Stack;

public class LightState extends RenderState {
    
    /**
     * Debug flag for turning off all lighting.
     */
    public static boolean LIGHTS_ENABLED = true;
    
    /**
     * defines the maximum number of lights that are allowed to be maintained at
     * one time.
     */
    public static final int MAX_LIGHTS_ALLOWED = 8;

    /** Ignore lights. */
    public static final int OFF = 0;

    /**
     * Combine light states starting from the root node and working towards the
     * given SceneElement. Ignore disabled states. Stop combining when lights ==
     * MAX_LIGHTS_ALLOWED
     */
    public static final int COMBINE_FIRST = 1;

    /**
     * Combine light states starting from the given Spatial and working towards
     * the root. Ignore disabled states. Stop combining when lights ==
     * MAX_LIGHTS_ALLOWED
     */
    public static final int COMBINE_CLOSEST = 2;

    /**
     * Similar to COMBINE_CLOSEST, but if a disabled state is encountered, it
     * will stop combining at that point. Stop combining when lights ==
     * MAX_LIGHTS_ALLOWED
     */
    public static final int COMBINE_RECENT_ENABLED = 3;

    /** Inherit mode from parent. */
    public static final int INHERIT = 4;

    /** Do not combine light states, just use the most recent one. */
    public static final int REPLACE = 5;

    /**
     * When applied to lightMask, implies ambient light should be set to 0 for
     * this lightstate
     */
    public static final int MASK_AMBIENT = 1;

    /**
     * When applied to lightMask, implies diffuse light should be set to 0 for
     * this lightstate
     */
    public static final int MASK_DIFFUSE = 2;

    /**
     * When applied to lightMask, implies specular light should be set to 0 for
     * this lightstate
     */
    public static final int MASK_SPECULAR = 4;

    /**
     * When applied to lightMask, implies global ambient light should be set to
     * 0 for this lightstate
     */
    public static final int MASK_GLOBALAMBIENT = 8;

    // holds the lights
    private FastList<Light> lightList;

    // mask value - default is no masking
    protected int lightMask = 0;

    // mask value stored by pushLightMask, retrieved by popLightMask
    protected int backLightMask = 0;

    /** When true, both sides of the model will be lighted. */
    protected boolean twoSidedOn = true;

    protected float[] globalAmbient = { 0.0f, 0.0f, 0.0f, 1.0f };
    //XXX move to record
    protected static FloatBuffer zeroBuffer;

    /**
     * When true, the eye position (as opposed to just the view direction) will
     * be taken into account when computing specular reflections.
     */
    protected boolean localViewerOn;

    /**
     * When true, specular highlights will be computed separately and added to
     * fragments after texturing.
     */
    protected boolean separateSpecularOn;

    /**
     * Constructor instantiates a new <code>LightState</code> object.
     * Initially there are no lights set.
     */
    public LightState() {
        lightList = new FastList<Light>();
        if (zeroBuffer == null) {
            zeroBuffer = BufferUtils.createFloatBuffer(4);
            zeroBuffer.put(0).put(0).put(0).put(1);
            zeroBuffer.rewind();
        }
    }

    /**
     * <code>getType</code> returns the type of render state this is.
     * (RS_LIGHT).
     * 
     * @see com.jme.scene.state.RenderState#getType()
     */
    public int getType() {
        return RS_LIGHT;
    }

    /**
     * 
     * <code>attach</code> places a light in the queue to be processed. If
     * there are already eight lights placed in the queue, the light is ignored
     * and false is returned. Otherwise, true is returned to indicate success.
     * 
     * @param light
     *            the light to add to the queue.
     * @return true if the light was added successfully, false if there are
     *         already eight lights in the queue.
     */
    public boolean attach(Light light) {
        if (lightList.size() < MAX_LIGHTS_ALLOWED) {
            if (!lightList.contains(light)) {
                lightList.add(light);
                setNeedsRefresh(true);
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * <code>detach</code> removes a light from the queue for processing.
     * 
     * @param light
     *            the light to be removed.
     */
    public void detach(Light light) {
        lightList.remove(light);
        setNeedsRefresh(true);
    }

    /**
     * 
     * <code>detachAll</code> clears the queue of all lights to be processed.
     * 
     */
    public void detachAll() {
        lightList.clear();
        setNeedsRefresh(true);
    }

    /**
     * 
     * <code>get</code> retrieves a particular light defined by an index. If
     * there exists no light at a particular index, null is returned.
     * 
     * @param i
     *            the index to retrieve the light from the queue.
     * @return the light at the given index, null if no light exists at this
     *         index.
     */
    public Light get(int i) {
        return lightList.get(i);
    }

    /**
     * 
     * <code>getQuantity</code> returns the number of lights currently in the
     * queue.
     * 
     * @return the number of lights currently in the queue.
     */
    public int getQuantity() {
        return lightList.size();
    }

    /**
     * Sets if two sided lighting should be enabled for this LightState.
     * 
     * @param twoSidedOn
     *            If true, two sided lighting is enabled.
     */
    public void setTwoSidedLighting(boolean twoSidedOn) {
        this.twoSidedOn = twoSidedOn;
        setNeedsRefresh(true);
    }

    /**
     * Returns the current state of two sided lighting for this LightState. By
     * default, it is off.
     * 
     * @return True if two sided lighting is enabled.
     */
    public boolean getTwoSidedLighting() {
        return this.twoSidedOn;
    }

    /**
     * Sets if local viewer mode should be enabled for this LightState.
     * 
     * @param localViewerOn
     *            If true, local viewer mode is enabled.
     */
    public void setLocalViewer(boolean localViewerOn) {
        this.localViewerOn = localViewerOn;
        setNeedsRefresh(true);
    }

    /**
     * Returns the current state of local viewer mode for this LightState. By
     * default, it is off.
     * 
     * @return True if local viewer mode is enabled.
     */
    public boolean getLocalViewer() {
        return this.localViewerOn;
    }

    /**
     * Sets if separate specular mode should be enabled for this LightState.
     * 
     * @param separateSpecularOn
     *            If true, separate specular mode is enabled.
     */
    public void setSeparateSpecular(boolean separateSpecularOn) {
        this.separateSpecularOn = separateSpecularOn;
        setNeedsRefresh(true);
    }

    /**
     * Returns the current state of separate specular mode for this LightState.
     * By default, it is off.
     * 
     * @return True if separate specular mode is enabled.
     */
    public boolean getSeparateSpecular() {
        return this.separateSpecularOn;
    }

    public void setGlobalAmbient(ColorRGBA color) {
        globalAmbient[0] = color.r;
        globalAmbient[1] = color.g;
        globalAmbient[2] = color.b;
        globalAmbient[3] = color.a;
        setNeedsRefresh(true);
    }

    public ColorRGBA getGlobalAmbient() {
        return new ColorRGBA(globalAmbient[0], globalAmbient[1],
                globalAmbient[2], globalAmbient[3]);
    }

    /**
     * @return Returns the lightMask - default is 0 or not masked.
     */
    public int getLightMask() {
        return lightMask;
    }

    /**
     * <code>setLightMask</code> sets what attributes of this lightstate to
     * apply as an int comprised of bitwise or'ed values.
     * 
     * @param lightMask
     *            The lightMask to set.
     */
    public void setLightMask(int lightMask) {
        this.lightMask = lightMask;
        setNeedsRefresh(true);
    }

    /**
     * Saves the light mask to a back store. That backstore is recalled with
     * popLightMask. Despite the name, this is not a stack and additional pushes
     * will simply overwrite the backstored value.
     */
    public void pushLightMask() {
        backLightMask = lightMask;
    }

    /**
     * Recalls the light mask from a back store or 0 if none was pushed.
     * 
     * @see com.jme.scene.state.LightState#pushLightMask()
     */
    public void popLightMask() {
        lightMask = backLightMask;
    }

    @Override
     public void update(RenderContext ctx) {
        if( indep != null )
            return;
        super.update(ctx);
        if( needsRefresh ) {
            ((LightState)impl).enabled = this.enabled;
            ((LightState)impl).needsRefresh = true;

            ((LightState)impl).lightList.clear();
            ((LightState)impl).lightList.addAll(lightList);
            
            ((LightState)impl).twoSidedOn = this.twoSidedOn;
            ((LightState)impl).localViewerOn = this.localViewerOn;
            ((LightState)impl).separateSpecularOn = this.separateSpecularOn;
            needsRefresh = false;
        }
    }
     
    @Deprecated
    public RenderState extract(Stack stack, SceneElement spat) {
        // TODO: use LightManagement
        
        int mode = spat.getLightCombineMode();
        if (mode == REPLACE || (mode != OFF && stack.size() == 1)) // todo: use
            // dummy
            // state if
            // off?
            return (LightState) stack.peek();

        // accumulate the lights in the stack into a single LightState object
        LightState newLState = new LightState();
        Object states[] = stack.toArray();
        boolean foundEnabled = false;
        switch (mode) {
        case COMBINE_CLOSEST:
        case COMBINE_RECENT_ENABLED:
            for (int iIndex = states.length - 1; iIndex >= 0; iIndex--) {
                LightState pkLState = (LightState) states[iIndex];
                if (!pkLState.isEnabled()) {
                    if (mode == COMBINE_RECENT_ENABLED)
                        break;

                    continue;
                }

                foundEnabled = true;
                if (pkLState.twoSidedOn)
                    newLState.setTwoSidedLighting(true);
                if (pkLState.localViewerOn)
                    newLState.setLocalViewer(true);
                if (pkLState.separateSpecularOn)
                    newLState.setSeparateSpecular(true);
                for (int i = 0, maxL = pkLState.getQuantity(); i < maxL; i++) {
                    Light pkLight = pkLState.get(i);
                    if (pkLight != null) {
                        newLState.attach(pkLight);
                    }
                }
            }
            break;
        case COMBINE_FIRST:
            for (int iIndex = 0, max = states.length; iIndex < max; iIndex++) {
                LightState pkLState = (LightState) states[iIndex];
                if (!pkLState.isEnabled())
                    continue;

                foundEnabled = true;
                if (pkLState.twoSidedOn)
                    newLState.setTwoSidedLighting(true);
                if (pkLState.localViewerOn)
                    newLState.setLocalViewer(true);
                if (pkLState.separateSpecularOn)
                    newLState.setSeparateSpecular(true);
                for (int i = 0, maxL = pkLState.getQuantity(); i < maxL; i++) {
                    Light pkLight = pkLState.get(i);
                    if (pkLight != null) {
                        newLState.attach(pkLight);
                    }
                }
            }
            break;
        case OFF:
            break;
        }
        newLState.setEnabled(foundEnabled);
        return newLState;
    }
}
