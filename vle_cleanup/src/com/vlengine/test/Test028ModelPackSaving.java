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
 * Demostrates converting and saving a model into a modelpack
 * @author vear (Arpad Vekas)
 */
public class Test028ModelPackSaving extends MainGame {

    @Override
    protected void simpleInitGame(AppContext app) {
        String modelname = "f-5etigerii(05).obj";
        
        ParameterMap p = new ParameterMap();
        p.put("listmode", BaseGeometry.LIST_NO);
        p.put("vbomode", BaseGeometry.VBO_YES);
        // dont flip faces with flipped normals
        p.put("obj_allow_face_flip", false);
        // channel if not requestted
        p.put("obj_check_texture_alpha_chanel", false);
        
        Model om = app.getResourceFinder().getModel(modelname, p);
        if(om!= null) {
            ModelPackCreator mpc = new ModelPackCreator();
            mpc.addModel(om);
            ModelPack mp = mpc.createPack(modelname);
            mp.save(app.conf.cache_path+"/f5etiger");
        }
    }

    @Override
    protected void simpleUpdate(Frame f) {
        
    }
    
    public static void main( String[] args ) {
        Test028ModelPackSaving test = new Test028ModelPackSaving();
        test.start();
    }
}
