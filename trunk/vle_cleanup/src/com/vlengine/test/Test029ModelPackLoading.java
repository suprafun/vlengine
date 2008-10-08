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
import com.vlengine.model.BaseGeometry;
import com.vlengine.resource.ParameterMap;
import com.vlengine.resource.model.Model;
import com.vlengine.resource.model.ModelPack;
import com.vlengine.resource.model.ModelPackCreator;
import com.vlengine.scene.Spatial;

/**
 * Demostrates loading a model from a saved modelpack, notice how it is way
 * faster than loading from OBJ model
 * @author vear (Arpad Vekas)
 */
public class Test029ModelPackLoading extends MainGame {

    @Override
    protected void simpleInitGame(AppContext app) {
        String modelname = "f-5etigerii(05).obj";

        ParameterMap p = new ParameterMap();
        p.put("listmode", BaseGeometry.LIST_NO);
        p.put("vbomode", BaseGeometry.VBO_LONGLIVED);
        
        ModelPack mp = app.getResourceFinder().getModelPack(modelname, p);
        if(mp!=null) {
            Model om = mp.getModel(modelname);
            if(om!= null) {
                Spatial lm = om.getInstance(app, ParameterMap.MAP_EMPTY);
                app.rootNode.attachChild(lm);
            }
        }
        
        // test ccamera frustrum calculation
        app.cam.setFrustumPerspective( 55.0f, (float) app.display.getWidth()
                / (float) app.display.getHeight(), 1f, app.conf.view_frustrum_far );
        app.cam.setParallelProjection( false );
        app.cam.update();
    }

    @Override
    protected void simpleUpdate(Frame f) {
        
    }
    
    public static void main( String[] args ) {
        Test029ModelPackLoading test = new Test029ModelPackLoading();
        test.start();
    }
}
