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

package com.vlengine.input;

import com.vlengine.app.AppContext;
import com.vlengine.math.Vector3f;
import com.vlengine.system.VleException;
import java.util.logging.Logger;

/**
 * Input handler for debug commands, this input handler is used in tests
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class InputHandler {
    private static final Logger logger = Logger.getLogger(InputHandler.class.getName());
    
    protected InputSystem input;
    protected KeyBindingSet basickeys;
    protected KeyBindingSet debugkeys;
    //protected MainGame mainGame;
    private boolean[] restrictKey = new boolean[InputSystem.MAX_KEYS];
    
    protected AppContext app;
    
    public InputHandler(AppContext app) {
        this.app = app;
    }
    
    public void setInputSystem(InputSystem is) {
        if( input == null) {
            this.input = is;
        } else {
            throw new VleException("InputSystem already set");
        }
    }
    
    /**
     * Returns true if a key is down and wasn't down last call.
     * If a key is down and not restricted, the key is set as restricted and true is returned.
     * If a key is down and restricted, false is returned.
     * If a key is not down and is restricted, the restriction is cleared.
     * @param key The key to test
     * @return True if the key is a fresh key input.
     */
    private boolean getStickyKey(int key) {
        if (!restrictKey[key] && app.keyInput.isKeyDown(key)) {
            restrictKey[key] = true;
            return true;
        } else if (!app.keyInput.isKeyDown(key) && restrictKey[key])
            restrictKey[key] = false;
        return false;
    }
    
    /*
    public void setMainGame(MainGame game ) {
        this.mainGame = game;
    }
     */
    
    // basic commands
    
    public void setUpBasicCommands() {
        if(basickeys!=null) return;
        basickeys = new KeyBindingSet("basic", "basic");
        basickeys.set( "screen_shot", KeyInput.KEY_F12, new InputListener() {
            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                screen_shot(parm, c, keyCode, pressed);
            }
        } );
        basickeys.set( "toggle_fps", KeyInput.KEY_F7, new InputListener() {
            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                toggle_fps(parm, c, keyCode, pressed);
            }
        } );
        basickeys.set( "exit", KeyInput.KEY_ESCAPE, new InputListener() {
            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                exit(parm, c, keyCode, pressed);
            }
        } );
        basickeys.setEnabled( true );
        input.addKeybindingSet(basickeys );
    }
    
    public void screen_shot(String parm, char c, int keyCode, boolean pressed) {
        if(getStickyKey(keyCode)) {
            app.doTakeScreenShot = true;
            //app.display.getRenderer().takeScreenShot( "GameScreenShot" );
        }
    }
    
    public void exit(String parm, char c, int keyCode, boolean pressed) {
        if(pressed) {
            app.finished = true;
        }
    }
    
    public void toggle_fps(String parm, char c, int keyCode, boolean pressed) {
        if(getStickyKey(keyCode)) {
            app.setShowFps(!app.isShowFps());
        }
    }
    
    // debug commands
    public void setUpDefaultDebugCommands() {
        if(debugkeys!=null) return;
        debugkeys = new KeyBindingSet("debug", "debug");
        debugkeys.set( "toggle_pause", KeyInput.KEY_P, new InputListener() {
            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                toggle_pause(parm, c, keyCode, pressed);
            }
        } );
        /** Assign key ADD to action "step". */
        debugkeys.set( "step_update", KeyInput.KEY_F6, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                step_update(parm, c, keyCode, pressed);
            }
        } );
        /** Assign key T to action "toggle_wire". */
        debugkeys.set( "toggle_wire", KeyInput.KEY_F10, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                toggle_wire(parm, c, keyCode, pressed);
            }
        } );
        /** Assign key L to action "toggle_lights". */
        debugkeys.set( "toggle_lights", KeyInput.KEY_L, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                toggle_lights(parm, c, keyCode, pressed);
            }
        } );
        /** Assign key B to action "toggle_bounds". */
        debugkeys.set( "toggle_bounds", KeyInput.KEY_B, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                toggle_bounds(parm, c, keyCode, pressed);
            }
        } );
        /** Assign key N to action "toggle_normals". */
        debugkeys.set( "toggle_normals", KeyInput.KEY_N, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                toggle_normals(parm, c, keyCode, pressed);
            }
        } );
        debugkeys.set( "toggle_depth", KeyInput.KEY_F8, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                toggle_depth(parm, c, keyCode, pressed);
            }
        } );
        /** Assign key C to action "camera_out". */
        debugkeys.set( "camera_out", KeyInput.KEY_C, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                camera_out(parm, c, keyCode, pressed);
            }
        } );
        debugkeys.set( "parallel_projection", KeyInput.KEY_F2, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                parallel_projection(parm, c, keyCode, pressed);
            }
        } );
        debugkeys.set( "mem_report", KeyInput.KEY_R, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                mem_report(parm, c, keyCode, pressed);
            }
        } );
        debugkeys.setEnabled( true );
        input.addKeybindingSet( debugkeys );
    }
    
    public void setEnableDebugKeys(boolean enable) {
        if(debugkeys!=null) {
            debugkeys.setEnabled(enable);
        }
    }
    
    public void toggle_pause(String parm, char c, int keyCode, boolean pressed) {
        if(getStickyKey(keyCode)) {
            app.pause= !app.pause;
            app.setPaused(app.pause);
        }
    }
    
    public void step_update(String parm, char c, int keyCode, boolean pressed) {
        if(pressed)
            app.setStepUpdate(true);
    }
    
    public void toggle_wire(String parm, char c, int keyCode, boolean pressed) {
        if(getStickyKey(keyCode)) {
            app.setShowWireframe(!app.getShowWireframe());
        }
    }
    
    public void toggle_lights(String parm, char c, int keyCode, boolean pressed) {
        if(getStickyKey(keyCode)) {
            app.enableLight = !app.enableLight;
        }
    }
    
    public void toggle_bounds(String parm, char c, int keyCode, boolean pressed) {
        if(getStickyKey(keyCode)) {
            app.showBounds = !app.showBounds;
        }
    }
    
    public void toggle_normals(String parm, char c, int keyCode, boolean pressed) {
        if(getStickyKey(keyCode)) {
            app.showNormals = !app.showNormals;
        }
    }
    
    public void camera_out(String parm, char c, int keyCode, boolean pressed) {
        if(getStickyKey(keyCode)) {
            Vector3f ang = new Vector3f().set(app.camn.getLocalRotation().toAngles(null));
            logger.info( "Camera at: "+ app.camn.getLocalTranslation() + " direction " + ang );
        }
    }
    
    public void parallel_projection(String parm, char c, int keyCode, boolean pressed) {
        if(getStickyKey(keyCode)) {
            app.cameraParallel = !app.cameraParallel;
        }
    }
    
    public void mem_report(String parm, char c, int keyCode, boolean pressed) {
        if(getStickyKey(keyCode)) {
            long totMem = Runtime.getRuntime().totalMemory();
            long freeMem = Runtime.getRuntime().freeMemory();
            long maxMem = Runtime.getRuntime().maxMemory();

            logger.info("|*|*|  Memory Stats  |*|*|");
            logger.info("Total memory: "+(totMem>>10)+" kb");
            logger.info("Free memory: "+(freeMem>>10)+" kb");
            logger.info("Max memory: "+(maxMem>>10)+" kb");
        }
    }
    
    public void toggle_depth(String parm, char c, int keyCode, boolean pressed) {
        if(getStickyKey(keyCode)) {
            app.showDepth = !app.showDepth;
        }
    }
}
