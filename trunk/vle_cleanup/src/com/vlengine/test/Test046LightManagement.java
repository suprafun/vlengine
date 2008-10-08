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
import com.vlengine.image.Texture;
import com.vlengine.math.FastMath;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.model.Box;
import com.vlengine.model.Quad;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.material.Material;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.LightNode;
import com.vlengine.scene.Mesh;
import com.vlengine.scene.Node;
import com.vlengine.scene.SetNode;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.MaterialState;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.TextureState;
import com.vlengine.light.LightShadowGameState;
import com.vlengine.light.LightSorterGameState;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.system.VleException;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Test046LightManagement extends MainGame {

    Node lc;
    Node ln;
    LightNode lin;
    Vector3f lightcenter = new Vector3f(-100,500f,100f);
    float rottime = 0;
    Vector3f boxLocation = new Vector3f(0,200,0);
    int dimension = 256;
    
    @Override
    protected void simpleInitGame(AppContext app) {
        
        if(app.conf.lightmode==0) {
            app.conf.lightmode = 1;
            LightSorterGameState lightManagement = new LightSorterGameState();
            lightManagement.setActive(true);
            app.getGameStates().attachChild(lightManagement);
            //app.lightState.setEnabled(false);
            //app.lightEffect.setEnabled(false);
        }
                
        Box b = new Box(new Vector3f(0,0,0), 50,50,50);

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
        m.getLocalTranslation().set(boxLocation);
        m.updateWorldVectors(app.nullUpdate);
        app.rootNode.attachChild(m);
        
        // create the ground quad
        Quad q = new Quad(5000,5000);
        TriBatch tq = new TriBatch();
        tq.setModel(q);
        tq.setRenderQueueMode(shadowCaster);
        tq.setCullMode(Spatial.CULL_NEVER);
        
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
        //q.setDisplayListMode(BaseGeometry.LIST_NO);
        TriBatch tl = new TriBatch();
        tl.setModel(bl);
        // this object does not cast shadow
        tl.setRenderQueueMode(RenderQueue.FILTER_OPAQUE);
        tl.setCullMode(Spatial.CULL_NEVER);
        Mesh ml = new Mesh("lightpos");
        ml.setBatch(tl);
        ml.updateWorldVectors(app.nullUpdate);
        
        ln = new SetNode("lightnode");
        ln.attachChild(ml);
        
        lin = createLight();
        //app.rootNode.attachChild(lin);
        lin.updateWorldVectors(app.nullUpdate);
        ln.attachChild(lin);

        ln.getLocalTranslation().set(0f, 0f, 0f);
        lc = new SetNode("lightcenter");
        lc.getLocalRotation().fromAngleAxis(0, Vector3f.UNIT_Y);
        lc.getLocalTranslation().set(0,0f,0);

        ln.updateWorldVectors(app.nullUpdate);
        lc.attachChild(ln);
        lc.updateWorldVectors(app.nullUpdate);
        app.rootNode.attachChild(lc);
        
        app.camn.getLocalTranslation().set(0,600,600);

    }

    float[] angles = new float[3];

    Quaternion rot = new Quaternion();
    
    @Override
    protected void simpleUpdate(Frame f) {
        //f.getPasses().getPass("opaque").setEnabled(false);
        
        rottime += f.getUpdateContext().time;
        if(rottime>FastMath.PI*2)
            rottime -= FastMath.PI*2;
        
        rot.fromAngleAxis(rottime, Vector3f.UNIT_Y);
        rot.mult(lightcenter, ln.getLocalTranslation());
        //ln.getLocalTranslation().addLocal(boxLocation);
        //lc.updateGeometricState(f.getUpdateContext(), true);
        ln.getLocalRotation().lookAt(ln.getLocalTranslation().subtract(boxLocation), Vector3f.UNIT_Y);
        ln.updateGeometricState(f.getUpdateContext(), true);
        //app.lightState.setNeedsRefresh(true);
    }

    public static void main(String[] args) {
        Test046LightManagement test = new Test046LightManagement();
        test.start();
    }
}
