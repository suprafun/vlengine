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

package com.vlengine.scene;

import com.vlengine.math.Vector3f;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.scene.control.UpdateContext;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class CameraNode extends SetNode {
    
    // the original vectors
    private Vector3f origUp = new Vector3f();
    private Vector3f origLeft = new Vector3f();
    private Vector3f origDirection = new Vector3f();
    private Vector3f newDirection = new Vector3f();
    private ViewCamera camera;
    
    public CameraNode(String name) {
        super(name);
        renderQueueMode = RenderQueue.QueueFilter.None.value;
    }
    
    public void setCamera(ViewCamera cam) {
        camera = cam;
        origUp.set(camera.getUp());
        origLeft.set(camera.getLeft());
        origDirection.set(camera.getDirection());
    }
    
    @Override
    public void updateWorldData(UpdateContext ctx) {
        super.updateWorldData(ctx);
        
        // update camera
        //worldRotation.mult(origUp, camera.getUp());
        //worldRotation.mult(origLeft, camera.getLeft());
        
        // set location
        camera.getLocation().set(worldTranslation);
        
        // set rotation
        worldRotation.mult(origDirection, newDirection);
        camera.lookDirection(newDirection, origUp);
        camera.update();
    }

    @Override
    public void updateWorldBound() {
        return;
    }
}
