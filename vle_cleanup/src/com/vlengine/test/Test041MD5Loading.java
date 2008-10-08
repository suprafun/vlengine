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
import com.vlengine.math.Vector3f;
import com.vlengine.resource.ParameterMap;
import com.vlengine.resource.md5.Md5MeshLoader;
import com.vlengine.resource.model.Model;
import com.vlengine.scene.SetNode;
import com.vlengine.scene.Spatial;
import java.nio.ByteBuffer;

/**
 * MD5 model loading is not complete
 * @author vear (Arpad Vekas)
 */
public class Test041MD5Loading extends MainGame {

    @Override
    protected void simpleInitGame(AppContext app) {
        
        // load the model file
        
        // the sarge node
        SetNode npc = new SetNode("npc");
        
        // the body model
        Model mdl = app.getResourceFinder().getModel("marine.md5mesh", ParameterMap.MAP_EMPTY);
        
        if(mdl==null) {
            app.finished = true;
            return;
        }
        ParameterMap params = new ParameterMap();
        // apply bindpose transform
        params.put("BINDPOSE", true);
        Spatial m = mdl.getInstance(app, params);
        if(m==null) {
            app.finished = true;
            return;
        }
        npc.attachChild(m);
        
        npc.getLocalTranslation().set(0,0,-200);
        npc.getLocalRotation().set(-0.5f, -0.5f, -0.5f, 0.5f);
        npc.updateWorldVectors(app.nullUpdate);
        
        // 
        app.rootNode.attachChild(npc);
        
        
    }

    public static void main(String[] args) {
        Test041MD5Loading test = new Test041MD5Loading();
        test.start();
    }

    @Override
    protected void simpleUpdate(Frame f) {
        
    }
}
