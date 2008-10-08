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

package com.vlengine.scene.batch;

import com.vlengine.app.frame.Frame;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.CullContext;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.Text;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class TextBatch extends Renderable {
    private StringBuffer text[];
    private ColorRGBA textColor[];
    
    
    
    public TextBatch() {
        super();

        text=new StringBuffer[Frame.MAX_FRAMES];
        textColor=new ColorRGBA[Frame.MAX_FRAMES];
        
        for(int i=0;i<Frame.MAX_FRAMES;i++) {
            text[i]=new StringBuffer();
            textColor[i]=new ColorRGBA();
        }
    }
    
    public ColorRGBA getTextColor(int frameId) {
        return textColor[frameId];
    }
    
    public StringBuffer getText( int frameId ) {
        return text[frameId];
    }
    
    @Override
    public boolean queue( CullContext ctx ) {
        return super.queue(ctx);
    }
    
    @Override
    public boolean docull(CullContext ctx) {
        return true;
    }

    public void update( CullContext ctx ) {
        int frameId = ctx.getFrameId();
        text[frameId].setLength(0);
        text[frameId].append(((Text)parent).getText());
        textColor[frameId].set(((Text)parent).getTextColor());
    }
    
    @Override
    public void draw(RenderContext ctx) {
        ctx.getRenderer().draw(this);
    }
    
    public void updateWorldBound(int frameid) {
        return;
    }
}
