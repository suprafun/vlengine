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

package com.vlengine.scene.animation;

import com.vlengine.scene.control.Controller;
import com.vlengine.scene.control.UpdateContext;

/**
 * Controller that runs animations based on BoneAnimation and WeightedGeometry
 * 
 * @author vear (Arpad Vekas)
 */
public abstract class BoneAnimationController extends Controller {
    
    // the reference absolute time
    protected float referenceTime = 0;
    // run animations at 60 FPS the most
    protected float throttle = (1f/60f);
    // the time since last update
    protected float lastUpdate = 0;
    // do we force update
    protected boolean shouldUpdate = true;

    public BoneAnimationController(String name) {
        super(name);
    }

    @Override
    public void update(UpdateContext ctx) {
        float currTime = ctx.frame.getTimer().getTimeInSeconds();
        float time = currTime - referenceTime;
        referenceTime=currTime;
        lastUpdate += time;
        if(lastUpdate<throttle && !shouldUpdate)
            return;
        updateAnimations(ctx);
        lastUpdate = 0;
        shouldUpdate = false;
    }
    
    protected abstract void updateAnimations(UpdateContext ctx);

    public abstract void scheduleAction(Action a);
    
    public float getRefernceTime() {
        return referenceTime;
    }
    
    public void forceUpdate() {
        shouldUpdate = true;
    }
}
