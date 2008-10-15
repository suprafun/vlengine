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

package com.vlengine.input;

import com.vlengine.app.AppContext;
import com.vlengine.util.FastList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dispatches input events to listeners
 * @author vear (Arpad Vekas)
 */
public class InputSystem {

    public static final int MAX_KEYS = 270;
    
    FastList<KeyBindingSet> keyBindingSet=new FastList<KeyBindingSet>();
    
    protected AppContext app;

    public InputSystem(AppContext app) {
        this.app = app;
    }
    
    public void activate() {
        app.mouseInput.setInputSystem(this);
        app.keyInput.setInputSystem(this);
    }
    
    public void update() {
        app.mouseInput.update();
        app.keyInput.update();
        //JoystickInput.get().update();
    }

    public void onButton(int button, boolean pressed, int x, int y) {
        processInput((char)0, MouseInput.MOUSE_1 + button, pressed );
    }
    
    public void onWheel( int wheelDelta, int x, int y ) {
        if( wheelDelta != 0)
            processInput((char)0, MouseInput.MOUSE_WHEEL, false );
    }
    
    public void onMove( int xDelta, int yDelta, int x, int y ) {
        if( xDelta != 0 || yDelta != 0 )
            processInput((char)0, MouseInput.MOUSE_MOVE, false );
    }

    public void onKey(char c, int keyCode, boolean pressed) {
        processInput(c, keyCode, pressed );
    }
    
    //public static Class[] handlerSig = new Class[] { String.class, char.class, int.class, boolean.class};
    
    protected boolean processInput(char c, int keyCode, boolean pressed ) {
        for( int i=0; i < keyBindingSet.size(); i++ ) {
            KeyBindingSet kbs = keyBindingSet.get(i);
            if( kbs.isEnabled() ) {
                String ks = kbs.getKeyCommand(keyCode);
                if( ks != null ) {
                        // get the object handling the command
                        InputListener handler = kbs.getHandler(keyCode);
                    try {
                        // call the appropriate method in inputHandler
                        if( handler!=null ) {
                            handler.handleInput(null, c, keyCode, pressed);
                            return true;
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(InputSystem.class.getName()).log(Level.WARNING, ks, ex);
                    }
                }
            }
        }
        return false;
    }
    
    public void addKeybindingSet(KeyBindingSet kbs) {
        keyBindingSet.add(kbs);
    }
    
    public void removeKeybindingSet(KeyBindingSet kbs) {
        keyBindingSet.remove(kbs);
    }
    
    public void disableKeyBindingSetGroup(String name) {
        for( int i = 0; i < keyBindingSet.size(); i++ ) {
            KeyBindingSet kbs = keyBindingSet.get(i);
            if(kbs.getGroup().equals(name)) {
                kbs.setEnabled(false);
            }
        }
    }
    
    public void enableKeyBindingSet(String name, String group) {
        for( int i = 0; i < keyBindingSet.size(); i++ ) {
            KeyBindingSet kbs = keyBindingSet.get(i);
            if(name!=null && kbs.getName().equals(name)) {
                kbs.setEnabled(true);
            } else if(group!=null && kbs.getGroup().equals(group)) {
                kbs.setEnabled(false);
            }
        }
    }
    
    public KeyBindingSet getKeyBindingSet(String name) {
        for( int i = 0; i < keyBindingSet.size(); i++ ) {
            KeyBindingSet kbs = keyBindingSet.get(i);
            if(name!=null && kbs.getName().equals(name)) {
                return kbs;
            }
        }
        return null;
    }

    public String getKeyName( int key ) {
        if(key >= 256 ) {
            // mouse button
            return app.mouseInput.getName(key);
        }
        return app.keyInput.getKeyName(key);
    }
    
    public int getKeyIndex(String name) {
        if( name.startsWith("MOUSE_")) {
            // mouse button
            return app.mouseInput.getButtonIndex(name);
        }
        return app.keyInput.getKeyIndex(name);
    }
}
