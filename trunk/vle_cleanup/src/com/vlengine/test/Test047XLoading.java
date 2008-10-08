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
import com.vlengine.bounding.BoundingVolume;
import com.vlengine.light.DirectionalLight;
import com.vlengine.light.Light;
import com.vlengine.light.LightSorterGameState;
import com.vlengine.light.PointLight;
import com.vlengine.light.SpotLight;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.pass.OpaquePass;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.resource.ParameterMap;
import com.vlengine.resource.model.Model;
import com.vlengine.scene.LightNode;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.animation.Action;
import com.vlengine.scene.animation.x.XBoneAnimationController;

/**
 * Demonstrates loading an X animated mesh exported from Maya 2008
 * with XExporter(); plugin
 * @author vear (Arpad Vekas)
 */
public class Test047XLoading extends MainGame {

    @Override
    protected void simpleInitGame(AppContext app) {

        String mesh = "testbox2.x";

        ParameterMap p = new ParameterMap();
        //p.put("listmode", BaseGeometry.LIST_YES);
        p.put("vbomode", BaseGeometry.VBO_LONGLIVED);
        //p.put("BINDPOSE", true);
        //p.put("processnormals", false);
        Model om = app.getResourceFinder().getModel(mesh, p);
        if(om!= null) {
            om.setModelBound(BoundingVolume.BOUNDING_SPHERE);
            Spatial lm = om.getInstance(app, p);
            Action a = new Action();
            a.loop = Action.LoopMode.RestartAtEnd;
            //a.name="walk";
            //a.name="wiggle";
            //a.name="high";
            //a.name="run";
            a.id=0;
            //a.startFrame = 10;
            ((XBoneAnimationController)lm.getController("BoneAnim")).scheduleAction(a);
            //a.release();
            lm.getLocalRotation().lookAt(new Vector3f(100,0,0), Vector3f.UNIT_Y);
            app.rootNode.attachChild(lm);
        }
        
        // 0-no
        // 1-directional
        // 2-point
        // 3-spot
        int testlight = 3;
        app.conf.lightmode=1;
        app.conf.nospecular=false;
        
        // enable single light testing
        if(testlight >0) {
            Light light = null;
            switch(testlight) {
                case 1: light = new DirectionalLight(); 
                        //((DirectionalLight)light).setDirection(new Vector3f(0.5f, -1f, 0.5f).normalizeLocal());
                        break;
                case 2: light = new PointLight();
                        ((PointLight)light).setLocation(new Vector3f( 0, 0, 0 ));
                        break;
                case 3: light = new SpotLight();
                        ((SpotLight)light).setDirection(new Vector3f(-1f, -5f, -1f).normalizeLocal());
                        ((SpotLight)light).setLocation(new Vector3f( 10, 50, 10 ));
                        ((SpotLight)light).setAngle(7);
                        ((SpotLight)light).setExponent(100f);
                        ((SpotLight)light).setSpecular(ColorRGBA.red.clone());
                        break;
            }

            light.setAttenuate(true);
            light.setLinear(0.05f);
            light.setQuadratic(0f);
            light.setConstant(1f);
            
            
            light.setDiffuse( new ColorRGBA( 0.8f, 0.8f, 0.8f, 1.0f ) );
            light.setAmbient( new ColorRGBA( 0f, 0f, 0f, 1.0f ) );
            //light.setAmbient( ColorRGBA.black.clone() );
            //light.setLocation(  );
            
            light.setEnabled( true );

            LightNode lin = new LightNode("demoLight", light);
            switch(testlight) {
                case 1: lin.getLocalTranslation().set(-0.5f, 1, -0.5f);
                        lin.getLocalRotation().lookAt(lin.getLocalTranslation().negate(), Vector3f.UNIT_Y);
                    break;
            }
            //lin.getLocalTranslation().set(100,100,100);
            //lin.getLocalRotation().lookAt(new Vector3f(0,-10,0), Vector3f.UNIT_Y);
            lin.updateWorldVectors(app.nullUpdate);
            if(testlight==3) {
                app.camn.attachChild(lin);
            } else {
                app.rootNode.attachChild(lin);
            }
            
            app.camn.getLocalTranslation().set(0,0,100);
            //app.camn.getLocalRotation().lookAt(new Vector3f(100,0,-100f), Vector3f.UNIT_Y);
            
            /*
            if(app.conf.lightmode==1) {
                LightSorterGameState lightManagement = new LightSorterGameState();
                lightManagement.setActive(true);
                app.getGameStates().attachChild(lightManagement);
            }
             */
        }
    }

    @Override
    protected void simpleUpdate(Frame f) {
        //f.getPasses().getPassById(RenderPass.PASS_DEPTH).setEnabled(false);
        //((OpaquePass)f.getPasses().getPassById(RenderPass.PASS_OPAQUE)).setZbufferWrite(true);
        //app.setPaused(true);
    }
    
    public static void main( String[] args ) {
        Test047XLoading test = new Test047XLoading();
        test.start();
    }
}
