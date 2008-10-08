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

package com.vlengine.app.state;

import com.vlengine.app.AppContext;
import com.vlengine.app.frame.Frame;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.FrameBuffer;
import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.control.UpdateContext;

/**
 * A renderpath controls the renderpass configuration
 * @author vear (Arpad Vekas)
 */
public abstract class RenderPath extends GameState {

    AppContext app;
    
    public RenderPath(AppContext app) {
        this.app = app;
    }
    
    public abstract void setup();

    public abstract void createDefaultPasses(Frame f);
    
    @Override
    public void preFrame(AppContext ctx) {
        
    }

    @Override
    public void preUpdate(UpdateContext ctx) {
        
    }

    @Override
    public void preCull(Frame f) {
        
    }

    @Override
    public void preCull(CullContext ctx) {
        
    }

    @Override
    public void postCull(CullContext ctx) {
        
    }

    @Override
    public void preMaterial(RenderContext ctx) {
        
    }

    public abstract FrameBuffer getRootFrameBuffer();

    @Override
    public void preRender(RenderContext ctx) {
        
    }

    @Override
    public void postRender(RenderContext ctx) {
        
    }

    @Override
    public void afterRender(RenderContext ctx) {
        
    }

    @Override
    public abstract void cleanup();

}
