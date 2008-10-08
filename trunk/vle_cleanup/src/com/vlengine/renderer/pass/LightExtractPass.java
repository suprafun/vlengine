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

package com.vlengine.renderer.pass;

import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.RenderQueue;

/**
 * The light-pass is responsible for preparing the 
 * light-indexed lighting for the opaque pass.
 * 
 * @author vear (Arpad Vekas)
 */
public class LightExtractPass extends RenderPass {
    
    public LightExtractPass() {
        this("lightindex");
    }
    
    public LightExtractPass(String name) {
        super(name);
        setId(RenderPass.StandardPass.Ligh.passId);
        setUsedMaterialFlags(-1);
        // renders queue 0
        setQueueNo(RenderQueue.StandardQueue.Ligh.queuId);
        // only render objects intended for opaque queue
        setQueueFilter(RenderQueue.QueueFilter.Light.value);
    }
    
    @Override
    public void renderPass(RenderContext ctx) {
        // this renderpass is a placeholder, the real work needs to be
        // done in the LightShadowGameState, because it is a work
        // which need to be done before entering the render stage
        
    }
}
