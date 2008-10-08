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
import com.vlengine.scene.animation.MD5.MD5BoneAnimation;
import com.vlengine.resource.model.Model;
import com.vlengine.scene.SetNode;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.animation.MD5.MD5BoneAnimationPack;

/**
 * MD5 manimation loading is not complete
 * @author vear (Arpad Vekas)
 */
public class Test042MD5AnimLoading extends MainGame {

    @Override
    protected void simpleInitGame(AppContext app) {
        // register test folder
        app.getResourceFinder().addClassPathFolder("/testdata", "com/vlengine/test/data", "");
        // load the model file
        
        // the sarge node
        SetNode sarge = new SetNode("sarge");
        
        // the body model
        Model mdl = app.getResourceFinder().getModel("marine.md5mesh", ParameterMap.MAP_EMPTY);
        
        if(mdl==null) {
            app.finished = true;
            return;
        }
        
        // the animation
        MD5BoneAnimation anim = app.getResourceFinder().getAnimation("marine.md5anim", ParameterMap.MAP_EMPTY);
        anim.group = MD5BoneAnimation.GROUP_MOVE;

        // create animation pack
        MD5BoneAnimationPack animPack = new MD5BoneAnimationPack();
        animPack.addAnimation("walk", anim);
        // create the animation controller
        
        
        ParameterMap params = new ParameterMap();
        // apply bindpose transform
        params.put("BINDPOSE", true);
        Spatial m = mdl.getInstance(app, params);
        if(m==null) {
            app.finished = true;
            return;
        }
        sarge.attachChild(m);
        
        // the head model
        Model head = app.getResourceFinder().getModel("sarge.md5mesh", ParameterMap.MAP_EMPTY);
        
        if(head==null) {
            app.finished = true;
            return;
        }
        
        Spatial h = head.getInstance(app, params);
        if(h==null) {
            app.finished = true;
            return;
        }
        // extract the shoulder transform
        
        sarge.attachChild(h);
        
        sarge.getLocalTranslation().set(0,0,-200);
        sarge.getLocalRotation().set(-0.5f, -0.5f, -0.5f, 0.5f);
        sarge.updateWorldVectors(app.nullUpdate);
        
        // 
        app.rootNode.attachChild(sarge);
        
        
    }

    public static void main(String[] args) {
        Test042MD5AnimLoading test = new Test042MD5AnimLoading();
        test.start();
    }

    @Override
    protected void simpleUpdate(Frame f) {
        
    }
}
