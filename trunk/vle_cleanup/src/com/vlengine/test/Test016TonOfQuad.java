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
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Quad;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.material.Material;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.CullState;
import com.vlengine.scene.state.TextureState;
import com.vlengine.system.VleException;

/**
 * Renders 1K quads
 * @author vear (Arpad Vekas)
 */
public class Test016TonOfQuad extends MainGame {

    @Override
    protected void simpleInitGame(AppContext app) {
        
        
        // load texture
        Texture tex = app.getResourceFinder().getTexture("tangents.jpg", ParameterMap.MAP_EMPTY);
        if ( tex == null ) {
            throw new VleException("Cannot load texture");
        }
        
        // create texturestate
        TextureState txs = new TextureState();
        txs.setTexture(tex);
        txs.setEnabled(true);
        
        CullState cs = new CullState();
        cs.setCullMode(CullState.CS_NONE);
        cs.setEnabled(true);
        
        // create material
        Material mat = new Material();
        mat.setRenderState(txs);
        mat.setRenderState(cs);
        
            // create quad geometry
            //Quad q = new Quad(100,100);
            //VBOInfo vi = new VBOInfo(false);
            //q.setVBOInfo(vi);
            //VBOAttributeInfo vboindex = new VBOAttributeInfo();
            //vboindex.useVBO = false;
            //q.getIndexBuffer().setVBOInfo(vboindex);
            
            Quad q1 = new Quad(100,100);
            // enable VBO for the quad
            q1.setVBOMode(BaseGeometry.VBO_LONGLIVED);
            q1.createVBOInfos();
            
            Quad q2 = new Quad(100,100);
            // enable VBO for the quad
            q2.setVBOMode(BaseGeometry.VBO_LONGLIVED);
            q2.createVBOInfos();
            
        for(int i=0; i< 1000; i++) {
            // create the renderable batch
            TriBatch t = new TriBatch();
            if(i%2==0)
                t.setModel(q1);
            else
                t.setModel(q2);
            t.setRenderQueueMode(RenderQueue.FILTER_OPAQUE);
            t.setCullMode(Spatial.CullMode.NEVER);
            // set material to batch as opaque material
            t.setMaterial(mat);
            // creeate mesh
            LodMesh m = new LodMesh("Quad "+i);
            // add batch as default lod
            m.addBatch(0, t);
            m.getLocalTranslation().set(FastMath.rand.nextFloat()*1000-500,FastMath.rand.nextFloat()*1000-500,FastMath.rand.nextFloat()*1000-500);
            app.rootNode.attachChild(m);
            
        }
            
        
    }

    public static void main(String[] args) {
        Test016TonOfQuad test = new Test016TonOfQuad();
        test.start();
    }

    @Override
    protected void simpleUpdate(Frame f) {
        
    }
}
