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
import com.vlengine.math.Vector3f;
import com.vlengine.model.Quad;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.material.Material;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.TextureState;
import com.vlengine.system.VleException;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexFormat;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Test017CompositeQuad extends MainGame {

    @Override
    protected void simpleInitGame(AppContext app) {
        Quad q = new Quad(100,100, VertexFormat.setRequested( VertexFormat.setRequested(
                VertexFormat.setRequested(0, VertexAttribute.USAGE_POSITION)
                ,VertexAttribute.USAGE_NORMAL)
                ,VertexAttribute.USAGE_TEXTURE0), 
                false);
        //q.setDisplayListMode(BaseGeometry.LIST_NO);
        TriBatch t = new TriBatch();
        t.setModel(q);
        t.setRenderQueueMode(RenderQueue.FILTER_OPAQUE);
        t.setCullMode(Spatial.CullMode.NEVER);
        
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
        
        LodMesh m = new LodMesh("Quad");
        m.addBatch(0, t);
        m.getLocalTranslation().set(0,0,-200);
        m.updateWorldVectors(app.nullUpdate);
        app.rootNode.attachChild(m);
        
        // -Z = north, -X = west, Y = height
        //cam.lookAt(m.getWorldTranslation(), cam.getUp());
    }

    public static void main(String[] args) {
        Test017CompositeQuad test = new Test017CompositeQuad();
        test.start();
    }

    @Override
    protected void simpleUpdate(Frame f) {
        
    }
}
