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

package com.vlengine.test;

import com.vlengine.app.AppContext;
import com.vlengine.app.MainGame;
import com.vlengine.app.frame.Frame;
import com.vlengine.bounding.BoundingBox;
import com.vlengine.image.Texture;
import com.vlengine.math.FastMath;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.model.Box;
import com.vlengine.model.Quad;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.material.Material;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.AnalyticSkyDome;
import com.vlengine.scene.Mesh;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.LightState;
import com.vlengine.scene.state.MaterialState;
import com.vlengine.scene.state.TextureState;
import com.vlengine.system.VleException;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Test032SkyDome extends MainGame {

    @Override
    protected void simpleInitGame(AppContext app) {
        AnalyticSkyDome dome;
        
        app.conf.lightmode = 1;
        /*
        if(app.conf.lightmode==0) {
            
            LightSorterGameState lightManagement = new LightSorterGameState();
            lightManagement.setActive(true);
            app.getGameStates().attachChild(lightManagement);
            //app.lightState.setEnabled(false);
            //app.lightEffect.setEnabled(false);
        }
         */
        
        dome = new AnalyticSkyDome("skyDome", 20, 50, app.conf.view_frustrum_far * 3 / 4,
        		app.conf.view_frustrum_far/2 , app.conf.view_frustrum_far);
        //dome.updateRenderState();
        dome.setUpdateTime(0.1f);//0.05f
        dome.setTimeWarp(3000.0f);
        dome.setDay(67);
        dome.setLatitude(-19f);
        dome.setLongitude(-47.5f);
        dome.setStandardMeridian(-45.0f);
        dome.setTurbidity(3.5f);//2
        dome.setSunEnabled(true);
        dome.setExposure(true, 20.0f);//18.0
        dome.setOvercastFactor(0.3f);//0
        dome.setGammaCorrection(2.5f);//2.5
        dome.setSunPosition(6);
        //dome.setIntensity(0.5f);
        dome.getSunLight().setAmbient(ColorRGBA.makeIntensity(0.4f));
        dome.getSunLight().setDiffuse(ColorRGBA.makeIntensity(0.8f));

        app.rootNode.attachChild(dome);
        
        Box b = new Box(new Vector3f(0,0,0), 50,50,50);
        b.setModelBound(new BoundingBox());
        b.updateModelBound();

        TriBatch t = new TriBatch();
        t.setModel(b);
        // this is casting shadow
        long shadowCaster = (RenderQueue.FILTER_OPAQUE);
        t.setRenderQueueMode(shadowCaster);
        
        Texture tex = app.getResourceFinder().getTexture("tangents.jpg", ParameterMap.MAP_EMPTY);

        if ( tex == null ) {
            throw new VleException("Cannot load texture");
        }
        
        // create texturestate
        TextureState txs = new TextureState();
        txs.setTexture(tex,0);
        txs.setEnabled(true);
        
        // crate material
        Material mat = new Material();
        mat.setRenderState(txs);
        
        // create materialstate
        MaterialState mats = new MaterialState();
        mats.setAmbient(ColorRGBA.black);
        
        mat.setRenderState(mats);

        // set material to batch as opaque material
        t.setMaterial(mat);
        
        Mesh m = new Mesh("Box");
        m.setBatch(t);
        m.getLocalTranslation().set(0f,200f,0f);
        m.updateWorldVectors(app.nullUpdate);
        app.rootNode.attachChild(m);
        
        // create the ground quad
        Quad q = new Quad(5000,5000);
        q.setModelBound(new BoundingBox());
        q.updateModelBound();

        TriBatch tq = new TriBatch();
        tq.setModel(q);
        tq.setRenderQueueMode(shadowCaster);
        tq.setCullMode(Spatial.CullMode.NEVER);
        
        Texture texq = app.getResourceFinder().getTexture("tangents.jpg", ParameterMap.MAP_EMPTY);

        if ( texq == null ) {
            throw new VleException("Cannot load texture");
        }
        
        // create texturestate
        TextureState txsq = new TextureState();
        txsq.setTexture(tex, 0);
        txsq.setEnabled(true);
        
        // crate material
        Material matq = new Material();
        matq.setRenderState(txsq);
        matq.setLightCombineMode(LightState.OFF);
        
        // set material to batch as opaque material
        tq.setMaterial(matq);
        
        Mesh mq = new Mesh("quad");
        mq.setBatch(tq);
        mq.getLocalTranslation().set(0,0,0);
        Quaternion qr = new Quaternion();
        qr.fromAngles(-FastMath.HALF_PI, 0, 0);
        mq.getLocalRotation().set(qr);
        mq.updateWorldVectors(app.nullUpdate);
        app.rootNode.attachChild(mq);


        Box bl = new Box(new Vector3f(0,0,0), 5,5,5);
        bl.setModelBound(new BoundingBox());
        bl.updateModelBound();
        
        //q.setDisplayListMode(BaseGeometry.LIST_NO);
        TriBatch tl = new TriBatch();
        tl.setModel(bl);
        // this object does not cast shadow
        tl.setRenderQueueMode(RenderQueue.FILTER_OPAQUE);
        tl.setCullMode(Spatial.CullMode.NEVER);
        Mesh ml = new Mesh("lightpos");
        ml.setBatch(tl);
        ml.updateWorldVectors(app.nullUpdate);
        
        app.camn.getLocalTranslation().set(0,600,600);
    }

    public static void main(String[] args) {
        Test032SkyDome test = new Test032SkyDome();
        test.start();
    }

    @Override
    protected void simpleUpdate(Frame f) {
        
    }
}
