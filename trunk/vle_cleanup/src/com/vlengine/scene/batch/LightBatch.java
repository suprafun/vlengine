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

package com.vlengine.scene.batch;

import com.vlengine.app.frame.Frame;
import com.vlengine.bounding.BoundingVolume;
import com.vlengine.light.DirectionalLight;
import com.vlengine.light.Light;
import com.vlengine.light.LightSorter;
import com.vlengine.light.PointLight;
import com.vlengine.light.SpotLight;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.scene.Renderable;
import com.vlengine.light.Shadow;
import com.vlengine.renderer.RenderQueue;

/**
 * This batch represents the drawing work related to drawing
 * a light in defferred lighting.
 * 
 * @author vear (Arpad Vekas)
 */
public class LightBatch extends Renderable {

    // the object holding the light properties
    // this is also usable in fixed-function pieline (LightState)
    protected Light light;
    
    // the bounding volume of the light, geometry inside this
    // bound is affected by the light, also geometry inside this
    // bound is affected by the lights shadow (if it has one)
    //protected BoundingVolume bound;
    
    // the shadow casting properties of this light
    protected Shadow shadow;

    protected Vector3f tmpVec = new Vector3f();
    
    public LightBatch() {
        super();
        worldBound = new BoundingVolume[Frame.MAX_FRAMES];
        setRenderQueueMode(RenderQueue.QueueFilter.Light.value);
        setRenderPassMode(RenderPass.PassFilter.Light.value);
    }

    public void setLight(Light l) {
        light = l;
        // create a bound for the light
        //bound = LightSorter.createLightBound(l);
    }

    public Light getLight() {
        return light;
    }

    public void createShadow(boolean perspective) {
        shadow = new Shadow(perspective);
        shadow.setParent(this);
    }
    
    public Shadow getShadow() {
        return shadow;
    }
    
    public void updateWorldBound( int frameId ) {
        if( light!= null && light.getBound() != null ) {
            worldBound[ frameId ] = light.getBound().clone(worldBound[ frameId ]).transform(
                                    worldTransform[ frameId ].getRotation(),
                                    worldTransform[ frameId ].getTranslation(),
                                    worldTransform[ frameId ].getScale(), worldBound[ frameId ]);
        }
    }
    
    public BoundingVolume getLocalBound() {
        return light!=null ? light.getBound() : null;
    }

    @Override
    public boolean docull( CullContext ctx ) {
        if(light==null)
            return false;
        updateWorldBound( ctx.getFrameId() );
        return super.docull(ctx);
    }
    
    /**
     * If the transforms on the LightNode is not locked,
     * the batch will need updating.
     * @param ctx
     */
    public void update(CullContext ctx) {
    }

    // transfer data into the light object for rendering
    @Override
    public boolean prepare( RenderContext ctx ) {
        super.prepare(ctx);

        Vector3f worldTranslation = worldTransform[ctx.frameId].getTranslation();
        Quaternion worldRotation = worldTransform[ctx.frameId].getRotation();

        switch (light.getType()) {
        case Light.LT_DIRECTIONAL: {
            // directional light is a rotated quad, light is cast as a normal from 
            // the plane
            // TODO: use rotation for direction, and not the position
            DirectionalLight dLight = (DirectionalLight) light;
            //dLight.getDirection().set(worldTranslation).negateLocal();
            worldRotation.getRotationColumn(2, dLight.getDirection());
            dLight.getDirection().negateLocal();
            break;
        }

        case Light.LT_POINT: {
            // point light is a sphere, the light is cast from the center
            // to the surface of the globe
            PointLight pLight = (PointLight) light;
            pLight.getLocation().set(worldTranslation);
            break;
        }

        case Light.LT_SPOT: {
            // spotlight is a cone, the light is cast from the apex
            // to the bottom of the cone
            SpotLight sLight = (SpotLight) light;
            sLight.getLocation().set(worldTranslation);
            worldRotation.getRotationColumn(2, sLight.getDirection());
            break;
        }

        default:
            break;
        }
        return true;
    }
    
    public void draw(RenderContext ctx) {
        //TODO:
        //ctx.getRenderer().draw(this);
    }
}
