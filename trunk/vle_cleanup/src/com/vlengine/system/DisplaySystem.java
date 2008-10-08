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

package com.vlengine.system;

import com.vlengine.awt.VLECanvas;
import com.vlengine.renderer.lwjgl.LWJGLRenderer;
import com.vlengine.renderer.Renderer;
import com.vlengine.renderer.TextureRenderer;
import com.vlengine.renderer.lwjgl.LWJGLPbufferTextureRenderer;
import com.vlengine.renderer.lwjgl.LWJGLTextureRenderer;
import com.vlengine.util.Timer;
import com.vlengine.util.lwjgl.LWJGLTimer;
import java.awt.Toolkit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ARBMultisample;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.RenderTexture;
import org.lwjgl.util.applet.LWJGLInstaller;

/**
 * <code>DisplaySystem</code> uses the LWJGL API for window creation and
 * rendering via OpenGL. <code>LWJGLRenderer</code> is also created that gives
 * a way of displaying data to the created window.
 *
 * @author Mark Powell
 * @author Gregg Patton
 * @author Joshua Slack - Optimizations, Headless rendering, RenderContexts, AWT integration
 * @author vear (Arpad Vekas) - Reworked for VL Engine, merged with LWJGLDisplaySystem
 */
public class DisplaySystem {

    private static final Logger logger = Logger.getLogger(DisplaySystem.class.getName());
    
    /** The display system that has been created. */
    protected static DisplaySystem system;
    
    // VTTE addon, server mode, no display
    private boolean servermode = false;
    
    /**
     * Width selected for the renderer.
     */
    protected int width;

    /**
     * height selected for the renderer.
     */
    protected int height;

    /**
     * Bit depth selected for renderer.
     */
    protected int bpp;

    /**
     * Frequency selected for renderer.
     */
    protected int frq;

    /**
     * Is the display full screen?
     */
    protected boolean fs;

    /**
     * Is the display created already?
     */
    protected boolean created;

    /**
     * Alpha bits to use for the renderer.
     */
    protected int alphaBits = 0;

    /**
     * Depth bits to use for the renderer.
     */
    protected int depthBits = 8;

    /**
     * Stencil bits to use for the renderer.
     */
    protected int stencilBits = 0;

    /**
     * Number of samples to use for the multisample buffer.
     */
    protected int samples = 0;

    /**
     * Gamma value of display - default is 1.0f. 0->infinity
     */
    protected float gamma = 1.0f;

    /**
     * Brightness value of display - default is 0f. -1.0 -> 1.0
     */
    protected float brightness = 0;

    /**
     * Copntract value of display - default is 1.0f. 0->infinity
     */
    protected float contrast = 1;
    
    protected Renderer renderer;
    private VLECanvas canvas;
    
    private Timer timer;
    
    protected Object oglContext;
    
    protected Thread oglThread;

    public static DisplaySystem getDisplaySystem() {

        // force to initialize joystick input before display system as there are
        // lwjgl issues with creating it afterwards
        //JoystickInput.get();

        if (system == null) {
            system = new DisplaySystem();
        }

        return system;
    }

    public void close() {
        if(!servermode)
            Display.destroy();
    }

    public void createWindow( int w, int h, int bpp, int frq, boolean fs ) throws VleException {
        // VTTE: do nothing in server mode
        if(servermode) {
            renderer = new Renderer();
            renderer.setHeadless(true);
        } else {
        
            // confirm that the parameters are valid.
            if ( w <= 0 || h <= 0 ) {
                throw new VleException( "Invalid resolution values: " + w + " " + h );
            }
            else if ( ( bpp != 32 ) && ( bpp != 16 ) && ( bpp != 24 ) ) {
                throw new VleException( "Invalid pixel depth: " + bpp );
            }

            // set the window attributes
            this.width = w;
            this.height = h;
            this.bpp = bpp;
            this.frq = frq;
            this.fs = fs;

            initDisplay();
            // create a renderer matching the capabilities of current hardware
            renderer = LWJGLRenderer.createMatchingRenderer( width, height );
        }
        created = true;
    }

    public void initForCanvas(int width, int height) {
        renderer = new LWJGLRenderer(width, height);
        renderer.setHeadless(true);
        this.width = width;
        this.height = height;
        created = true;
    }
    
    /**
     * Returns the Pbuffer used for headless display or null if not headless.
     *
     * @return Pbuffer
     */
    public VLECanvas getCurrentCanvas() {
        return canvas;
    }

    public void setCurrentCanvas(VLECanvas canvas) {
        this.canvas = canvas;
        oglContext = canvas.getContext();
    }

    public Object getOGLContext() {
        return oglContext;
    }
    
    /**
     * <code>createTextureRenderer</code> builds the renderer used to render
     * to a texture.
     */
    public TextureRenderer createTextureRenderer( int width, int height, int target) {
        if ( !isCreated() ) {
            return null;
        }

        TextureRenderer textureRenderer = new LWJGLTextureRenderer( width, height,
                (LWJGLRenderer) getRenderer());

        if (!textureRenderer.isSupported()) {
            textureRenderer = null;

            if ( target == TextureRenderer.RENDER_TEXTURE_1D ) {
                target = RenderTexture.RENDER_TEXTURE_1D;
            }
            else if ( target == TextureRenderer.RENDER_TEXTURE_2D ) {
                target = RenderTexture.RENDER_TEXTURE_2D;
            }
            else if ( target == TextureRenderer.RENDER_TEXTURE_CUBE_MAP ) {
                target = RenderTexture.RENDER_TEXTURE_CUBE_MAP;
            }
            else if ( target == TextureRenderer.RENDER_TEXTURE_RECTANGLE ) {
                target = RenderTexture.RENDER_TEXTURE_RECTANGLE;
            }

            //boolean useRGB, boolean useRGBA, boolean useDepth, boolean isRectangle, int target, int mipmaps
            RenderTexture renderTexture = new RenderTexture(false, true, true, false, target, 0);
            
            textureRenderer = new LWJGLPbufferTextureRenderer( width, height, 
                    (LWJGLRenderer) getRenderer(), renderTexture);
        }
        
        return textureRenderer;
    }

    /**
     * <code>setTitle</code> sets the window title of the created window.
     *
     * @param title the title.
     */
    public void setTitle( String title ) {
        if(!servermode)
            Display.setTitle( title );
    }
    
    private void initDisplay() {
        // create the Display.
        DisplayMode mode = selectMode();
        PixelFormat format = getFormat();
        if ( null == mode ) {
            throw new VleException( "Bad display mode" );
        }

        try {
            Display.setDisplayMode( mode );
            Display.setFullscreen( fs );
            if ( !fs ) {
                int x, y;
                x = ( Toolkit.getDefaultToolkit().getScreenSize().width - width ) >> 1;
                y = ( Toolkit.getDefaultToolkit().getScreenSize().height - height ) >> 1;
                Display.setLocation( x, y );
            }
            Display.create( format );
            
            if (samples != 0 && GLContext.getCapabilities().GL_ARB_multisample) {
                GL11.glEnable(ARBMultisample.GL_MULTISAMPLE_ARB);
            }
            
            // kludge added here... LWJGL does not properly clear their
            // keyboard and mouse buffers when you call the destroy method,
            // so if you run two jme programs in the same jvm back to back
            // the second one will pick up the esc key used to exit out of
            // the first.
            Keyboard.poll();
            Mouse.poll();
            
            // save the OpenGL context, so that other threads can access it
            oglContext = Display.getDrawable().getContext();
            
            // mark this thread as having the ogl context
            makeCurrent();
        } catch ( Exception e ) {
            // System.exit(1);
            logger.severe("Cannot create window");
            logger.logp(Level.SEVERE, this.getClass().toString(), "initDisplay()", "Exception", e);
            throw new VleException( "Cannot create window: " + e.getMessage() );
        }
    }
    
    private DisplayMode selectMode() {
        DisplayMode mode;
        if ( fs ) {
            mode = getValidDisplayMode( width, height, bpp, frq );
            if ( null == mode ) {
                throw new VleException( "Bad display mode" );
            }
        }
        else {
            mode = new DisplayMode( width, height );
        }
        return mode;
    }
    
    public PixelFormat getFormat() {
        return new PixelFormat( bpp, alphaBits, depthBits,
                stencilBits, samples );
    }
        
    public boolean isClosing() {
        if ( !servermode && Display.isCreated()) {
            return Display.isCloseRequested();
        }
       
        return false;
    }

    public void setBits(int depthBits, int stencilBits, int alphaBits, int samples) {       
        this.alphaBits = alphaBits;
        this.depthBits = depthBits;
        this.stencilBits = stencilBits;
        this.samples = samples;
    }
    
    private DisplayMode getValidDisplayMode( int width, int height, int bpp,
                                             int freq ) {
        // get all the modes, and find one that matches our width, height, bpp.
        DisplayMode[] modes;
        try {
            modes = Display.getAvailableDisplayModes();
        } catch ( LWJGLException e ) {
            logger.logp(Level.SEVERE, this.getClass().toString(),
                    "getValidDisplayMode(width, height, bpp, freq)", "Exception", e);
            return null;
        }
        
        // Try to find a best match.
        int best_match = -1; // looking for request size/bpp followed by exact or highest freq
        int match_freq = -1;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].getWidth() != width) {
                logger.fine("DisplayMode " + modes[i] + ": Width != " + width);
                continue;
            }
            if (modes[i].getHeight() != height) {
                logger.fine("DisplayMode " + modes[i] + ": Height != "
                                + height);
                continue;
            }
            if (modes[i].getBitsPerPixel() != bpp) {
                logger.fine("DisplayMode " + modes[i] + ": Bits per pixel != "
                        + bpp);
                continue;
            }
            if (best_match == -1) {
                logger.fine("DisplayMode " + modes[i] + ": Match! ");
                best_match = i;
                match_freq = modes[i].getFrequency();
            } else {
                int cur_freq = modes[i].getFrequency();
                if( match_freq!=freq &&          // Previous is not a perfect match
                    ( cur_freq == freq ||        // Current is perfect match
                      match_freq < cur_freq ) )  //      or is higher freq
                {
                    logger.fine("DisplayMode " + modes[i] + ": Better match!");
                    best_match = i;
                    match_freq = cur_freq;
                }
            }
        }

        if (best_match == -1)
            return null; // none found;
        else {
            logger.info("Selected DisplayMode: " + modes[best_match]);
            return modes[best_match];
        }
    }
    
    public boolean isValidDisplayMode( int width, int height, int bpp, int freq ) {
        return getValidDisplayMode( width, height, bpp, freq ) != null;
    }
    
    public Renderer getRenderer() {
        return renderer;
    }
    
    public boolean isCreated() {
        return this.created;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    /**
     * Sets a new width for the display system
     * @param width
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Sets a new height for the display system
     * @param height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Returns the set bitdepth for the display system.
     * 
     * @return the set bit depth
     */
    public int getBitDepth() {
        return bpp;
    }

    /**
     * Returns the set frequency for the display system.
     * 
     * @return the set frequency
     */
    public int getFrequency() {
        return frq;
    }

    /**
     * Returns whether or not the display system is set to be full screen.
     * 
     * @return true if full screen
     */
    public boolean isFullScreen() {
        return fs;
    }

    /**
     * Returns the minimum bits per pixel in the alpha buffer.
     * 
     * @return the int value of alphaBits.
     */
    public int getMinAlphaBits() {
        return alphaBits;
    }

    /**
     * Sets the minimum bits per pixel in the alpha buffer.
     * 
     * @param alphaBits -
     *            the new value for alphaBits
     */
    public void setMinAlphaBits(int alphaBits) {
        this.alphaBits = alphaBits;
    }

    /**
     * Returns the minimum bits per pixel in the depth buffer.
     * 
     * @return the int value of depthBits.
     */
    public int getMinDepthBits() {
        return depthBits;
    }

    /**
     * Sets the minimum bits per pixel in the depth buffer.
     * 
     * @param depthBits -
     *            the new value for depthBits
     */
    public void setMinDepthBits(int depthBits) {
        this.depthBits = depthBits;
    }

    /**
     * Returns the minimum bits per pixel in the stencil buffer.
     * 
     * @return the int value of stencilBits.
     */
    public int getMinStencilBits() {
        return stencilBits;
    }

    /**
     * Sets the minimum bits per pixel in the stencil buffer.
     * 
     * @param stencilBits -
     *            the new value for stencilBits
     */
    public void setMinStencilBits(int stencilBits) {
        this.stencilBits = stencilBits;
    }

    /**
     * Returns the minimum samples in multisample buffer.
     * 
     * @return the int value of samples.
     */
    public int getMinSamples() {
        return samples;
    }

    /**
     * Sets the minimum samples in the multisample buffer.
     * 
     * @param samples -
     *            the new value for samples
     */
    public void setMinSamples(int samples) {
        this.samples = samples;
    }

    /**
     * Returns the brightness last requested by this display.
     * 
     * @return brightness - should be between -1 and 1.
     */
    public float getBrightness() {
        return brightness;
    }

    /**
     * Note: This affects the whole screen, not just the game window.
     * 
     * @param brightness
     *            The brightness to set (set -1 to 1) default is 0
     */
    public void setBrightness(float brightness) {
        this.brightness = brightness;
        updateDisplayBGC();
    }

    /**
     * @return Returns the contrast.
     */
    public float getContrast() {
        return contrast;
    }

    /**
     * Note: This affects the whole screen, not just the game window.
     * 
     * @param contrast
     *            The contrast to set (set greater than 0) default is 1
     */
    public void setContrast(float contrast) {
        this.contrast = contrast;
        updateDisplayBGC();
    }

    /**
     * @return Returns the gamma.
     */
    public float getGamma() {
        return gamma;
    }

    /**
     * Note: This affects the whole screen, not just the game window.
     * 
     * @param gamma
     *            The gamma to set (default is 1)
     */
    public void setGamma(float gamma) {
        this.gamma = gamma;
        updateDisplayBGC();
    }

    /**
     * Sets all three in one call. <p/> Note: This affects the whole screen, not
     * just the game window.
     * 
     * @param brightness
     * @param gamma
     * @param contrast
     */
    public void setBrightnessGammaContrast(float brightness, float gamma,
            float contrast) {
        this.brightness = brightness;
        this.gamma = gamma;
        this.contrast = contrast;
        updateDisplayBGC();
    }

    /**
     * Update the display's gamma, brightness and contrast based on the set values.
     */
    protected void updateDisplayBGC() {
        try {
            Display.setDisplayConfiguration( gamma, brightness, contrast );
        } catch ( LWJGLException e ) {
            logger
                    .warning("Unable to apply gamma/brightness/contrast settings: "
                            + e.getMessage());
        }
    }

    public String getDisplaySignature() {
        String[] sig = getDisplaySignatures();
        if( sig == null ) 
            return null;
        String fsig=null;
        for(int i=0; i< sig.length; i++ ) {
            if( fsig == null ) {
                fsig = sig[i];
            } else {
                fsig += "," + sig[i];
            }
        }
        return fsig;
    }
            
    public String[] getDisplaySignatures() {
        String[] sig;
        if(servermode) {
            sig=new String[1];
            sig[1]= "null";
            return sig;
        }
        sig=new String[5];
        sig[0] = Display.getAdapter();
        sig[1] = Display.getVersion();
        try {
            sig[2] = GL11.glGetString(GL11.GL_VENDOR);
        } catch (Exception e) {
            sig[2] = "n/a";
        }
        try {
            sig[3] = GL11.glGetString(GL11.GL_RENDERER);
        } catch (Exception e) {
            sig[3] = "n/a";
        }
        try {
            sig[4] = GL11.glGetString(GL11.GL_VERSION);
        } catch (Exception e) {
            sig[4] = "n/a";
        }
        return sig;
    }
    
    public Timer getTimer() {
        if (timer == null) {
        	timer = new LWJGLTimer();
        }
        return timer;
    }
    
    public void installLibs() {
        try {
            LWJGLInstaller.tempInstall();
        } catch (Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "installLibs()", "Exception",
                            e);
            throw new VleException("Could not install lwjgl libs! "+e);
        }
    }
    
    public void makeCurrent() {
        if(canvas==null && renderer != null && !renderer.isHeadless()) {
            try {
                Display.makeCurrent();
            } catch (LWJGLException ex) {

            }
        }
        oglThread = Thread.currentThread();
    }
    
    public boolean isOglThread() {
        return oglThread == Thread.currentThread();
    }
}
