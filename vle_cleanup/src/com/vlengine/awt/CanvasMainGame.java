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

package com.vlengine.awt;

import com.vlengine.app.MainGame;
import com.vlengine.input.InputHandler;
import com.vlengine.scene.CameraNode;
import com.vlengine.scene.control.KeyboardMoveController;
import com.vlengine.scene.control.MouseLookController;
import com.vlengine.system.DisplaySystem;
import com.vlengine.system.PropertiesIO;
import java.util.logging.Level;



/**
 * Main class when the engine is used inside an AWT application
 * and rendering is done into an AWT canvas
 * 
 * @author vear (Arpad Vekas)
 */
public abstract class CanvasMainGame extends MainGame {
    
    protected VLECanvas canvas;
    protected int width, height;
    // should mouse be set-up for right-mouse button drag only mode
    protected boolean dragOnly = false;
    
    public CanvasMainGame() {
        initApp();
    }
    
    public void setCanvas(VLECanvas canv) {
        canvas = canv;
    }
    
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public void setMouseLookDragOnly(boolean dragOnly) {
        this.dragOnly = dragOnly;
    }
    
    @Override
    protected void getAttributes() {
        if(app.properties==null) {
            app.properties = new PropertiesIO("properties.cfg");
            boolean loaded = app.properties.load(app.conf);

            // do not show settings dialog
            if (!loaded) {
                app.properties.save(app.conf);
            }
        }
    }

    @Override
    protected void initInput() {
        // by default initialize LWJGL input systems
        // this method should be overriden in canvas application
        AWTKeyInput ki = new AWTKeyInput();
        ki.setup(canvas);
        app.keyInput = ki;
        
        AWTMouseInput mi = new AWTMouseInput();
        // TODO: maybe drag-only input should be an option?
        mi.setup(canvas, false);
        //mi.setRelativeDelta(canvas);
        app.mouseInput = mi;
    }

    protected MouseLookController mlc;
    protected KeyboardMoveController kmc;
    
    @Override
    protected void initKeybindings() {
                // set up camera node
        app.camn = new CameraNode("Camera Node");
        app.camn.setCamera(app.cam);
        
        // create mouse look controller
        mlc = new MouseLookController(app.mouseInput);
        mlc.setInputSystem(app.inputSystem);
        mlc.setUpCommands();
        mlc.setControlledNode(app.camn);
        mlc.setSpeed(2);
        mlc.setXSpeed(4);
        mlc.setYSpeed(2);
        mlc.setDragOnly(dragOnly);
        mlc.setActive(true);
        app.camn.addController(mlc);
        
        // create keyboard move controller
        kmc = new KeyboardMoveController();
        kmc.setInputSystem(app.inputSystem);
        kmc.setAcceleration(10);
        kmc.setUpCommands();
        kmc.setRotationSource(app.camn);
        kmc.setControlledNode(app.camn);
        kmc.setSpeed(50);
        kmc.setActive(true);
        app.camn.addController(kmc);
        
        app.rootNode.attachChild(app.camn);

        // create handler for basic commands
        app.input = new InputHandler(app);
        //app.input.setMainGame(this);
        app.input.setInputSystem(app.inputSystem);
        app.input.setUpBasicCommands();
        // enable debugging commands also
        app.input.setUpDefaultDebugCommands();
    }
    
    @Override
    protected void initDisplay() {
        app.display = DisplaySystem.getDisplaySystem( );
        app.display.initForCanvas(width, height);
        // mark this as the ogl thread
        app.display.makeCurrent();
        app.cam = app.display.getRenderer().createCamera( app.display.getWidth(),
                app.display.getHeight() );
    }

    public void doSetup() {

        logger.info( "Application started.");
        try {
            
            getAttributes();
            if (!app.finished) {
                initSystem();
                assertDisplayCreated();
                initGame();
                initRenderPath();
                // no main loop, AWT will call update as sees fit
            }
        } catch (Throwable t) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "start()", "Exception in game loop", t);
            app.finished = true;
        }
    }

    public void cleanup() {
        
        cleanup(app);
        logger.info( "Application ending.");
        // dont call Display.close(), because we dont own the window
    }

    @Override
    public void start() {
        // do nothing, main loop is in the VLECanvas
    }

}
