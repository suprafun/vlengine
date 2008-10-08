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

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author vear
 */
public class KeyBindingSet {
    protected boolean enabled = false;
    
    protected String name;
    protected String group;
    
    protected String[] keyBinding = new String[InputSystem.MAX_KEYS];
    protected InputListener[] handler = new InputListener[InputSystem.MAX_KEYS];
    //protected Method[] method = new Method[InputSystem.MAX_KEYS];
    
    public KeyBindingSet(String name, String group) {
        this.name = name;
        this.group = group;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public void set(String command, int keyCode, InputListener handler) {
        try {
            //Method mth = handler.getClass().getMethod(command, InputSystem.handlerSig);
            keyBinding[keyCode] = command;
            this.handler[keyCode] = handler;
            //method[keyCode] = mth;
        } catch (Exception ex) {
            Logger.getLogger(KeyBindingSet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getKeyCommand( int keyCode ) {
        return keyBinding[keyCode];
    }
    
    public InputListener getHandler( int keyCode ) {
        return handler[keyCode];
    }
    
    /*
    public Method getMethod(int keyCode) {
        return method[keyCode];
    }
     */

    public void remove( String command ) {
        for( int i=0; i<keyBinding.length; i++ ) {
            if(command.equals(keyBinding[i])) {
                keyBinding[i] = null;
            }
        }
    }

}
