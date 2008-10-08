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

package com.vlengine.awt;

import com.vlengine.app.AppContext;
import com.vlengine.app.frame.Frame;
import com.vlengine.input.InputSystem;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.Renderer;
import com.vlengine.system.DisplaySystem;
import com.vlengine.system.VleException;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ARBMultisample;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.PixelFormat;

/**
 * @author Joshua Slack
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class VLECanvas extends AWTGLCanvas {

    private static final Logger logger = Logger.getLogger(VLECanvas.class
            .getName());

    private static final long serialVersionUID = 1L;

    protected boolean setup = false;
    
    protected CanvasMainGame engine;
    protected AppContext app;
    protected Renderer renderer;

    // should we resize during next render
    protected boolean resize = false;
    protected int width;
    protected int height;
    protected boolean updateinput = false;
    
    protected boolean locked = false;
    protected boolean waslocked = true;
    
    private static final String PAINT_LOCK = "INIT_LOCK";

    public VLECanvas(int width, int height) throws LWJGLException {
        super(generatePixelFormat());
        this.width = width;
        this.height = height;
        resize = true;
    }

    public void setMainGame(CanvasMainGame maingame) {
        engine = maingame;
    }

    private static PixelFormat generatePixelFormat() {
        return DisplaySystem.getDisplaySystem().getFormat();
    }

    public void setVSync(boolean sync) {
        setVSyncEnabled(sync);
    }

    @Override
    public void paintGL() {
        synchronized (PAINT_LOCK) {
            try {
                // if locked, skip repainting this frame
                if(locked)
                    return;
                if(waslocked) {
                    try {
                        DisplaySystem.getDisplaySystem().makeCurrent();
                        this.makeCurrent();
                    } catch (Exception ex) {
                    }
                    
                    try {
                        //GLContext.useContext(getContext());
                        GLContext.getCapabilities();
                      } catch (Exception ex) {
                      }
                    waslocked = false;
                }
                /*
                try {
                    this.makeCurrent();
                } catch (Exception ex) {
                    
                }
                 */
                DisplaySystem.getDisplaySystem().setCurrentCanvas(this);
                if (!setup) {
                    doSetup();
                }
                if(resize ) {
                   //renderer.resize(width, height);
                   app.display.setWidth(width);
                   app.display.setHeight(height);
                   app.cam.resize(width, height);
                   app.cam.update();
                   this.setBounds(0, 0, width, height);
                   resize = false;
                }
                app.updateInput = updateinput;
                // make the engine update and render a frame
                engine.gameLoop();
                swapBuffers();
            } catch (LWJGLException e) {
                logger.log(Level.SEVERE, "Exception in paintGL()", e);
            }
            setup = true;
        }
    }

    protected void doSetup() {
        engine.setCanvas(this);
        engine.setSize(width, height);
        engine.doSetup();
        app = engine.getAppContext();
        renderer = app.display.getRenderer();
        if (app.display.getMinSamples() != 0 && GLContext.getCapabilities().GL_ARB_multisample) {
            GL11.glEnable(ARBMultisample.GL_MULTISAMPLE_ARB);
        }
    }
    
    protected ColorRGBA makeColorRGBA(Color color) {
        return new ColorRGBA(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, color.getAlpha() / 255f);
    }

    /* (non-Javadoc)
     * @see com.jmex.awt.JMECanvas#doUpdateInput()
     */
    public boolean doUpdateInput() {
        return updateinput;
    }

    /* (non-Javadoc)
     * @see com.jmex.awt.JMECanvas#setUpdateInput(boolean)
     */
    public void setUpdateInput(boolean doUpdate) {
        updateinput = doUpdate;
    }

    public boolean isSetup() {
        return setup;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void resizeCanvas(int width, int height) {
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;
        this.height = height;
        this.width = width;
        this.resize = true;
    }

    public void resizeCanvas() {
        resizeCanvas(this.getParent().getWidth(), this.getParent().getHeight());
    }
    
    @Override
    public void setBackground(Color bgColor) {
        lockEngine();
        setBackground(makeColorRGBA(bgColor));
        unlockEngine();
    }

    public void setBackground(ColorRGBA colorRGBA) {
        renderer.setBackgroundColor(colorRGBA);
    }
    
    public AppContext getAppContext() {
        return app;
    }
    
    public void cleanup() {
        lockEngine();
        app.finished = true;
        engine.cleanup();
        unlockEngine();
    }
    
    public AppContext lockEngine() {
        // pause until engine fully loaded
        if(!setup) {
            logger.log(Level.SEVERE, "Tryed to lock engine before it is set up");
            //throw new VleException("Tryed to lock engine before it is set up");
        }
        while(!setup) {
            try {
                Thread.sleep(100);
                Thread.yield();
            } catch (InterruptedException ex) {
            }
        }
        return lockEngineDirect();
    }
    
    public AppContext lockEngineDirect() {
        synchronized(PAINT_LOCK) {
            locked = true;
            //this.setVisible(false);
            /*
            try {
                this.releaseContext();
                //GLContext.useContext(app.display.getOGLContext());
                //GLContext.getCapabilities();
              } catch (Exception ex) {
              }
              try {
                this.makeCurrent();
              } catch (Exception ex) {
              }
             */
        }
        return app;
    }

    public void unlockEngine() {
        /*
        try {
            this.releaseContext();
        } catch (LWJGLException ex) {
        }
         */
        //this.setVisible(true);
        locked = false;
    }
}
