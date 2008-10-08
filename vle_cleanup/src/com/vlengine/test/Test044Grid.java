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
import com.vlengine.math.Vector3f;
import com.vlengine.model.Box;
import com.vlengine.model.Quad;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.material.Material;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.Mesh;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.AlphaBlendState;
import com.vlengine.scene.state.CullState;
import com.vlengine.scene.state.TextureState;
import com.vlengine.system.VleException;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Test044Grid extends MainGame {

    @Override
    protected void simpleInitGame(AppContext app) {
        Box q = new Box(new Vector3f(0,0,0), 100,100,100);
        //q.setDisplayListMode(BaseGeometry.LIST_NO);
        TriBatch t = new TriBatch();
        t.setModel(q);
        t.setRenderQueueMode(RenderQueue.FILTER_OPAQUE);
        t.setCullMode(Spatial.CULL_NEVER);
        
        Texture tex = app.getResourceFinder().getTexture("tangents.jpg", ParameterMap.MAP_EMPTY);

        if ( tex == null ) {
            throw new VleException("Cannot load texture");
        }
        
        // create texturestate
        TextureState txs = new TextureState();
        txs.setTexture(tex);
        txs.setEnabled(true);
        
        // crate material
        Material mat = new Material();
        mat.setRenderState(txs);
        
        // set material to batch as opaque material
        t.setMaterial(mat);
        
        LodMesh m = new LodMesh("Box");
        m.addBatch(0, t);
        m.getLocalTranslation().set(0,0,-200);
        m.updateWorldVectors(app.nullUpdate);
        app.rootNode.attachChild(m);
        
        Quad gridq = new Quad(4096,4096);
        gridq.setModelBound(new BoundingBox());
        gridq.updateModelBound();
        TriBatch tb = new TriBatch();
        tb.setRenderQueueMode(RenderQueue.QueueFilter.AlphaTested.value);
        tb.setModel(gridq);
        
        Material gridmat = new Material();
        TextureState ts = new TextureState();
        Texture gt=app.getResourceFinder().getTexture("gridlines1.png", ParameterMap.MAP_EMPTY);
        gt.setScale(new Vector3f(20f,20f,20f));
        ts.setTexture(gt);
        gridmat.setRenderState(ts);
        AlphaBlendState as = new AlphaBlendState();
        as.setSourceFunction(AlphaBlendState.SB_ONE_MINUS_DST_COLOR);
        as.setDestinationFunction(AlphaBlendState.DB_ONE);
        as.setEnabled(true);
        gridmat.setRenderState(as);
        CullState cs = new CullState();
        cs.setCullMode(CullState.CS_NONE);
        gridmat.setRenderState(cs);
        gridmat.setRenderQueueMode(RenderQueue.QueueFilter.AlphaTested.value);
        tb.setMaterial(gridmat);
        grid = new Mesh("Grid");
        grid.setBatch(tb);
        grid.getLocalTranslation().set(0, 0, 0);
        grid.getLocalRotation().fromAngles((float) Math.toRadians(-90),0, 0 );
        
        app.rootNode.attachChild(grid);
        
        
        
        // -Z = north, -X = west, Y = height
        //cam.lookAt(m.getWorldTranslation(), cam.getUp());
    }

    Mesh grid;
    public static void main(String[] args) {
        Test044Grid test = new Test044Grid();
        test.start();
    }

    @Override
    protected void simpleUpdate(Frame f) {
        Vector3f ct = f.getApp().camn.getLocalTranslation();
        float cx = ((int)(ct.x/256))*256;
        float cz = ((int)(ct.z/256))*256;
        grid.getLocalTranslation().set(cx, 0, cz);
    }
}
