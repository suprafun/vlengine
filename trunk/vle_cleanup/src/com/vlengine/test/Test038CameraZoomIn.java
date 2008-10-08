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
import com.vlengine.model.Box;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.material.Material;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.TextureState;
import com.vlengine.system.VleException;
import com.vlengine.util.geom.BufferUtils;
import com.vlengine.util.geom.VertexAttribute;
import java.nio.FloatBuffer;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Test038CameraZoomIn extends MainGame {

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
        m.getLocalTranslation().set(0f,0f,-500);
        m.updateWorldVectors(app.nullUpdate);
        app.rootNode.attachChild(m);
        
        // look the camera on the box
        app.cam.getLocation().set(0,0,300);
        app.cam.lookAt(m.getWorldTranslation(), app.cam.getUp());
        app.cam.setFrustumPerspective(45.0f, ((float)app.display.getWidth())/((float)app.display.getHeight()), 1f, 5000.0f);
        //app.cam.update();

        // get the vertices of the box
        FloatBuffer vertsBuf = q.getAttribBuffer(VertexAttribute.USAGE_POSITION).getDataBuffer();
        Vector3f[] verts = BufferUtils.getVector3Array(vertsBuf);

        app.cam.update();
        // zoom on the box geometry
        app.cam.setZoom(verts);
        app.cam.update();

        // -Z = north, -X = west, Y = height
        //cam.lookAt(m.getWorldTranslation(), cam.getUp());
    }

    public static void main(String[] args) {
        Test038CameraZoomIn test = new Test038CameraZoomIn();
        test.start();
    }

    @Override
    protected void simpleUpdate(Frame f) {
        
    }
}
