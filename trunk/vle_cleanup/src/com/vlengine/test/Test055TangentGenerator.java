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
import com.vlengine.light.DirectionalLight;
import com.vlengine.light.Light;
import com.vlengine.light.PointLight;
import com.vlengine.light.SpotLight;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.resource.ParameterMap;
import com.vlengine.resource.model.Model;
import com.vlengine.resource.model.ModelPack;
import com.vlengine.resource.model.ModelPackCreator;
import com.vlengine.resource.obj.ObjModel;
import com.vlengine.scene.LightNode;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.Spatial;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Test055TangentGenerator extends MainGame {

    protected void setupLight() {
        // 0-no
        // 1-directional
        // 2-point
        // 3-spot
        int testlight = 3;
        
        // enable single light testing
        if(testlight >0) {
            Light light = null;
            switch(testlight) {
                case 1: light = new DirectionalLight(); 
                        //((DirectionalLight)light).setDirection(new Vector3f(0.5f, -1f, 0.5f).normalizeLocal());
                        break;
                case 2: light = new PointLight();
                        ((PointLight)light).setLocation(new Vector3f( 0, 0, 0 ));
                        //((PointLight)light).setAttenuate(false);
                        break;
                case 3: light = new SpotLight();
                        //((SpotLight)light).setDirection(new Vector3f(-1f, -5f, -1f).normalizeLocal());
                        //((SpotLight)light).setLocation(new Vector3f( 10, 50, 10 ));
                        ((SpotLight)light).setAngle(40);
                        ((SpotLight)light).setExponent(100f);
                        
                        //((SpotLight)light).setDiffuse(ColorRGBA.red.clone());
                        break;
            }

            light.setAttenuate(false);
            light.setLinear(0.005f);
            light.setQuadratic(0f);
            light.setConstant(1f);
            
            light.setSpecular(ColorRGBA.red.clone());
            light.setDiffuse( new ColorRGBA( 0.8f, 0.8f, 0.8f, 1.0f ) );
            light.setAmbient( new ColorRGBA( 0f, 0f, 0f, 1.0f ) );
            //light.setAmbient( ColorRGBA.black.clone() );
            //light.setLocation(  );
            
            light.setEnabled( true );

            LightNode lin = new LightNode("demoLight", light);
            switch(testlight) {
                case 1: lin.getLocalTranslation().set(200, 100, 100);
                        lin.getLocalRotation().lookAt(lin.getLocalTranslation().negate(), Vector3f.UNIT_Y);
                    break;
               case 2: lin.getLocalTranslation().set(200, 100, 100);
                    break;
            }
            //lin.getLocalTranslation().set(100,100,100);
            //lin.getLocalRotation().lookAt(new Vector3f(0,-10,0), Vector3f.UNIT_Y);
            
            if(testlight==3) {
                app.camn.attachChild(lin);
            } else {
                app.rootNode.attachChild(lin);
            }
            
            app.camn.getLocalTranslation().set(47,40,23);
            app.camn.getLocalRotation().fromAngles(new float[] {-0.0033333874f, 1.6399999f, 0f});
            lin.updateWorldVectors(app.nullUpdate);
        }
    }
    
    @Override
    protected void simpleInitGame(AppContext app) {
        //String modelname = "level_mesh_v3_9.obj";
        //String modelname = "testboxes.obj";
        String modelname = "trade_shopbldg.obj";
        //String modelname = "johanstockcorp.obj";
                
        // try to load from the pack
        ModelPack mp=app.getResourceFinder().getModelPack(modelname, ParameterMap.MAP_EMPTY);
        if(mp==null) {
            ParameterMap p = new ParameterMap();
            p.put("listmode", BaseGeometry.LIST_NO);
            p.put("vbomode", BaseGeometry.VBO_YES);
            //p.put("vbomode", BaseGeometry.VBO_NO);
            Model om = app.getResourceFinder().getModel(modelname, p);
            if(om==null)
                return;
            ModelPackCreator mpc = new ModelPackCreator();
            mpc.addModel(om);
            mp = mpc.createPack(modelname);
            mp.save(app.conf.cache_path+"/test");
        }
        if(mp!= null) {
            // convert the model
            // get the converted model
            Model om= mp.getModel(modelname);
            if( om != null) {
                Spatial lm = om.getInstance(app, ParameterMap.MAP_EMPTY);
                //lm.setCullMode(Spatial.CULL_NEVER);
                app.rootNode.attachChild(lm);
            }
        }
        
        setupLight();
        
    }

    @Override
    protected void simpleUpdate(Frame f) {
        
    }
    
    public static void main( String[] args ) {
        Test055TangentGenerator test = new Test055TangentGenerator();
        test.start();
    }
}
