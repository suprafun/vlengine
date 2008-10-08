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

package com.vlengine.renderer.lwjgl;

import com.vlengine.image.Texture;
import com.vlengine.math.FastMath;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.Renderer;
import com.vlengine.renderer.TextureRenderer;
import com.vlengine.scene.state.lwjgl.LWJGLTextureState;
import com.vlengine.system.DisplaySystem;
import com.vlengine.system.VleException;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.BufferUtils;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.RenderTexture;

/**
 * This class is used by LWJGL to render textures. Users should <b>not </b>
 * create this class directly. Instead, allow DisplaySystem to create it for
 * you.
 * 
 * @author Joshua Slack, Mark Powell
 * @author vear (Arpad Vekas) reworked for VL engine
 * @version
 * @see com.vlengine.system.DisplaySystem#createTextureRenderer
 */
public class LWJGLPbufferTextureRenderer extends TextureRenderer {
    private static final Logger logger = Logger.getLogger(LWJGLPbufferTextureRenderer.class.getName());

    private int pBufferWidth = 16;

    private int pBufferHeight = 16;

    /* Pbuffer instance */
    private Pbuffer pbuffer;

    private int active, caps;

    private boolean useDirectRender = false;

    private RenderTexture texture;

    private boolean headless = false;

    private int bpp, alpha, depth, stencil, samples;
    
    public LWJGLPbufferTextureRenderer(int width, int height,
            LWJGLRenderer parentRenderer, RenderTexture texture) {

        this(width, height, parentRenderer, texture, 
                DisplaySystem.getDisplaySystem().getBitDepth(), 
                DisplaySystem.getDisplaySystem().getMinAlphaBits(), 
                DisplaySystem.getDisplaySystem().getMinDepthBits(), 
                DisplaySystem.getDisplaySystem().getMinStencilBits(), 
                DisplaySystem.getDisplaySystem().getMinSamples());
    }
    
    public LWJGLPbufferTextureRenderer(int width, int height,
            LWJGLRenderer parentRenderer, RenderTexture texture, 
            int bpp,
            int alpha, int depth, int stencil, int samples) {

        this.bpp = bpp;
        this.alpha = alpha;
        this.depth = depth;
        this.stencil = stencil;
        this.samples = samples;

        caps = Pbuffer.getCapabilities();

        if (((caps & Pbuffer.PBUFFER_SUPPORTED) != 0)) {
            isSupported = true;

            // Check if we have non-power of two sizes. If so,
            // find the smallest power of two size that is greater than
            // the provided size.
            width = FastMath.nearestPowerOfTwo(width);
            height = FastMath.nearestPowerOfTwo(height);

            if (width > 0)
                pBufferWidth = width;
            if (height > 0)
                pBufferHeight = height;

            this.texture = texture;
            setMultipleTargets(false);
            validateForCopy();

            if (pBufferWidth != pBufferHeight
                    && (caps & Pbuffer.RENDER_TEXTURE_RECTANGLE_SUPPORTED) == 0) {
                pBufferWidth = pBufferHeight = Math.max(width, height);
            }

            this.parentRenderer = parentRenderer;
            initPbuffer();
        } else {
            isSupported = false;
        }
    }
        
    @Override
    public void setBackgroundColor(ColorRGBA c) {

        // if color is null set background to white.
        super.setBackgroundColor(c);

        if (!isSupported) {
            return;
        }

        activate();
        GL11.glClearColor(backgroundColor.r, backgroundColor.g,
                backgroundColor.b, backgroundColor.a);
        deactivate();
    }
    
    /**
     * <code>setupTexture</code> initializes a new Texture object for use with
     * TextureRenderer. Generates a valid gl texture id for this texture and
     * inits the data type for the texture.
     */
    public void setupTexture(Texture tex) {
        setupTexture(tex, pBufferWidth, pBufferHeight);
    }
    
    /**
     * <code>setupTexture</code> initializes a new Texture object for use with
     * TextureRenderer. Generates a valid gl texture id for this texture and
     * inits the data type for the texture.
     */
    public void setupTexture(Texture tex, int width, int height) {
        if (!isSupported) {
            return;
        }

        IntBuffer ibuf = BufferUtils.createIntBuffer(1);

        if (tex.getTextureId() != 0) {
            ibuf.put(tex.getTextureId());
            ibuf.rewind();
            GL11.glDeleteTextures(ibuf);
            ibuf.clear();
        }

        // Create the texture
        GL11.glGenTextures(ibuf);
        tex.setTextureId(ibuf.get(0));
        //TextureManager.registerForCleanup(tex.getTextureKey(), tex.getTextureId());
        LWJGLTextureState.doTextureBind(tex.getTextureId(), 0);

        int source = GL11.GL_RGBA;
        switch (tex.getRTTSource()) {
            case Texture.RTT_SOURCE_RGBA: break;
            case Texture.RTT_SOURCE_RGB: source = GL11.GL_RGB; break;
            case Texture.RTT_SOURCE_ALPHA: source = GL11.GL_ALPHA; break;
            case Texture.RTT_SOURCE_DEPTH: source = GL11.GL_DEPTH_COMPONENT; break;
            case Texture.RTT_SOURCE_INTENSITY: source = GL11.GL_INTENSITY; break;
            case Texture.RTT_SOURCE_LUMINANCE: source = GL11.GL_LUMINANCE; break;
            case Texture.RTT_SOURCE_LUMINANCE_ALPHA: source = GL11.GL_LUMINANCE_ALPHA; break;
        }
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, source, 0, 0, width, height, 0);
        logger.info("setup tex" + tex.getTextureId() + ": " + width + ","
                + height);
    }
    
    public void beginRender(RenderContext ctx, Texture tex, boolean doClear) {
        if (!isSupported) {
            return;
        }
        if(this.ctx != null)
            throw new VleException("Trying to use TextureRenderer while not finished prevois render");
        this.ctx = ctx;
        ctx.renderTextures.add(tex);

        // clear the current states since we are renderering into a new location
        // and can not rely on states still being set.
        try {
            if (pbuffer.isBufferLost()) {
                logger
                        .warning("PBuffer contents lost - will recreate the buffer");
                deactivate();
                pbuffer.destroy();
                initPbuffer();
            }

            if (useDirectRender
                    && tex.getRTTSource() != Texture.RTT_SOURCE_DEPTH) {
                // setup and render directly to a 2d texture.
                pbuffer.releaseTexImage(Pbuffer.FRONT_LEFT_BUFFER);
            }
            activate();
            switchCameraIn(doClear);
                
        } catch (Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(),
                    "beginRender", "Exception", e);
        }
    }
    
    public void beginRender(RenderContext ctx, FastList<Texture> texs, boolean doClear) {
        if (!isSupported) {
            return;
        }
        if(this.ctx != null)
            throw new VleException("Trying to use TextureRenderer while not finished prevois render");
        this.ctx = ctx;
        ctx.renderTextures.addAll(texs);

        // clear the current states since we are renderering into a new location
        // and can not rely on states still being set.
        try {
            if (pbuffer.isBufferLost()) {
                logger
                        .warning("PBuffer contents lost - will recreate the buffer");
                deactivate();
                pbuffer.destroy();
                initPbuffer();
            }

            if (texs.size() == 1 && useDirectRender
                    && texs.get(0).getRTTSource() != Texture.RTT_SOURCE_DEPTH) {
                pbuffer.releaseTexImage(Pbuffer.FRONT_LEFT_BUFFER);
            }
            
            activate();
            switchCameraIn(doClear);

        } catch (Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(),
                    "render(Spatial, Texture)", "Exception", e);
        }
    }
    
    public void endRender() {
        if (!isSupported) {
            return;
        }
        FastList<Texture> texs = ctx.renderTextures;

        // clear the current states since we are renderering into a new location
        // and can not rely on states still being set.
        try {
            switchCameraOut();
            if (texs.size() == 1 && useDirectRender
                    && texs.get(0).getRTTSource() != Texture.RTT_SOURCE_DEPTH) {
                deactivate();
                pbuffer.bindTexImage(Pbuffer.FRONT_LEFT_BUFFER);
            } else {
                for (int i = 0; i < texs.size(); i++) {
                    copyToTexture(texs.get(i), pBufferWidth, pBufferHeight);
                }
                deactivate();
            }
            texs.clear();
        } catch (Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(),
                    "render(Spatial, Texture)", "Exception", e);
        }
        // clear the rendercontext
        this.ctx = null;
    }
    
    /**
     * <code>copyToTexture</code> copies the pbuffer contents to
     * the given Texture. What is copied is up to the Texture object's rttSource
     * field.
     * 
     * @param tex
     *            The Texture to copy into.
     * @param width
     *            the width of the texture image
     * @param height
     *            the height of the texture image
     */
    public void copyToTexture(Texture tex, int width, int height) {
        LWJGLTextureState.doTextureBind(tex.getTextureId(), 0);

        int source = GL11.GL_RGBA;
        switch (tex.getRTTSource()) {
            case Texture.RTT_SOURCE_RGBA: break;
            case Texture.RTT_SOURCE_RGB: source = GL11.GL_RGB; break;
            case Texture.RTT_SOURCE_ALPHA: source = GL11.GL_ALPHA; break;
            case Texture.RTT_SOURCE_DEPTH: source = GL11.GL_DEPTH_COMPONENT; break;
            case Texture.RTT_SOURCE_INTENSITY: source = GL11.GL_INTENSITY; break;
            case Texture.RTT_SOURCE_LUMINANCE: source = GL11.GL_LUMINANCE; break;
            case Texture.RTT_SOURCE_LUMINANCE_ALPHA: source = GL11.GL_LUMINANCE_ALPHA; break;
        }
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, source, 0, 0, width, height, 0);
    }
    
    private void initPbuffer() {
        if (!isSupported) {
            return;
        }

        try {
            if (pbuffer != null) {
                giveBackContext();
            }
            pbuffer = new Pbuffer(pBufferWidth, pBufferHeight, new PixelFormat(
                    bpp, alpha, depth, stencil, samples), texture, null);
        } catch (Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "initPbuffer()", "Exception", e);

            if (texture != null && useDirectRender) {
                logger.warning("LWJGL reports this card supports Render to Texture"
                              + ", but fails to enact it.  Please report this to the LWJGL team.");
                logger.warning("Attempting to fall back to Copy Texture.");
                texture = null;
                useDirectRender = false;
                initPbuffer();
                return;
            }

            logger.log(Level.WARNING, "Failed to create Pbuffer.", e);
            isSupported = false;
            return;            
        }

        try {
            activate();

            pBufferWidth = pbuffer.getWidth();
            pBufferHeight = pbuffer.getHeight();

            GL11.glClearColor(backgroundColor.r, backgroundColor.g,
                    backgroundColor.b, backgroundColor.a);

            if (camera == null)
                initCamera();
            camera.update();

            deactivate();
		} catch( Exception e ) {
			logger.log(Level.WARNING, "Failed to initialize created Pbuffer.",
                    e);
			isSupported = false;
			return;
		}
	}

    public void activate() {
        if (!isSupported) {
            return;
        }
        if (active == 0) {
            try {
                pbuffer.makeCurrent();
            } catch (LWJGLException e) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "activate()", "Exception",
                        e);
                throw new VleException();
            }
        }
        active++;
    }

    public void deactivate() {
        if (!isSupported) {
            return;
        }
        if (active == 1) {
            try {
                if (!useDirectRender)
                    
                giveBackContext();
                parentRenderer.reset();
            } catch (LWJGLException e) {
                logger.logp(Level.SEVERE, this.getClass().toString(),
                        "deactivate()", "Exception", e);
                throw new VleException();
            }
        }
        active--;
    }
    
    private void giveBackContext() throws LWJGLException {
        if (!headless && Display.isCreated()) {
            Display.makeCurrent();
        }
    }
    
    private void initCamera() {
        if (!isSupported) {
            return;
        }
        camera = new LWJGLCamera(pBufferWidth, pBufferHeight, this);
        camera.setFrustum(1.0f, 1000.0f, -0.50f, 0.50f, 0.50f, -0.50f);
        Vector3f loc = new Vector3f(0.0f, 0.0f, 0.0f);
        Vector3f left = new Vector3f(-1.0f, 0.0f, 0.0f);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f dir = new Vector3f(0.0f, 0f, -1.0f);
        camera.setFrame(loc, left, up, dir);
    }

    @Override
    public void cleanup() {
        if (!isSupported) {
            return;
        }

        pbuffer.destroy();
    }
    
    private void validateForCopy() {
        if (pBufferWidth > DisplaySystem.getDisplaySystem().getWidth()) {
            pBufferWidth = DisplaySystem.getDisplaySystem().getWidth();
        }

        if (pBufferHeight > DisplaySystem.getDisplaySystem().getHeight()) {
            pBufferHeight = DisplaySystem.getDisplaySystem().getHeight();
        }
    }

    public void setMultipleTargets(boolean force) {
        if (force) {
            useDirectRender = false;
			validateForCopy();
            initPbuffer();
        } else {
            if ((caps & Pbuffer.RENDER_TEXTURE_SUPPORTED) != 0) {
                logger.info("Render to Texture Pbuffer supported!");
                if (texture == null) {
                    logger.info("No RenderTexture used in init, falling back to Copy Texture PBuffer.");
                    useDirectRender = false;
					validateForCopy();
                } else {
                    useDirectRender = true;
                }
            } else {
                logger.info("Copy Texture Pbuffer supported!");
                texture = null;
            }
        }
    }
}
