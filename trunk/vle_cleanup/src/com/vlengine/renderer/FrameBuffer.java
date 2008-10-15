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

package com.vlengine.renderer;

import com.vlengine.app.AppContext;
import com.vlengine.image.Texture;
import com.vlengine.renderer.lwjgl.LWJGLTextureRenderer;
import com.vlengine.util.FastList;

/**
 * Class representing a framebuffer into which we can render
 * @author vear (Arpad Vekas)
 */
public class FrameBuffer {
    
    // enum for components of the frame buffer
    public static enum ComponentType {
        Color0(0, Texture.RTT_SOURCE_RGBA),
        Color1(1, Texture.RTT_SOURCE_RGBA),
        Color2(2, Texture.RTT_SOURCE_RGBA),
        Color3(3, Texture.RTT_SOURCE_RGBA),
        Color4(4, Texture.RTT_SOURCE_RGBA),
        Color5(5, Texture.RTT_SOURCE_RGBA),
        Color6(6, Texture.RTT_SOURCE_RGBA),
        Color7(7, Texture.RTT_SOURCE_RGBA),
        Depth(8, Texture.RTT_SOURCE_DEPTH);
        
        public final int id;
        public final int rttType;

        ComponentType(int id, int rttType) {
            this.id=id;
            this.rttType = rttType;
        }
    }
    
    protected AppContext app;

    // the texture renderer if we are rendering to
    // a texture, and not into the backbuffer
    protected TextureRenderer tRenderer;
    
    /**
     * Class represents components of the framebuffer
     */
    protected class FrameBufferComponent {
        // the type of tis component
        protected FrameBuffer.ComponentType  type;
        
        // the texture containing the component
        protected Texture tex;
        
        // is the texture current for this frame
        // or it need to be refreshed
        protected boolean current;
        
    }
    
    // the array holding the components
    protected FrameBufferComponent[] components;
    
    // is this buffer the main framebuffer, or a texture renderer buffer
    protected boolean mainDisplay;
    protected int width;
    protected int height;
    
    // list of textures used when setting up the renderer
    FastList<Texture> textures = new FastList<Texture>();
    
    // is this framebuffer currently active?
    protected boolean active = false;
    
    // is this framebuffer finaly removed from processing stack
    protected boolean removed = true;
    
    public FrameBuffer(AppContext app) {
        this.app = app;
        this.mainDisplay = true;
        components = new FrameBufferComponent[ComponentType.values().length];
        width = app.display.getWidth();
        height = app.display.getHeight();
        
    }

    public FrameBuffer(AppContext app, int width, int height) {
        this.app = app;
        this.mainDisplay = false;
        components = new FrameBufferComponent[ComponentType.values().length];
        this.width = width;
        this.height = height;
    }
    
    public void addComponent(ComponentType componentType) {
        // check if component already exists
        if(components[componentType.id]!=null)
            return;
        FrameBufferComponent component = new FrameBufferComponent();
        component.type = componentType;
        component.current = false;
        components[componentType.id] = component;
    }

    public void setupBuffers() {
        if(mainDisplay) {
            // we are the main display
            // and we dont need other thing to do
            
        } else {
            // create a new texture renderer
            if(tRenderer == null) {
                tRenderer = app.display.createTextureRenderer(width, height, TextureRenderer.RENDER_TEXTURE_2D);
            }
        }
        // setup the render-to textures
        for(int i=0; i<components.length; i++) {
            if(components[i]!=null) {
                // based on type create the texture
                if(components[i].tex==null) {
                    Texture t = new Texture();

                    t.setWrap(Texture.WM_CLAMP_S_CLAMP_T);
                    t.setFilter(Texture.FM_LINEAR);
                    // set the RTT source
                    t.setRTTSource(components[i].type.rttType);
                    LWJGLTextureRenderer.setupRTTTexture(t, width, height);
                    components[i].tex = t;
                }
            }
        }
    }

    public void activate(RenderContext ctx) {
        activate(ctx, false);
    }
            
    public void activate(RenderContext ctx, boolean doClear) {
        if(active) {
            return;
        }

        if(!mainDisplay) {
            // get the list of textures
            textures.clear();
            for(int i=0; i<components.length; i++) {
                if(components[i]!=null) {
                    textures.add(components[i].tex);
                }
            }
            tRenderer.beginRender(ctx, textures, doClear);
        } else {
            // force set FBO 0?
            
        }    
        active = true;
    }

    public void deactivate(RenderContext ctx) {
        if(!active) {
            return;
        }
        if(!mainDisplay) {
            tRenderer.endRender();
        }
        active = false;
    }
            
    public void cleanup() {
        // cleanup the renderer
        if(tRenderer!=null) {
            tRenderer.cleanup();
        }
        // TODO: release textures?
    }
    
    public Texture getComponent(ComponentType compType) {
        if(mainDisplay) {
            // maybe output a warning, that we are doing texture copy?
            FrameBufferComponent comp = components[compType.id];
            if(comp==null) {
                comp = new FrameBufferComponent();
                comp.type = compType;
                // create a new component although it is not declared
                Texture t = new Texture();
                t.setWrap(Texture.WM_CLAMP_S_CLAMP_T);
                t.setFilter(Texture.FM_LINEAR);
                // set the RTT source
                t.setRTTSource(compType.rttType);
                LWJGLTextureRenderer.setupRTTTexture(t, width, height);
                        
                //((TextureRenderer)renderer).setupTexture(t);
                comp.tex = t;
                comp.current = false;
                
                components[compType.id] = comp;
            }
            if(!comp.current) {
                LWJGLTextureRenderer.copyToTexture(comp.tex, width, height);
                comp.current = true;
            }
            return comp.tex;
        } else {
            return components[compType.id].tex;
        }
    }
    
    public void markDirty() {
        // set every texture to dirty
        for(int i=0; i<components.length; i++) {
            if(components[i]!=null) {
                // based on type create the texture
                components[i].current = false;
            }
        }
    }
}
