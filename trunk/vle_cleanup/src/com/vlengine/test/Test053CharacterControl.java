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
import com.vlengine.bounding.BoundingVolume;
import com.vlengine.bounding.CollisionVolume;
import com.vlengine.image.Texture;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Box;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.material.Material;
import com.vlengine.resource.ParameterMap;
import com.vlengine.resource.model.Model;
import com.vlengine.scene.Mesh;
import com.vlengine.scene.SceneElement;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.control.CharacterController;
import com.vlengine.scene.state.AlphaBlendState;
import com.vlengine.scene.state.LightState;
import com.vlengine.scene.state.TextureState;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Test053CharacterControl extends MainGame {

    protected void setupGrid() {
        Box gridq = new Box(Vector3f.ZERO.clone(), 2048, 64, 2048);
        //Quad gridq = new Quad(2048,2048);
        gridq.setModelBound(new BoundingBox());
        gridq.updateModelBound();
        // generate collision tree
        gridq.createCollisionVolume(CollisionVolume.DEFAULT_CELLSIZE_STATIC);
        gridq.setCollidable(true);
        
        TriBatch tb = new TriBatch();
        tb.setRenderQueueMode(RenderQueue.QueueFilter.BackGround.value);
        tb.setModel(gridq);
        tb.setCullMode(SceneElement.CULL_NEVER);
        Material gridmat = new Material();
        gridmat.setLightCombineMode(LightState.OFF);
        TextureState ts = new TextureState();
        Texture gt=app.getResourceFinder().getTexture("gridlines1.png", ParameterMap.MAP_EMPTY);
        ts.setTexture(gt);
        ts.setEnabled(true);
        gridmat.setRenderState(ts);
        AlphaBlendState as = new AlphaBlendState();
        as.setSourceFunction(AlphaBlendState.SB_ONE_MINUS_DST_COLOR);
        as.setDestinationFunction(AlphaBlendState.DB_ONE);
        as.setEnabled(true);
        gridmat.setRenderState(as);
        tb.setMaterial( gridmat);
        Mesh grid = new Mesh("Grid");
        grid.setBatch(tb);
        
        grid.getLocalTranslation().set(0, 0, 0);
        //grid.getLocalRotation().fromAngles((float) Math.toRadians(-90),0, 0 );
        
        app.rootNode.attachChild(grid);
    }

    @Override
    protected void simpleInitGame(AppContext app) {

        setupGrid();
        
        app.mouseInput.setCursorVisible(true);
        
        String mesh = "testbox2.x";
        
        ParameterMap p = new ParameterMap();
        //p.put("listmode", BaseGeometry.LIST_YES);
        p.put("vbomode", BaseGeometry.VBO_LONGLIVED);
        // generate collision tree for the character, if it does not yet exist
        p.put("coltree", true);
        
        //p.put("BINDPOSE", true);
        //p.put("processnormals", false);
        Model om = app.getResourceFinder().getModel(mesh, p);
        if(om!= null) {
            om.setModelBound(BoundingVolume.BOUNDING_BOX);
            Spatial lm = om.getInstance(app, p);
            
            // create the character controller
            CharacterController cc = new CharacterController();
            cc.setAppContext(app);
            cc.setControlledCamera(app.camn);
            cc.setControlledNode(lm);
            // uncomment this if you have proper animation sequences set up
            //ControllerAnimationPack ap = new ControllerAnimationPack();
            //ap.setAnimationController(((XBoneAnimationController)lm.getController("BoneAnim")));
            //cc.setAnimPack(ap);
            lm.addController(cc);
            cc.setupController();
            
            lm.getLocalRotation().lookAt(new Vector3f(100,-10,0), Vector3f.UNIT_Y);
            lm.getLocalTranslation().set(0,200,0);
            app.rootNode.attachChild(lm);
        }
    }

    @Override
    protected void simpleUpdate(Frame f) {
        //f.getPasses().getPassById(RenderPass.PASS_DEPTH).setEnabled(false);
        //((OpaquePass)f.getPasses().getPassById(RenderPass.PASS_OPAQUE)).setZbufferWrite(true);
        //app.setPaused(true);
    }
    
    public static void main( String[] args ) {
        Test053CharacterControl test = new Test053CharacterControl();
        test.start();
    }
}
