/*
 * Copyright (c) 2003-2007 jMonkeyEngine, 2008 VL Engine
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
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
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

import com.vlengine.image.Texture;
import com.vlengine.renderer.lwjgl.LWJGLRenderer;
import com.vlengine.util.FastList;

/**
 *
 * @author vear (Arpad Vekas)
 */
public abstract class TextureRenderer {
    /**
     * defines a constant for usage of a one dimensional texture.
     */
    public static final int RENDER_TEXTURE_1D = 1;

    /**
     * defines a constant for usage of a two dimensional texture.
     */
    public static final int RENDER_TEXTURE_2D = 2;

    /**
     * defines a constant for usage of a rectangular texture.
     */
    public static final int RENDER_TEXTURE_RECTANGLE = 3;

    /**
     * defines a constant for usage of a cubic texture.
     */
    public static final int RENDER_TEXTURE_CUBE_MAP = 4;
    
    protected boolean isSupported = true;

    protected LWJGLRenderer parentRenderer;
    protected int width, height;
    protected ColorRGBA backgroundColor = ColorRGBA.black.clone();

    protected int active = 0;
    protected ViewCamera camera;
    protected RenderContext ctx;
    
    /**
     * 
     * <code>isSupported</code> obtains the capability of the graphics card.
     * If the graphics card does not have pbuffer support, false is returned,
     * otherwise, true is returned. TextureRenderer will not process any scene
     * elements if pbuffer is not supported.
     * 
     * @return if this graphics card supports pbuffers or not.
     */
    public boolean isSupported() {
        return isSupported;
    }
    
    /**
     * <code>setupTexture</code> initializes a Texture object for use with
     * TextureRenderer. Generates a valid gl texture id for this texture and
     * sets up data storage for it.  The texture will be equal to the pbuffer size.
     * 
     * Note that the pbuffer size is not necessarily what is specified in the constructor.
     * 
     * @param tex
     *            The texture to setup for use in Texture Rendering.
     */
    public abstract void setupTexture(Texture tex);
    
    public abstract void beginRender(RenderContext ctx, Texture tex, boolean doClear);
    
    public abstract void beginRender(RenderContext ctx, FastList<Texture> texs, boolean doClear);

    public abstract void endRender();

    private ViewCamera oldCamera;
    private int oldWidth, oldHeight;

    public void switchCameraIn(boolean doClear) {
        // grab non-rtt settings
        if(oldCamera==null) {
            oldCamera = new ViewCamera();
        }
        parentRenderer.getCamera().copy(oldCamera);
        oldWidth = parentRenderer.getWidth();
        oldHeight = parentRenderer.getHeight();
        parentRenderer.setCamera(camera);

        // swap to rtt settings
        parentRenderer.resize(width, height);

        // clear the scene
        if (doClear) {
            parentRenderer.clearBuffers();
        }
    }

    public void switchCameraOut() {
        parentRenderer.setCamera(oldCamera);
        parentRenderer.resize(oldWidth, oldHeight);
    }

    public LWJGLRenderer getParentRenderer() {
        return parentRenderer;
    }

    /**
     * Set up this textureRenderer for use with multiple targets. If you are
     * going to use this texture renderer to render to more than one texture,
     * call this with true.
     * 
     * @param multi
     *            true if you plan to use this texture renderer to render
     *            different content to more than one texture.
     */
    public abstract void setMultipleTargets(boolean multi);
    
    public void cleanup() {
        
    }
    
    public void setBackgroundColor(ColorRGBA c) {
        backgroundColor.set(c);
    }
}
